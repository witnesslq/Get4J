package com.bytegriffin.net.http;

import com.bytegriffin.conf.Seed;
import com.bytegriffin.core.Page;

/**
 * Http引擎<br>
 * 目前有两种：HttpClient 和 HttpUnit
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
	Page SetContentAndCookies(Page page);

}
