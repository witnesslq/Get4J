package com.bytegriffin.get4j.net.sync;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.util.ConcurrentQueue;

/**
 * 计划任务：用于按时或按量分批次地同步资源 
 */
public class BatchScheduler implements Runnable {

	private Syncer sync;
	private static long last_update_time = System.currentTimeMillis();
	private static AtomicInteger count = new AtomicInteger();
	// 格式： seedname|avatar path 例如：seed1|c:\seed1\abc.jpg  seedname用于在远程创建目录
	public static final String split ="|";
	public static ConcurrentQueue<String> resources = new ConcurrentQueue<>();

	private static volatile boolean flag = true;

	public static void addResource(String seedName, String path) {
		if(!UrlAnalyzer.isStartHttpUrl(path)){
			resources.add(seedName+split+path);
		}
	}

	public BatchScheduler(Syncer sync){
		this.sync = sync;
	}
	
	public static void start(){
		flag = true;
	}
	
	public static void stop(){
		flag = false;
	}

	@Override
	public synchronized void run() {
		while (flag) {
			if(resources.size() >= Constants.SYNC_PER_MAX_COUNT 
					|| (System.currentTimeMillis() - last_update_time) >= Constants.SYNC_PER_MAX_INTERVAL){
				List<String> avatar = resources.list;				
				sync.setBatch(avatar);
				sync.sync();
				last_update_time = System.currentTimeMillis();
				count.incrementAndGet();
				resources.clear();
			}
			
		}
		
	}


}
