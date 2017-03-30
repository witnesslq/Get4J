package com.bytegriffin.get4j.net.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

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
	public void frequentAccesslog(String seedName, String url, String content, Logger logger) {
		if (isFind(content)) {
			logger.warn("线程[" + Thread.currentThread().getName() + "]种子[" + seedName + "]访问[" + url + "]时太过频繁。");
		}
	}

	/**
	 * 转换页面内容： 将inputstream转换成string类型
	 * @param is
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public String getContentAsString(InputStream is, String charset) throws IOException {
		String str = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
		StringBuffer sb = new StringBuffer();
		while ((str = reader.readLine()) != null) {
			sb.append(str).append("\n");
		}
		return sb.toString();
	}
	
	/**
	 * 将输入流转换成字节数组
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public byte[] transfer(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while ((len = inputStream.read(buffer)) > -1) {
			baos.write(buffer, 0, len);
		}
		baos.flush();
		baos.close();
		return baos.toByteArray();
	}

	/**
	 * 获取页面编码<br>
	 * 1.如果Response的Header中有 Content-Type:text/html; charset=utf-8直接获取<br>
	 * 2.但是有时Response的Header中只有 Content-Type:text/html;没有charset，此时需要去html页面中寻找meta标签，
	 * 例如：[meta http-equiv=Content-Type content="text/html;charset=gb2312"]<br>
	 * 3.有时html页面中是这种形式：[meta charset="gb2312"]<br>
	 * 4.如果都没有那只能返回utf-8
	 * @param contentType
	 * @param content
	 * @return
	 */
	public String getCharset(String contentType, String content) {
		String charset = "";
		if (contentType.contains("charset=")) {
			charset = contentType.split("charset=")[1];
		} else {
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

		}
		return charset;
	}

}
