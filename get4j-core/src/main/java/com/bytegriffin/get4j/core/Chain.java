package com.bytegriffin.get4j.core;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * 爬虫工作链 :抓取=>下载=>抽取=>解析=>保存=>转发=>抓取=>...   <br/>
 */
public class Chain {

    List<Process> list = Lists.newArrayList();

    public void execute(Page page) {
        for (Process p : list) {
            p.execute(page);
        }
    }

    public Chain addProcess(Process p) {
        list.add(p);
        return this;
    }

}
