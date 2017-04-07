package com.bytegriffin.get4j.sample;

import org.jsoup.nodes.Entities;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;
import com.bytegriffin.get4j.util.StringUtil;

public class BaiduNewsPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        if (!StringUtil.isNullOrBlank(page.getTitle())) {
            System.err.println(page.getTitle() + "   " + page.getCharset() + "  " + page.getUrl() + " "
                    + Entities.getByName(page.getUrl()));
        }
    }

    public static void main(String[] args) throws Exception {
        Spider.cascade().fetchUrl("http://news.baidu.com/")
                .parser(BaiduNewsPageParser.class)
                .jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                .thread(1).start();
    }

}
