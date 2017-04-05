package com.bytegriffin.get4j.fetch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.util.UrlQueue;
import com.jayway.jsonpath.JsonPath;

/**
 * List-Detail格式的页面抓取器<br>
 */
public class ListDetailFetcher implements Process {

    private static final Logger logger = LogManager.getLogger(ListDetailFetcher.class);
    private HttpEngine http = null;
    /**
     * 存放list列表url key：seed_name value：该seed下所有的列表url
     */
    private Map<String, List<String>> listLink = new HashMap<>();
    /**
     * 存放详情页面链接与avatar资源的映射关系 key：detail_link value：avatar_link
     */
    private Map<String, String> detailLinkAvatar = new HashMap<>();

    @Override
    public void init(Seed seed) {
        // 1.获取相应的http引擎
        http = Constants.HTTP_ENGINE_CACHE.get(seed.getSeedName());

        // 2.初始化detail页面
        String detailSelect = seed.getFetchDetailSelector();
        if (!StringUtil.isNullOrBlank(detailSelect)) {
            Constants.FETCH_DETAIL_SELECT_CACHE.put(seed.getSeedName(), detailSelect.replace(" ", ""));
        }

        // 3.初始化资源选择器缓存
        FetchResourceSelector.init(seed);

        // 4.根据总页数来初始化列表url集合
        String fetchUrl = seed.getFetchUrl();
        String totalPages = seed.getFetchTotalPages();
        if (!StringUtil.isNullOrBlank(totalPages) && !StringUtil.isNumeric(totalPages)) {
            Page page = Constants.HTTP_ENGINE_CACHE.get(seed.getSeedName()).getPageContent(
                    new Page(seed.getSeedName(), fetchUrl.replace(Constants.FETCH_LIST_URL_VAR_LEFT, "")
                            .replace(Constants.FETCH_LIST_URL_VAR_RIGHT, "")));
            if (totalPages.contains(Constants.JSON_PATH_PREFIX)) {// json格式
                int totalPage = JsonPath.read(page.getJsonContent(), totalPages);// Json会自动转换类型
                totalPages = String.valueOf(totalPage);// 所以需要再次转换
            } else {// html格式
                Document doc = Jsoup.parse(page.getHtmlContent());
                totalPages = doc.select(totalPages.trim()).text().trim();
                if (StringUtil.isNullOrBlank(totalPages)) {
                    totalPages = "1";
                }
            }
        }

        // 5.根据输入的列表Url生成当前页面的Url
        generateListUrl(seed.getSeedName(), fetchUrl, Integer.valueOf(totalPages));
        logger.info("种子[" + seed.getSeedName() + "]的组件ListDetailFetcher的初始化完成。");
    }

    /**
     * 根据输入的列表Url生成当前页面的Url<br>
     * 生成规则就是将大括号中的值自增1，即表示下一个列表页<br>
     * 例如：http://www.aaa.com/bbb?p={1} ==>
     * http://www.aaa.com/bbb?p=1、...、http://www.aaa.com/bbb?p=10
     *
     * @param seedName seedName
     * @param listUrl listUrl
     * @param totalPages totalPages
     */
    private void generateListUrl(String seedName, String listUrl, int totalPages) {
        Pattern p = Pattern
                .compile("\\" + Constants.FETCH_LIST_URL_VAR_LEFT + "(.*)" + Constants.FETCH_LIST_URL_VAR_RIGHT);
        Matcher m = p.matcher(listUrl);
        if (m.find()) {
            int pagenum = Integer.valueOf(m.group(1));
            String prefix = listUrl.substring(0, listUrl.indexOf(Constants.FETCH_LIST_URL_VAR_LEFT));
            String suffix = listUrl.substring(listUrl.indexOf(Constants.FETCH_LIST_URL_VAR_RIGHT) + 1);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < totalPages; i++) {
                int pn = pagenum + i;
                UrlQueue.newUnVisitedLink(seedName, prefix + pn + suffix);
                list.add(prefix + pn + suffix);
                listLink.put(seedName, list);
            }
        }
        logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子[" + seedName + "]列表Url总数是[" + listLink.size() + "]个。");
    }

    @Override
    public void execute(Page page) {

        List<String> listurls = listLink.get(page.getSeedName());
        if (listurls.contains(page.getUrl())) {// 访问的是list url
            // 1.获取并设置列表页Page的HtmlContent或JsonContent属性
            page = http.getPageContent(page);

            // 2.设置Page其它属性 （detailSelect要先设置）
            page.setFetchTime(DateUtil.getCurrentDate());

            // 3.获取并设置Page的Resource属性
            FetchResourceSelector resourceselector = Constants.FETCH_RESOURCE_SELECTOR_CACHE.get(page.getSeedName());
            String detailSelect = Constants.FETCH_DETAIL_SELECT_CACHE.get(page.getSeedName());
            // 如果fetch.resource.selector配置了all或者none，或者没有配置fetch.detail.selecotr只配置了fetch.resource.selector，那说明没有detaillink与avatar对应，也就是说只能抓取所有resource
            if (resourceselector == null || resourceselector.isConfigAll() || resourceselector.isConfigNone()
                    || StringUtil.isNullOrBlank(detailSelect)) {
                UrlAnalyzer.custom(page).sniffAndSetResources();
            } else { // 如果fetch.resource.selector配置了具体参数（一定是包含detaillink与avatar资源的外部的css选择器或正则，而不只是指定avatar），则表示抓取符合参数的具体资源
                detailLinkAvatar = UrlAnalyzer.custom(page).mappingDetailLinkAndAvatar();
            }

            // 4.设置Page的detailPage属性
            HashSet<String> links = UrlAnalyzer.custom(page).sniffDetailLinks();
            if (links != null && links.size() > 0) {
                UrlQueue.addUnVisitedLinks(page.getSeedName(), links);
                page.setDetailLinks(links);
                logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]的详情Url总数是[" + UrlQueue.getUnVisitedLink(page.getSeedName()).size() + "]个。");
            }

        } else {// 访问的是detail url

            // 1.获取并设置详情页DetailPage的HtmlContent或JsonContent属性
            Page detailPage = new Page(page.getSeedName(), page.getUrl());
            detailPage = http.getPageContent(detailPage);

            // 2.设置Detail Link对应的Avatar资源
            if (!detailLinkAvatar.isEmpty()) {
                if (detailLinkAvatar.containsKey(page.getUrl())) {
                    detailPage.setAvatar(detailLinkAvatar.get(page.getUrl()));
                }
            } else {
                // 3.获取并设置详情页DetailPage的Resource属性
                UrlAnalyzer.custom(detailPage).sniffAndSetResources();
            }


            // 4.设置详情页DetailPage其它属性
            detailPage.setFetchTime(DateUtil.getCurrentDate());

            // 5.将详情页面属性指定为传递对象，当Page类增加新属性后此段代码也需要更新
            page.setTitle(detailPage.getTitle());
            page.setAvatar(detailPage.getAvatar());
            page.setCookies(detailPage.getCookies());
            page.setFetchTime(detailPage.getFetchTime());
            page.setHtmlContent(detailPage.getHtmlContent());
            page.setJsonContent(detailPage.getJsonContent());
            page.setResources(detailPage.getResources());
            page.setResourceSavePath(detailPage.getResourceSavePath());
            page.setId(detailPage.getId());
            page.setSeedName(detailPage.getSeedName());
            page.setHost(detailPage.getHost());
            page.setUrl(detailPage.getUrl());

            logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]完成。");
        }

    }

}
