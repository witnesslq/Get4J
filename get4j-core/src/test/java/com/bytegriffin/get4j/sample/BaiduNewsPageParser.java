package com.bytegriffin.get4j.sample;


import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class BaiduNewsPageParser implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getCharset() + "  " );
    }

    public static void main(String[] args) throws Exception {
        Spider.cascade().fetchUrl("http://news.baidu.com/")
                .parser(BaiduNewsPageParser.class).seedName("aaa")
                .defaultDownloadDisk().scp("192.168.1.11", "roo", "/home/roo/桌面", 22)
                //.jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                //.defaultLucene()
                .thread(3).start();
    }

}
