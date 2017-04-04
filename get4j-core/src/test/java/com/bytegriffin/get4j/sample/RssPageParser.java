package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class RssPageParser implements PageParser{

	@Override
	public void parse(Page page) {
		String title = page.getTitle();
		System.err.println(title+"  "+page.getAvatar());
	}

	/**
	 * 爬取rss/atom文件
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
//		String url = "http://www.nhzy.org/feed"; http://news.baidu.com/n?cmd=1&class=gsdt&tn=rss
		Spider.single().fetchUrl("https://www.zhihu.com/rss").parser(RssPageParser.class)
		.jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
		.thread(3).downloadDisk("classpath:/data/download/rss/").start();
	}
}
