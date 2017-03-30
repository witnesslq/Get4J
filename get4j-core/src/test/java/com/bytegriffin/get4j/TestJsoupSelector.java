package com.bytegriffin.get4j;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class TestJsoupSelector {
	
	static String url = "http://www.iteye.com/blogs";
	static String selector = "div.content>h3>a[href]";

	public static void main(String[] args) throws Exception{
		Document doc = Jsoup.connect(url).get();
		Elements eles = doc.select(selector);
		System.out.println(eles.html());
	}

}
