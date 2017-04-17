package com.bytegriffin.get4j.core;

import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.net.http.HttpClientEngine;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.sync.BatchScheduler;
import com.bytegriffin.get4j.net.sync.RsyncSyncer;
import com.bytegriffin.get4j.net.sync.ScpSyncer;
import com.bytegriffin.get4j.store.FailUrlStorage;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.ShellUtil;

/**
 * 轮询每个seed，将seed中url分配给各个worker工作线程
 */
public class JobController extends TimerTask {

	private static final Logger logger = LogManager.getLogger(JobController.class);

    private String seedName;
    private int threadNum;
    private ExecutorService batch;
    // 当avatar资源文件已经同步到图片服务器后，是否删除本地已经下载的这些资源文件，从而节省磁盘空间
    private boolean isDeleteDownloadFile = false;

    public JobController(String siteName, int threadNum) {
        this.seedName = siteName;
        this.threadNum = threadNum;
    }
    
    // 设置资源同步
    private void setSync(){
    	if(Constants.RESOURCE_SYNCHRONIZER == null){
        	return;
        }
    	if((Constants.RESOURCE_SYNCHRONIZER instanceof RsyncSyncer || Constants.RESOURCE_SYNCHRONIZER instanceof ScpSyncer)
    			&& System.getProperty("os.name").toLowerCase().contains("windows")){
        	logger.error("Rsync暂时不支持window系统，因此会强制关闭资源同步");
        	Constants.SYNC_OPEN = false;
        	return;
        } else if(Constants.RESOURCE_SYNCHRONIZER instanceof ScpSyncer){
        	// Scp如果想实现增量复制需要先在目标服务器上创建文件夹
        	ScpSyncer scp = (ScpSyncer) Constants.RESOURCE_SYNCHRONIZER;
			ShellUtil.executeShell("ssh "+scp.getHost()+" 'mkdir "+scp.getDir()+seedName+"'");
        }
    	BatchScheduler.start();
    	batch = Executors.newSingleThreadExecutor();	
		batch.execute(new BatchScheduler(Constants.RESOURCE_SYNCHRONIZER));
    }

    @Override
    public void run() {
        ExecutorService executorService;
        CountDownLatch latch;
        // 设置资源同步
        setSync();
        // 设置执行线程
        if (threadNum <= 1) {
            latch = new CountDownLatch(1);
            executorService = Executors.newSingleThreadExecutor();
            Worker worker = new Worker(seedName, latch);
            executorService.execute(worker);
        } else {
            executorService = Executors.newFixedThreadPool(threadNum);
            long waitThread = 3000;
            latch = new CountDownLatch(threadNum);
            for (int i = 0; i < threadNum; i++) {
                Worker worker = new Worker(seedName, latch);
                executorService.execute(worker);
                try {// 必须要加等待，否则运行太快会导致只有一个线程抓取，其它线程因为运行太快urlQueue里面为空而一直处于等待
                    Thread.sleep(waitThread);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // 等待所有工作线程执行完毕，再将坏链dump出来
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Set<String> seedNameKeys = Constants.CHAIN_CACHE.keySet();
        for (String seedName : seedNameKeys) {
            FailUrlStorage.dumpFile(seedName);
        }
        // 关闭闲置链接，以便下一次多线程调用
        HttpEngine he = Constants.HTTP_ENGINE_CACHE.get(seedName);
        if (he instanceof HttpClientEngine) {
            HttpClientEngine.closeIdleConnection();
        }
        // 关闭资源同步器
        if(Constants.RESOURCE_SYNCHRONIZER != null){
        	while(BatchScheduler.resources.size() > 0){
        		try {
					Thread.sleep(10000);					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        	if(batch != null){
        		BatchScheduler.stop();
            	batch.shutdown();
        	}
        	// 清空下载目录：将页面以及资源文件全部删除，从而节省磁盘空间
            if(isDeleteDownloadFile){
            	for(String seedName : Constants.DOWNLOAD_DIR_CACHE.keySet()){
                	FileUtil.deleteFile(Constants.DOWNLOAD_DIR_CACHE.get(seedName));
                }
            }
        }
        
    }

}
