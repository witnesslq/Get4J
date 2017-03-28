package com.bytegriffin.get4j.core;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bytegriffin.get4j.fetch.FetchMode;
import com.bytegriffin.get4j.util.ConcurrentQueue;
import com.bytegriffin.get4j.util.UrlQueue;

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
		ExecutorService executorService = null;
		if (threadNum <= 1) {
			executorService = Executors.newSingleThreadExecutor();
			Worker worker = new Worker(seedName, UrlQueue.getUnVisitedLink(seedName));
			executorService.execute(worker);
		} else {
			ConcurrentQueue<String> urlQueue = UrlQueue.getUnVisitedLink(seedName);
			executorService = Executors.newCachedThreadPool();
			long waitThread = 1000;
			FetchMode fm = Constants.FETCH_MODE_CACHE.get(seedName);
			if(FetchMode.cascade.equals(fm) || FetchMode.site.equals(fm)){//抓取这种类型页面中链接时会比较费时，所以需要将线程等待时间设计的长一些
				waitThread = 3000;
			}
			for (int i = 0; i < threadNum; i++) {
				Worker worker = new Worker(seedName, urlQueue);
				executorService.execute(worker);
				try {//必须要加等待，否则运行太快会导致只有一个线程抓取，其它线程因为运行太快urlQueue里面为空而一直处于等待
					Thread.sleep(waitThread);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}


}
