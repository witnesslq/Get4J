package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;
import com.bytegriffin.get4j.util.StringUtil;

/**
 * 点评网商铺信息
 */
public class DianpingShopPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        if (!StringUtil.isNullOrBlank(page.getTitle())) {
            System.err.println(page.getTitle() + "   " + page.getCharset() + "  " + page.getUrl() + "  ");
        }
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://www.dianping.com/search/category/2/10/p{1}").parser(DianpingShopPageParser.class)
                .totalPages(1).detailSelector("div.pic>a[href]")
                .jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                .thread(3).start();

    }

}
