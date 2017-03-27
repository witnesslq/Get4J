package com.bytegriffin.get4j.core;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
			// 根据url数量来动态计算，分配给每个工作线程
			int urlsize = urlQueue.size();
			int num = urlsize / threadNum;// 这样子可能还有余数,应该把余数也分摊
			if (urlsize % threadNum != 0) {
				num++;// 如果有余数(一定小于threadNum),则前面的线程分摊下,每个线程多做一个任务
			}

			for (int i = 0; i < threadNum; i++) {
				int start = i * num;
				int end = Math.min((i + 1) * num, urlsize);// 最后一个线程任务可能不够
				ConcurrentQueue<String> subUrlQueue = urlQueue.subQueue(start, end);
				Worker worker = new Worker(seedName, subUrlQueue);
				executorService.execute(worker);
			}
		}

	}

	public static void main(String[] sadf) {
		System.out.println(1/ 3);
	}

}
