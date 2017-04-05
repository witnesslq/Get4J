package com.bytegriffin.get4j.download;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;

public class HdfsDownloader implements Process {

    private static final Logger logger = LogManager.getLogger(HdfsDownloader.class);

    public HdfsDownloader() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void init(Seed seed) {
        // TODO Auto-generated method stub
        logger.info("种子[" + seed.getSeedName() + "]的组件HdfsDownloader的初始化完成。");
    }

    @Override
    public void execute(Page page) {
        // TODO Auto-generated method stub

    }

}
