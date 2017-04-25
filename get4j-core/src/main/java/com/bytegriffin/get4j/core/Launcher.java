package com.bytegriffin.get4j.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.probe.PageChangeProber;
import com.bytegriffin.get4j.net.http.HttpClientEngine;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.net.sync.BatchScheduler;
import com.bytegriffin.get4j.net.sync.RsyncSyncer;
import com.bytegriffin.get4j.net.sync.ScpSyncer;
import com.bytegriffin.get4j.store.FailUrlStorage;
import com.bytegriffin.get4j.util.ConcurrentQueue;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.ShellUtil;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.util.UrlQueue;
import com.jayway.jsonpath.JsonPath;

/**
 * 轮询每个seed，将seed中url分配给各个worker工作线程
 */
public class Launcher extends TimerTask {

    private static final Logger logger = LogManager.getLogger(Launcher.class);

    private Seed seed;
    private ExecutorService batch;
    // 当avatar资源文件已经同步到图片服务器后，是否删除本地已经下载的这些资源文件，从而节省磁盘空间
    private boolean isDeleteDownloadFile = false;

    public Launcher(Seed seed) {
        this.seed = seed;
    }

    @Override
    public void run() {
        // 设置页面变化监测器
        PageChangeProber probe = Constants.FETCH_PROBE_CACHE.get(seed.getSeedName());
        if (probe != null) {
            probe.run();
            working();
            probe.start();
            run();
        }
        working();
    }

    private void working() {
        // 设置UrlQueue
        setUnVisitedUrlQueue(seed);
        // 设置资源同步
        setSync();
        // 设置执行线程
        ExecutorService executorService;
        CountDownLatch latch;
        int threadNum = seed.getThreadNumber();
        if (threadNum <= 1) {
            latch = new CountDownLatch(1);
            executorService = Executors.newSingleThreadExecutor();
            Worker worker = new Worker(seed.getSeedName(), latch);
            executorService.execute(worker);
        } else {
            executorService = Executors.newFixedThreadPool(threadNum);
            long waitThread = 3000;
            latch = new CountDownLatch(threadNum);
            for (int i = 0; i < threadNum; i++) {
                Worker worker = new Worker(seed.getSeedName(), latch);
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
        HttpEngine he = Constants.HTTP_ENGINE_CACHE.get(seed.getSeedName());
        if (he instanceof HttpClientEngine) {
            HttpClientEngine.closeIdleConnection();
        }
        // 关闭资源同步器
        if (Constants.RESOURCE_SYNCHRONIZER != null) {
            while (BatchScheduler.resources.size() > 0) {
                try {
                    Thread.sleep(Constants.SYNC_BATCH_TIME * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (batch != null) {
                BatchScheduler.stop();
                batch.shutdown();
            }
            // 清空下载目录：将页面以及资源文件全部删除，从而节省磁盘空间
            if (isDeleteDownloadFile) {
                for (String seedName : Constants.DOWNLOAD_DIR_CACHE.keySet()) {
                    FileUtil.deleteFile(Constants.DOWNLOAD_DIR_CACHE.get(seedName));
                }
            }
        }
        // 清空这次抓取访问过的url集合
        clearVisitedUrlQueue(seed);
    }

    /**
     * 初始化UrlQueue
     *
     * @param seed seed
     */
    private void setUnVisitedUrlQueue(Seed seed) {
        if (!PageMode.list_detail.equals(seed.getPageMode())) {
            // 添加每个seed对应的未访问url
            UrlQueue.newUnVisitedLink(seed.getSeedName(), seed.getFetchUrl());
        } else {// list_detail 因为分页的问题要特殊处理
            // 1.计算总页数
            String fetchUrl = seed.getFetchUrl();
            String totalPages = seed.getFetchTotalPages();
            if (!StringUtil.isNullOrBlank(totalPages) && !StringUtil.isNumeric(totalPages)) {
                Page page = Constants.HTTP_ENGINE_CACHE.get(seed.getSeedName()).getPageContent(new Page(seed.getSeedName(), UrlAnalyzer.formatListDetailUrl(fetchUrl)));
                if (totalPages.contains(Constants.JSON_PATH_PREFIX)) {// json格式
                    int totalPage = JsonPath.read(page.getJsonContent(), totalPages);// Json会自动转换类型
                    totalPages = String.valueOf(totalPage);// 所以需要再次转换
                } else {// html格式
                    Document doc = Jsoup.parse(page.getHtmlContent());
                    totalPages = doc.select(totalPages.trim()).text().trim();
                    if (StringUtil.isNullOrBlank(totalPages)) {
                        totalPages = "1";
                    }
                }
            }

            // 2.根据输入的列表Url和总页数生成所有页面Url，生成规则就是将大括号中的值自增1，即表示下一个列表页
            // 例如：http://www.aaa.com/bbb?p={1} ==> http://www.aaa.com/bbb?p=1、...、http://www.aaa.com/bbb?p=10
            Pattern p = Pattern.compile("\\" + Constants.FETCH_LIST_URL_VAR_LEFT + "(.*)" + Constants.FETCH_LIST_URL_VAR_RIGHT);
            Matcher m = p.matcher(fetchUrl);
            if (m.find()) {
                int pagenum = Integer.valueOf(m.group(1));
                String prefix = fetchUrl.substring(0, fetchUrl.indexOf(Constants.FETCH_LIST_URL_VAR_LEFT));
                String suffix = fetchUrl.substring(fetchUrl.indexOf(Constants.FETCH_LIST_URL_VAR_RIGHT) + 1);
                List<String> list = new ArrayList<>();
                int totalPage = Integer.valueOf(totalPages);
                for (int i = 0; i < totalPage; i++) {
                    int pn = pagenum + i;
                    UrlQueue.newUnVisitedLink(seed.getSeedName(), prefix + pn + suffix);
                    list.add(prefix + pn + suffix);
                }
                Constants.LIST_URLS_CACHE.put(seed.getSeedName(), list);
            }
            logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子[" + seed.getSeedName() + "]列表Url总数是[" + Constants.LIST_URLS_CACHE.size() + "]个。");
        }
    }

    /**
     * 每次抓取完都要清空一次已访问的url集合，以方便下次继续抓取
     * 否则在下次抓取时程序会判断内存中已经存在抓取过的url就不再去抓取
     *
     * @param seed seed
     * @see UrlQueue.addUnVisitedLinks()
     */
    private void clearVisitedUrlQueue(Seed seed) {
        ConcurrentQueue<String> visitedlink = UrlQueue.getVisitedLink(seed.getSeedName());
        if (visitedlink != null && !visitedlink.isEmpty()) {
            visitedlink.clear();
        }
        ConcurrentQueue<String> visitedResource = UrlQueue.getVisitedResource(seed.getSeedName());
        if (visitedResource != null && !visitedResource.isEmpty()) {
            visitedResource.clear();
        }
    }

    // 设置资源同步
    private void setSync() {
        if (Constants.RESOURCE_SYNCHRONIZER == null) {
            return;
        }
        if ((Constants.RESOURCE_SYNCHRONIZER instanceof RsyncSyncer || Constants.RESOURCE_SYNCHRONIZER instanceof ScpSyncer)
                && System.getProperty("os.name").toLowerCase().contains("windows")) {
            logger.error("Rsync或Scp暂时不支持window系统，因此会强制关闭资源同步");
            Constants.SYNC_OPEN = false;
            return;
        } else if (Constants.RESOURCE_SYNCHRONIZER instanceof ScpSyncer) {
            // Scp如果想实现增量复制需要先在目标服务器上创建文件夹
            ScpSyncer scp = (ScpSyncer) Constants.RESOURCE_SYNCHRONIZER;
            ShellUtil.executeShell("ssh " + scp.getHost() + " 'mkdir " + scp.getDir() + seed.getSeedName() + "'");
        }
        BatchScheduler.start();
        batch = Executors.newSingleThreadExecutor();
        batch.execute(new BatchScheduler(Constants.RESOURCE_SYNCHRONIZER));
    }
}
