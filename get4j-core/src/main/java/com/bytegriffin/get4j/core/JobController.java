package com.bytegriffin.get4j.core;

import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bytegriffin.get4j.net.http.HttpClientEngine;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.store.FailUrlStorage;

/**
 * 轮询每个seed，将seed中url按照平均分配原则 来分配给工作线程。
 */
public class JobController extends TimerTask {

    private String seedName;
    private int threadNum;

    public JobController(String siteName, int threadNum) {
        this.seedName = siteName;
        this.threadNum = threadNum;
    }

    @Override
    public void run() {
        ExecutorService executorService;
        CountDownLatch latch;
        if (threadNum <= 1) {
            latch = new CountDownLatch(1);
            executorService = Executors.newSingleThreadExecutor();
            Worker worker = new Worker(seedName, latch);
            executorService.execute(worker);
        } else {
            executorService = Executors.newFixedThreadPool(threadNum);
            long waitThread = 1000;
            PageMode fm = Constants.FETCH_PAGE_MODE_CACHE.get(seedName);
            if (PageMode.cascade.equals(fm) || PageMode.site.equals(fm)) {// 抓取这种类型页面中链接时会比较费时，所以需要将线程等待时间设计的长一些
                waitThread = 3000;
            }
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
    }

}
