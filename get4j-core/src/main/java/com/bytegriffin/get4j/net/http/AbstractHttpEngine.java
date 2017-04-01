package com.bytegriffin.get4j.net.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.util.UrlQueue;

/**
 * HttpEngine共有属性方法
 */
public abstract class AbstractHttpEngine {

	/**
	 * 出现防止刷新页面的关键词， 当然有种可能是页面中的内容包含这些关键词，而网站并没有屏蔽频繁刷新的情况
	 */
	private static final Pattern KEY_WORDS = Pattern.compile(".*(\\.(刷新太过频繁|刷新太频繁|刷新频繁|频繁访问|访问频繁|访问太频繁|访问过于频繁))$");

	/**
	 * 是否在页面中发现此内容
	 * 
	 * @param url
	 * @return
	 */
	private boolean isFind(String url) {
		Matcher m = KEY_WORDS.matcher(url);
		boolean isfind = m.find();
		return isfind;
	}

	/**
	 * 记录站点防止频繁抓取的页面链接<br>
	 * 处理某些站点避免频繁请求而作出的页面警告，当然这些警告原本就是页面内容，不管如何都先记录下来<br>
	 * 有的站点需要等待一段时间就可以访问正常；有的需要人工填写验证码，有的直接禁止ip访问等 <br>
	 * 出现这种问题，爬虫会先记录下来，如果出现这类日志，爬虫使用者可以设置Http代理和UserAgent重新抓取
	 * 
	 * @param seedName
	 * @param url
	 * @param content
	 * @param logger
	 */
	protected void frequentAccesslog(String seedName, String url, String content, Logger logger) {
		if (isFind(content)) {
			logger.warn("线程[" + Thread.currentThread().getName() + "]种子[" + seedName + "]访问[" + url + "]时太过频繁。");
		}
	}

	/**
	 * 转换页面内容： 将inputstream转换成string类型
	 * 
	 * @param is
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	protected String getContentAsString(InputStream is, String charset) throws IOException {
		String str = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
		StringBuffer sb = new StringBuffer();
		while ((str = reader.readLine()) != null) {
			sb.append(str).append("\n");
		}
		return sb.toString();
	}

	/**
	 * 对url进行解码，否则就是类似这种格式：http://news.baidu.com/n?cmd=6&loc=0&name=%B1%B1%BE%A9
	 * 暂时没用
	 * 
	 * @param url
	 * @param charset
	 * @return
	 */
	@Deprecated
	protected String decodeUrl(String url, String charset) {
		try {
			url = URLDecoder.decode(url, charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return url;
	}

	/**
	 * 初始化Http引擎配置参数：HttpProxy、UserAgent、Sleep、SleepRange
	 * @param seed
	 */
	public void initParams(Seed seed, Logger logger) {
		// 1.初始化Http Proxy
		List<HttpProxy> httpProxys = seed.getFetchHttpProxy();
		if (httpProxys != null && httpProxys.size() > 0) {
			HttpProxySelector hplooper = new HttpProxySelector();
			hplooper.setQueue(httpProxys);
			Constants.HTTP_PROXY_CACHE.put(seed.getSeedName(), hplooper);
		}

		// 2.初始化Http UserAgent
		List<String> userAgents = seed.getFetchUserAgent();
		if (userAgents != null && userAgents.size() > 0) {
			UserAgentSelector ualooper = new UserAgentSelector();
			ualooper.setQueue(userAgents);
			Constants.USER_AGENT_CACHE.put(seed.getSeedName(), ualooper);
		}

		// 3.设置HttpClient请求的间隔时间，
		if (seed.getFetchSleep() != null && seed.getFetchSleep() != 0) { // 首先判断sleep参数，然后才判断sleep.range参数
			Constants.FETCH_SLEEP_CACHE.put(seed.getSeedName(), seed.getFetchSleep());
		} else {
			setSleepRange(seed, logger);
		}
	}

	/**
	 * 设置Sleep Range参数
	 * @param seed
	 * @param logger
	 */
	private static void setSleepRange(Seed seed,Logger logger){
		String sleepRange = seed.getFetchSleepRange();
		if(StringUtil.isNullOrBlank(sleepRange)){
			return;
		}
		String split = sleepRange.contains("-") ? "-" : "－";
		String start = sleepRange.substring(0, sleepRange.indexOf(split));
		String end = sleepRange.substring(sleepRange.lastIndexOf(split)+1, sleepRange.length());
		if(!StringUtil.isNumeric(start) || !StringUtil.isNumeric(end)){
			logger.error("线程[" + Thread.currentThread().getName() + "]检查种子[" + seed.getSeedName() + "]的间隔范围配置中[" + sleepRange + "]不能出现字符串。");
			System.exit(1);
		}
		Long min = Long.valueOf(start);
		Long max = Long.valueOf(end);
		if (max == min){
			Constants.FETCH_SLEEP_CACHE.put(seed.getSeedName(), min);
		} else if(min > max){
			Long temp = max;
			max = min;
			min = temp;
		}
		List<Long> list = new ArrayList<Long>();
		for(Long i=min; i<=max; i++){
			list.add(i);
		}
		SleepRandomSelector sleeprandom = new SleepRandomSelector();
		sleeprandom.setQueue(list);
		Constants.FETCH_SLEEP_RANGE_CACHE.put(seed.getSeedName(), sleeprandom);
	}

	/**
	 * 设计Http请求间隔时间
	 * 
	 * @param seedName
	 */
	protected static void sleep(String seedName, Logger logger) {
		Long millis = Constants.FETCH_SLEEP_CACHE.get(seedName);
		if (millis == null) {
			SleepRandomSelector random = Constants.FETCH_SLEEP_RANGE_CACHE.get(seedName);
			if (random == null) {
				return;
			}
			millis = random.choice();
		} 
		try {
			Thread.sleep(millis * 1000);
		} catch (InterruptedException e) {
			logger.error("Http请求时间间隔时出错：", e);
		}
	}

	/**
	 * 判断HttpClient下载是否为Json文件
	 * 
	 * @param contentType
	 * @return
	 */
	protected static boolean isJsonFile(String contentType) {
		if (contentType.contains("json") || contentType.contains("JSON") || contentType.contains("Json")) {
			return true;
		}
		return false;
	}

	/**
	 * 判断是否为普通页面
	 * @param contentType
	 * @return
	 */
	protected static boolean isPage(String contentType) {
		if (contentType.contains("text/html") || contentType.contains("text/plain")) {
			return true;
		}
		return false;
	}

	/**
	 * 判断是否为xml文件
	 * @param contentType
	 * @return
	 */
	protected static boolean isXmlFile(String contentType, String content) {
		if (contentType.contains("xml") || content.contains("<?xml")) {
			return true;
		}
		return false;
	}

	/**
	 * 获取页面编码<br>
	 * 1.如果Response的Header中有 Content-Type:text/html; charset=utf-8直接获取<br>
	 * 2.但是有时Response的Header中只有
	 * Content-Type:text/html;没有charset，此时需要去html页面中寻找meta标签， 例如：[meta
	 * http-equiv=Content-Type content="text/html;charset=gb2312"]<br>
	 * 3.有时html页面中是这种形式：[meta charset="gb2312"]<br>
	 * 4.如果都没有那只能返回utf-8
	 * 
	 * @param contentType
	 * @param content  转码前的content，有可能是乱码
	 * @return
	 */
	protected String getCharset(String contentType, String content) {
		String charset = "";
		if (contentType.contains("charset=")) {// 如果Response的Header中有 Content-Type:text/html; charset=utf-8直接获取
			charset = contentType.split("charset=")[1];
		} else {// 但是有时Response的Header中只有 Content-Type:text/html;没有charset
			if(isXmlFile(contentType, content)){ // 首先判断是不是xml文件，有的xml文件返回的也是text/html，但是根节点是<?xml ...>
				Document doc = Jsoup.parse(content, "", Parser.xmlParser());
				Node root = doc.root();
				Node node = root.childNode(0);
				charset = node.attr("encoding");
			} else if (isPage(contentType)) {// 如果是html，可以用jsoup解析html页面上的meta元素
				Document doc = Jsoup.parse(content);
				Elements eles1 = doc.select("meta[http-equiv=Content-Type]");
				Elements eles2 = doc.select("meta[charset]");
				if (!eles1.isEmpty() && eles1.get(0) != null) {
					String meta = eles1.get(0).attr("content");
					charset = meta.split("charset=")[1];
				} else if (!eles2.isEmpty() && eles2.get(0) != null) {// 也可以是这种类型：
					charset = eles2.get(0).attr("charset");
				} else {// 如果html页面内也没有含Content-Type的meta标签，那就默认为utf-8
					charset = Charset.defaultCharset().name();
				}
			} else if(isJsonFile(contentType)){ // 如果是json，那么给他设置默认编码
				charset = Charset.defaultCharset().name();
			}
		}
		return charset;
	}

	/**
	 * 根据ContentType设置page内容
	 * 
	 * @param contentType
	 * @param content 转码后的content
	 * @param page
	 */
	protected void setContent(String contentType, String content, Page page) {
		if (isPage(contentType)) {
			// 注意：有两种特殊情况
			// 1.有时text/plain这种文本格式里面放的是json字符串，并且是这个json字符串里的属性值却是html
			// 2.有时text/html反应出来的是rss的xml格式
			if (content.contains("<?xml") || content.contains("<rss") || content.contains("<feed")) {
				page.setXmlContent(content);
			} else {
				page.setHtmlContent(content);
			}
			// json文件中一般不好嗅探titile属性
			page.setTitle(UrlAnalyzer.getTitle(content));
		} else if (isJsonFile(contentType)) {
			page.setJsonContent(content);
		} else if(isXmlFile(contentType, content)){
			page.setXmlContent(content);
			// json文件中一般不好嗅探titile属性
			page.setTitle(UrlAnalyzer.getTitle(content));
		} else { // 不是html也不是json，那么只能是resource的链接了，xml也是
			HashSet<String> resources = page.getResources();
			resources.add(page.getUrl());
		}
	}

	/**
	 * 是否要继续访问 根据response返回的状态码判断是否继续访问，true：是；false：否
	 * 
	 * @param statusCode
	 * @param page
	 * @param logger
	 * @return
	 */
	protected static boolean isVisit(int statusCode, Page page, Logger logger) {
		// 404/403/500/503
		if (HttpStatus.SC_NOT_FOUND == statusCode || HttpStatus.SC_FORBIDDEN == statusCode
				|| HttpStatus.SC_INTERNAL_SERVER_ERROR == statusCode
				|| HttpStatus.SC_SERVICE_UNAVAILABLE == statusCode) {
			UrlQueue.newFailVisitedUrl(page.getSeedName(), page.getUrl());
			logger.error("线程[" + Thread.currentThread().getName() + "]访问种子[" + page.getSeedName() + "]的url["
					+ page.getUrl() + "]时发生[" + statusCode + "]错误。");
			return false;
		} else if (HttpStatus.SC_OK != statusCode) {
			logger.warn("线程[" + Thread.currentThread().getName() + "]访问种子[" + page.getSeedName() + "]的url["
					+ page.getUrl() + "]时发生[" + statusCode + "]错误。");
		}
		return true;
	}

}
