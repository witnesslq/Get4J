package com.bytegriffin.get4j.net.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

/**
 * 处理某些站点避免频繁请求而作出的页面警告，<br>
 * 有的站点需要等待一段时间就可以访问正常；有的需要人工填写验证码，有的直接禁止ip访问等 <br>
 * 出现这种问题，爬虫会先记录下来，如果出现这类日志，爬虫使用者可以设置Http代理和UserAgent重新抓取
 */
public final class FrequentAccess {
	
	/**
	 * 出现防止刷新页面的关键词，
	 * 当然有种可能是页面中的内容包含这些关键词，而网站并没有屏蔽频繁刷新的情况
	 */
	public static final Pattern KEY_WORDS = Pattern
            .compile(".*(\\.(刷新太过频繁|刷新太频繁|刷新频繁|频繁访问|访问频繁|访问太频繁|访问过于频繁))$");
	
	/**
	 * 是否在页面中发现此内容
	 * @param url
	 * @return
	 */
	public static boolean isFind(String url){
		Matcher m = KEY_WORDS.matcher(url);
		boolean isfind = m.find();
		return isfind;
	}
	
	/**
	 * 记录站点防止频繁抓取的页面链接
	 * @param seedName
	 * @param url
	 * @param content
	 * @param logger
	 */
	public static void log(String seedName, String url, String content, Logger logger){
		if (isFind(content)) {
			logger.warn("线程[" + Thread.currentThread().getName() + "]种子[" + seedName + "]访问[" + url+ "]时太过频繁。");
		}
	}

}
