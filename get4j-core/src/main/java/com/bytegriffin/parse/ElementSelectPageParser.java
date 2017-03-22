package com.bytegriffin.parse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.bytegriffin.core.Page;
import com.bytegriffin.net.http.UrlAnalyzer;

/**
 * 针对页面中或Json中的某一个元素的解析器<br />
 * 这个Parser主要用于长期获取网页上的某个值<br />
 * 而不是一堆值，一堆值需要自己写解析类<br />
 * 默认支持Jsoup的css选择器和正则表达式
 */
public class ElementSelectPageParser implements PageParser{
	
	private static final Logger logger = LogManager.getLogger(ElementSelectPageParser.class);
	
	private String select;
	
	public ElementSelectPageParser(String select){
		this.select = select;
	}

	@Override
	public void parse(Page page) {
		String text = null;
		if(page.isHtmlContent()){//用jsoup的css选择器或正则表达式解析html
			text = UrlAnalyzer.select(page.getHtmlContent(),select);
			page.setHtmlContent(text);
		} else if(page.isJsonContent()){//用jsonpath解析json
			try{
				text = JsonPath.read(page.getJsonContent(), select);
			}catch(PathNotFoundException p){
				logger.error("Seed["+page.getSeedName()+"]在使用Jsonpath["+select+"]定位解析Json字符串时出错，",p);
			}
			page.setJsonContent(text);
		}
		
	}

}
