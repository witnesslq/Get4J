package com.bytegriffin.get4j.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.util.ConcurrentQueue;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.util.UrlQueue;

/**
 * 工作线程
 */
public class Worker implements Runnable {

	private static final Logger logger = LogManager.getLogger(Worker.class);

	private ConcurrentQueue<String> urlQueue;
	private String seedName;

	public Worker(String seedName, ConcurrentQueue<String> urlQueue) {
		this.seedName = seedName;
		this.urlQueue = urlQueue;
	}

	@Override
	public void run() {
		if (StringUtil.isNullOrBlank(seedName)) {
			return;
		}

		while (urlQueue != null && !urlQueue.isEmpty()) {
			logger.info("线程[" + Thread.currentThread().getName() + "]开始执行任务[" + seedName + "]。。。");
			Object obj = urlQueue.outFirst();
			if (obj == null) {
				break;
			}
			String url = obj.toString();
			Chain chain = Constants.CHAIN_CACHE.get(seedName);
			chain.execute(new Page(seedName, url));
			UrlQueue.newVisitedLink(seedName, url);
			logger.info("线程[" + Thread.currentThread().getName() + "]完成任务[" + seedName + "]。。。");
		}

	}

}