package com.bytegriffin.net.http;

import java.util.LinkedList;

/**
 * 循环获取User Agent，更好地模拟人为操作
 */
public class UserAgentLooper extends Looper<String> {

	private LinkedList<String> queue;

	@Override
	protected LinkedList<String> getQueue() {
		return queue;
	}

	@Override
	public void setQueue(LinkedList<String> queue) {
		this.queue = queue;
	}

}
