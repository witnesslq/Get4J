package com.bytegriffin.get4j.parse;

import java.util.HashSet;

import com.bytegriffin.get4j.core.Page;

/**
 * 自定义的解析类（示例，实际开发可以定义适合具体场景的PageParser）
 */
public class CustomPageParser implements PageParser{

	@Override
	public void parse(Page page) {
		HashSet<Page> sets = page.getDetailPages();
		
	}

	public static void main(String[] args) throws Exception{
//		Class<?> clazz = Class.forName("com.bytegriffin.get4j.parse.CustomPageParser");
//		boolean anno = clazz.isAnnotationPresent(FetchUrl.class);
//		if(anno){
//			FetchUrl url = (FetchUrl) clazz.getAnnotation(FetchUrl.class);
//			System.out.println(url.value());
//		}
		
	}

}