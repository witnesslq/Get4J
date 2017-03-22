package com.bytegriffin.parse;

import java.util.HashSet;

import com.bytegriffin.core.Page;


public class CustomPageParser implements PageParser{

	@Override
	public void parse(Page page) {
		HashSet<Page> sets = page.getDetailPages();
		
	}

	public static void main(String[] args) throws Exception{
//		Class<?> clazz = Class.forName("com.bytegriffin.parse.CustomPageParser");
//		boolean anno = clazz.isAnnotationPresent(FetchUrl.class);
//		if(anno){
//			FetchUrl url = (FetchUrl) clazz.getAnnotation(FetchUrl.class);
//			System.out.println(url.value());
//		}
		
	}

}