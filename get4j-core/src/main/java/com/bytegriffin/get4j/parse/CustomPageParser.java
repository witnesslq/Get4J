package com.bytegriffin.get4j.parse;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.annotation.Site;
import com.bytegriffin.get4j.core.Page;

/**
 * 自定义的解析类（示例，实际开发可以定义适合具体场景的PageParser）
 */
@Site(url = "https://www.yirendai.com/")
public class CustomPageParser implements PageParser{

	@Override
	public void parse(Page page) {

	}

	public static void main(String[] args) throws Exception{
		Spider.annotation(CustomPageParser.class).start();
	}

}