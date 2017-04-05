package com.bytegriffin.get4j.sample;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;
import com.bytegriffin.get4j.util.UrlQueue;

public class RssPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        String title = page.getTitle();
        System.err.println(title + "  " + page.getAvatar());
    }

    
    /**
     *
     *  线程[pool-2-thread-3]下载大小为[22]MB的文件[E:\work\workspace\get4j\get4j-core\data\download\rssnews.baidu.com.]总共花费时间为[]。
     * 爬取rss/atom文件
     * 文件路径有问题
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
//		String url = "http://www.nhzy.org/feed"; http://news.baidu.com/n?cmd=1&class=gsdt&tn=rss
        Spider.cascade().fetchUrl("http://news.baidu.com/n?cmd=1&class=gsdt&tn=rss").parser(RssPageParser.class)
                .jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                .thread(3).downloadDisk("classpath:/data/download/rss/").start();
    }
}
