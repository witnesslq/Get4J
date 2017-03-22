package com.bytegriffin.core;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobController extends TimerTask{

	private String seedName;
	private int threadNum;

	public JobController(String siteName, int threadNum) {
		this.seedName = siteName;
		this.threadNum = threadNum;
	}
 
	@Override
	public void run() {
		ExecutorService executorService = Executors.newCachedThreadPool();
		for(int i=0; i<threadNum; i++){
			Worker worker = new Worker(seedName);
	        executorService.execute(worker);
		}
	}

}
