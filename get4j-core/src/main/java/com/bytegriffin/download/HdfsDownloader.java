package com.bytegriffin.download;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.conf.Seed;
import com.bytegriffin.core.Page;
import com.bytegriffin.core.Process;

public class HdfsDownloader implements Process{
	
	private static final Logger logger = LogManager.getLogger(HdfsDownloader.class);

	public HdfsDownloader() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(Seed seed) {
		// TODO Auto-generated method stub
		logger.info("Seed[" + seed.getSeedName() + "]的组件HdfsDownloader的初始化完成。");
	}

	@Override
	public void execute(Page page) {
		// TODO Auto-generated method stub
		
	}

}
