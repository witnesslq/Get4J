package com.bytegriffin.get4j.probe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.probe.ProbePageSerial.ProbePage;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.MD5Util;
import com.bytegriffin.get4j.util.StringUtil;

/**
 * 页面变化探测器 ：用于再次爬取页面前事先探查页面的具体情况。<br>
 * 一般探查结果有如下几种情况：<br>
 * 1.页面没有任何改变：继续轮询探测<br>
 * 2.页面框架没变，只是新增了动态数据，此时需要爬取<br>
 * 3.页面改版：需要重新设置爬虫<br>
 * 4.页面url不能再被访问：需要查明情况再定<br>
 * 注意：应尽量避免监控整个页面内容，即：fetchProbeSelector值尽量避免设置为default<br>
 * 因为有的页面每次请求都会在页面中动态生成一个随机字符串，这样就会导致程序判断每次访问页面都发生了变化。
 */
public class PageChangeProber {

    private static final Logger logger = LogManager.getLogger(PageChangeProber.class);
    private HttpEngine http = null;
    private static volatile boolean isrun = true;
    private ProbePage probePage;
    private String fetchProbeSelector;
    private int fetchProbeSleep;
    private Page page;

    public PageChangeProber(Seed seed) {
        init(seed);
    }

    public void init(Seed seed) {
        // 1.设置page对象
        page = new Page(seed.getSeedName(), UrlAnalyzer.formatListDetailUrl(seed.getFetchUrl()));
        fetchProbeSelector = seed.getFetchProbeSelector();
        if (Constants.default_config.equalsIgnoreCase(seed.getFetchProbeSleep()) ||
                StringUtil.isNullOrBlank(seed.getFetchProbeSleep()) ||
                !StringUtil.isNumeric(seed.getFetchProbeSleep()) ||
                Integer.valueOf(seed.getFetchProbeSleep()) <= 0) {
            fetchProbeSleep = Constants.default_probe_sleep * 1000;
        } else {
            fetchProbeSleep = Integer.valueOf(seed.getFetchProbeSleep()) * 1000;
        }

        // 2.创建probe文件夹
        FileUtil.makeFile(Constants.probe_folder, ProbeFileStorage.filename);

        // 3.缓存ProbePage文件对象
        if (FileUtil.isExistContont(ProbeFileStorage.probe_file)) {
            probePage = ProbeFileStorage.read(UrlAnalyzer.formatListDetailUrl(seed.getFetchUrl()));
        }

        // 4.获取相应的http引擎
        http = Constants.HTTP_ENGINE_CACHE.get(seed.getSeedName());
        logger.info("种子[" + seed.getSeedName() + "]的组件PageChangeProber的初始化完成。");
    }

    public void stop() {
        isrun = false;
    }

    public void start() {
        isrun = true;
    }


    public void run() {
        // 1.第一次启动需要判断probe文件中等于url数据是否为空，如果为空的话则表示要先要抓取一次。
        // 注意：此时还可以添加 判断数据库中是否存在与url相等的记录
        if (probePage == null) {
            http.probePageContent(page);
            String content = null;
            if (page.isHtmlContent()) {
                content = page.getHtmlContent();
            } else if (page.isJsonContent()) {
                content = page.getJsonContent();
            } else if (page.isXmlContent()) {
                content = page.getXmlContent();
            }
            if (!Constants.default_config.equalsIgnoreCase(fetchProbeSelector)) {
                content = UrlAnalyzer.selectContent(page, fetchProbeSelector);
            }
            ProbeFileStorage.append(ProbeFileStorage.probe_file, page.getUrl(), content);
            probePage = ProbeFileStorage.read(page.getUrl());
            return;
        }

        while (isrun) {

            // 2.获取最新页面内容
            String newContent = http.probePageContent(page);
            // 当页面内容为空时，发送Email通知
            if (StringUtil.isNullOrBlank(newContent)) {
                stop();
                logger.error("探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]内容为空。");
                break;
            }

            String content;
            if (Constants.default_config.equals(fetchProbeSelector)) {
                content = newContent;
            } else {
                content = UrlAnalyzer.selectContent(page, fetchProbeSelector);
            }

            if (StringUtil.isNullOrBlank(content)) {
                stop();
                logger.error("探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]页面选择器[" + content + "]出错或者是页面改版。");
                break;
            }

            // 3.比对probe文件中存储url页面内容与新抓取的页面内容，
            // 如果相同，则需要继续轮询抓取页面
            // 如果不同，需要更新probe文件中相同url的content内容 并且 需要抓取新变化的内容
            if (!probePage.getContent().equalsIgnoreCase(MD5Util.convert(content))) {
                probePage = ProbeFileStorage.update(page.getUrl(), content);
                logger.error("探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]发现新增内容，准备抓取更新操作。。。");
                stop();
                break;
            }

            try {
                Thread.sleep(fetchProbeSleep);
            } catch (InterruptedException e) {
                logger.error("探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]时出错。", e);
            }
            logger.info("正在探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]的页面变化。。。");
        }

    }


}

