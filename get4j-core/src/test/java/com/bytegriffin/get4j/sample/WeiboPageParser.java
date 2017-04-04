package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class WeiboPageParser implements PageParser{

	@Override
	public void parse(Page page) {
		System.err.println(page.getTitle());
	}

	public static void main(String[] args) throws Exception{
		Spider.cascade().fetchUrl("http://weibo.com").parser(WeiboPageParser.class).defaultDownloadDisk().javascriptSupport(true)
			.jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
			.thread(1).start();
	}

}
