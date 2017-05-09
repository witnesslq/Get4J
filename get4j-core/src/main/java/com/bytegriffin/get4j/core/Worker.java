package com.bytegriffin.get4j.core;

import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.util.ConcurrentQueue;
import com.google.common.base.Strings;
import com.bytegriffin.get4j.core.UrlQueue;

/**
 * 工作线程
 */
public class Worker implements Runnable {

    private static final Logger logger = LogManager.getLogger(Worker.class);

    private String seedName;
    private CountDownLatch latch;

    public Worker(String seedName, CountDownLatch latch) {
        this.seedName = seedName;
        this.latch = latch;
    }

    @Override
    public void run() {
        if (Strings.isNullOrEmpty(seedName)) {
            return;
        }
        Chain chain = Globals.CHAIN_CACHE.get(seedName);
        ConcurrentQueue<String> urlQueue = UrlQueue.getUnVisitedLink(seedName);
        logger.info("线程[" + Thread.currentThread().getName() + "]开始执行任务[" + seedName + "]。。。");
        while (urlQueue != null && !urlQueue.isEmpty()) {
            Object obj = urlQueue.outFirst();
            if (obj == null) {
                break;
            }
            String url = obj.toString();
            chain.execute(new Page(seedName, url));
            UrlQueue.newVisitedLink(seedName, url);
        }
        logger.info("线程[" + Thread.currentThread().getName() + "]完成任务[" + seedName + "]。。。");
        latch.countDown();
    }

}