package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * iteye博客抓取
 */
public class IteyePagePaser implements PageParser{

	@Override
	public void parse(Page page) {
		System.err.println(page.getTitle());
	}

	public static void main(String[] args) throws Exception{
		Spider.list_detail().fetchUrl("http://www.iteye.com/blogs?page={1}")
			.totalPages("div.pagination>a:eq(6)").detailSelector("div.content>h3>a[href]").parser(IteyePagePaser.class)
			.jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
			.thread(3).start();
	}

}
