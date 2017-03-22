package com.bytegriffin.net.http;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 循环访问机制
 */
public abstract class Looper<E> {

	private int front = 0;

	private static final ReentrantLock lock = new ReentrantLock(true);

	// 设置queue
	public abstract void setQueue(LinkedList<E> queue);

	// 获取queue
    protected abstract LinkedList<E> getQueue();

    /**
     * 循环获取Queue的下一个元素
     * @param <T>
     * @return
     */
	protected E next() {
		lock.lock();
		try {
			int size = getQueue().size();
			int rear = size - 1;
			if (front > rear) {
				front = 0;
			}
			E looper = (E) getQueue().get(front);
			front += 1;
			return looper;
		} finally {
			lock.unlock();
		}
	}

}
