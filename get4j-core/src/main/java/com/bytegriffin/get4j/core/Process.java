package com.bytegriffin.get4j.core;

import com.bytegriffin.get4j.conf.Seed;

/**
 * 爬虫工作流程（抓取、下载、抽取、解析、保存、转发）
 */
public interface Process{
	
	public void init(Seed seed);

	void execute(Page page);

}
