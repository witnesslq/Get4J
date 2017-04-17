package com.bytegriffin.get4j.util;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentQueue<E> {

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    public final LinkedList<E> list = new LinkedList<>();

    public void add(E e) {
        writeLock.lock();
        try {
            if (!list.contains(e)) {
                list.add(e);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public Object get(int index) {
        readLock.lock();
        try {
            return list.get(index);
        } finally {
            readLock.unlock();
        }
    }

    public int size() {
        readLock.lock();
        try {
            return list.size();
        } finally {
            readLock.unlock();
        }
    }

    public void clear() {
        writeLock.lock();
        try {
            list.clear();
        } finally {
            writeLock.unlock();
        }
    }

    public boolean isEmpty() {
        writeLock.lock();
        try {
            return list.isEmpty();
        } finally {
            writeLock.unlock();
        }
    }

    public Object outFirst() {
        writeLock.lock();
        try {
            if (!list.isEmpty()) {
                return list.removeFirst();
            }
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    public boolean contains(E e) {
        return list.contains(e);
    }


}
