package com.bytegriffin.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.util.ConcurrentQueue;
import com.bytegriffin.util.StringUtil;
import com.bytegriffin.util.UrlQueue;

/**
 * 工作线程
 */
public class Worker implements Runnable {

	private static final Logger logger = LogManager.getLogger(Worker.class);

	private String siteName;

	public Worker(String siteName) {
		this.siteName = siteName;
	}

	@Override
	public void run() {
		if (StringUtil.isNullOrBlank(siteName)) {
			return;
		}
		ConcurrentQueue<String> urlQueue = UrlQueue.getUnVisitedLink(siteName);
		while (urlQueue != null && !urlQueue.isEmpty()) {
			logger.info("线程[" + Thread.currentThread().getName() + "]开始执行任务[" + siteName + "]。。。");
			Object obj = urlQueue.outFirst();
			if (obj == null) {
				break;
			}
			String url = obj.toString();
			Chain chain = Constants.CHAIN_CACHE.get(siteName);
			chain.execute(new Page(siteName, url));
			UrlQueue.newVisitedLink(siteName, url);
			logger.info("线程[" + Thread.currentThread().getName() + "]完成任务[" + siteName + "]。。。");
		}
		
	}

}