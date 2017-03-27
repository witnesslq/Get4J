package com.bytegriffin.get4j.fetch;

import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.UrlQueue;

/**
 * 级联页面抓取器<br>
 * 和SiteFetch不同，它提供页面上的所有url都要抓取，不管是不是外链 <br>
 * 慎用：容易出现各种问题，因为网站外链所在的网站上仍然有外链，<br>
 * 这样就会造成永远抓取不完的情况，但是如果能事先确认某一个站点内的 <br>
 * 所有链接都不是外链的话，那抓取的效果就等同于SiteFetch
 */
public class CascadeFetcher implements Process{

	private static final Logger logger = LogManager.getLogger(CascadeFetcher.class);

	@Override
	public void init(Seed seed) {
		// 初始化url选择/过滤器缓存
		FetchResourceSelector.init(seed);
		logger.info("Seed[" + seed.getSeedName() + "]的组件CascadeFetcher的初始化完成。");
	}

	@Override
	public void execute(Page page) {
		// 1.获取并设置Page的HtmlContent或JsonContent属性、Cookies属性
		page = Constants.HTTPPROBE_CACHE.get(page.getSeedName()).SetContentAndCookies(page);
		
		// 2.获取并设置Page的Resource属性
		UrlAnalyzer.custom(page).sniffAndSetResources();

		// 3.设置Page其它属性
		page.setFetchTime(DateUtil.getCurrentDate());
		if (page.isHtmlContent()) {// Html格式
			page.setTitle(UrlAnalyzer.getTitle(page.getHtmlContent()));
		} else { // Json格式

		}

		// 4.嗅探出新访问地址并增加新的访问链接交给爬虫队列
		HashSet<String> links = UrlAnalyzer.custom(page).sniffAllLinks();
		UrlQueue.addUnVisitedLinks(page.getSeedName(), links);
		logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子Seed["+page.getSeedName()+"]级联页面完成。");
	}

}
