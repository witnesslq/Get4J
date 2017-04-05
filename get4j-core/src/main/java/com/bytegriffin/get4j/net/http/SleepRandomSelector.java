package com.bytegriffin.get4j.net.http;

import java.util.List;

/**
 * 每次请求的时间间隔随机选择器
 */
public class SleepRandomSelector extends RandomSelector<Long> {

    private List<Long> queue;

    @Override
    protected List<Long> getQueue() {
        return queue;
    }

    @Override
    public void setQueue(List<Long> queue) {
        this.queue = queue;
    }

}
