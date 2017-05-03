package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class CsdnBlogPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println(page.getTitle() + "  " + page.getAvatar());
    }

    public static void main(String[] args) throws Exception {//blog_list clearfix
        Spider.list_detail().fetchUrl("http://blog.csdn.net/?&page={1}").sleepRange(3, 5)
                .resourceSelector("img.head[src]").totalPages(1).detailSelector("h3.tracking-ad>a[href]").parser(CsdnBlogPageParser.class)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36")
                //.jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                //.mongodb("mongodb://localhost:27017")
                .thread(1).start();
    }

}
