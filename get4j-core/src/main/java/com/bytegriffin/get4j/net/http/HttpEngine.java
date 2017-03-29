package com.bytegriffin.get4j.net.http;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Page;

/**
 * Http引擎<br>
 * 目前有两种：HttpClient 和 HtmlUnit
 */
public interface HttpEngine {

	/**
	 * 初始化探测器配置
	 * @param seed
	 */
	void init(Seed seed);

	/**
	 * 测试HttpProxy是否可运行，都不可用程序则退出
	 * @param url
	 * @param httpProxy
	 * @return
	 */
	boolean testHttpProxy(String url, HttpProxy httpProxy);

	/**
	 * 设置页面Content、Cookie
	 * @param page
	 * @return
	 */
	Page getPageContent(Page page);

}