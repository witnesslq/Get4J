package com.bytegriffin.core;

import com.bytegriffin.conf.Seed;

/**
 * 爬虫工作流程（抓取、下载、抽取、解析、保存、转发）
 */
public interface Process{
	
	public void init(Seed seed);

	void execute(Page page);

}
