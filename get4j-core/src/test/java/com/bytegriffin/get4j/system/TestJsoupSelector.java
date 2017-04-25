package com.bytegriffin.get4j.system;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class TestJsoupSelector {

    static String url = "http://blog.chinaunix.net/site/index/page/1.html";
    static String selector = "div.two_cont2_3>a[href]";

    public static void main(String[] args) throws Exception {
        Document doc = Jsoup.connect(url).get();
        Elements eles = doc.select(selector);
        System.out.println(eles);
    }

}
