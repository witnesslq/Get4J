package com.bytegriffin.get4j.net.http;

import java.util.LinkedList;

/**
 * 循环获取Http Proxy，更好地模拟人为操作
 */
public class HttpProxyLooper extends Looper<HttpProxy> {

	private LinkedList<HttpProxy> queue;

	@Override
	protected LinkedList<HttpProxy> getQueue() {
		return queue;
	}

	@Override
	public void setQueue(LinkedList<HttpProxy> queue) {
		this.queue = queue;
	}

}
