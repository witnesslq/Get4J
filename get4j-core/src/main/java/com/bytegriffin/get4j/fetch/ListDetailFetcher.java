package com.bytegriffin.get4j.fetch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.jayway.jsonpath.JsonPath;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.util.UrlQueue;

/**
 * List-Detail格式的页面抓取器<br>
 */
public class ListDetailFetcher implements Process {

	private static final Logger logger = LogManager.getLogger(ListDetailFetcher.class);

	@Override
	public void init(Seed seed) {
		// 1.初始化detail页面
		String detailSelect = seed.getFetchDetailSelector();
		if (!StringUtil.isNullOrBlank(detailSelect)) {
			Constants.FETCH_DETAIL_SELECT_CACHE.put(seed.getSeedName(), detailSelect.replace(" ", ""));
		}

		// 2.初始化资源选择器缓存
		FetchResourceSelector.init(seed);

		// 3.根据总页数来初始化列表url集合
		String fetchUrl = seed.getFetchUrl();
		String totalPages = seed.getFetchTotalPages();
		if (!StringUtil.isNullOrBlank(totalPages) && !isNumeric(totalPages)) {
			Page page = Constants.HTTPPROBE_CACHE.get(seed.getSeedName()).SetContentAndCookies(new Page(seed.getSeedName(), fetchUrl
					.replace(Constants.FETCH_LIST_URL_VAR_LEFT, "").replace(Constants.FETCH_LIST_URL_VAR_RIGHT, "")));
			if (totalPages.contains(Constants.JSON_PATH_PREFIX)) {// json格式
				int totalPage = JsonPath.read(page.getJsonContent(), totalPages);// Json会自动转换类型
				totalPages = String.valueOf(totalPage);// 所以需要再次转换
			} else {// html格式
				Document doc = Jsoup.parse(page.getHtmlContent());
				totalPages = doc.select(totalPages.trim()).text();
			}
		}
		generateListUrl(seed.getSeedName(), fetchUrl, Integer.valueOf(totalPages));
		logger.info("Seed[" + seed.getSeedName() + "]的组件ListDetailFetcher的初始化完成。");
	}

	/**
	 * 是否是数字
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str) {
		return str.matches("[0-9]{1,}");
	}

	/**
	 * 根据输入的列表Url生成当前页面的Url<br>
	 * 生成规则就是将大括号中的值自增1，即表示下一个列表页<br>
	 * 例如：http://www.aaa.com/bbb?p={1} ==>
	 * http://www.aaa.com/bbb?p=1、...、http://www.aaa.com/bbb?p=10
	 * 
	 * @param listUrl
	 */
	private void generateListUrl(String seedName, String listUrl, int totalPages) {
		Pattern p = Pattern
				.compile("\\" + Constants.FETCH_LIST_URL_VAR_LEFT + "(.*)\\" + Constants.FETCH_LIST_URL_VAR_RIGHT);
		Matcher m = p.matcher(listUrl);
		if (m.find()) {
			int pagenum = Integer.valueOf(m.group(1));
			String prefix = listUrl.substring(0, listUrl.indexOf(Constants.FETCH_LIST_URL_VAR_LEFT));
			String suffix = listUrl.substring(listUrl.indexOf(Constants.FETCH_LIST_URL_VAR_RIGHT) + 1,
					listUrl.length());
			for (int i = 0; i < totalPages; i++) {
				int pn = pagenum + i;
				UrlQueue.newUnVisitedLink(seedName, prefix + pn + suffix);
			}
		}
	}

	@Override
	public void execute(Page page) {
		// 1.获取并设置列表页Page的HtmlContent或JsonContent属性
		HttpEngine probe = Constants.HTTPPROBE_CACHE.get(page.getSeedName());
		page = probe.SetContentAndCookies(page);

		// 2.设置Page其它属性 （detailSelect要先设置）
		page.setFetchTime(DateUtil.getCurrentDate());
		if (page.isHtmlContent()) {// Html格式
			page.setTitle(UrlAnalyzer.getTitle(page.getHtmlContent()));
		} else { // Json格式：程序不知道具体哪个字段是title字段

		}

		// 3.获取并设置Page的Resource属性
		Map<String, String> detailLinkAvatar = new HashMap<String, String>();
		FetchResourceSelector resourceselector = Constants.FETCH_RESOURCE_SELECTOR_CACHE.get(page.getSeedName());
		String detailSelect = Constants.FETCH_DETAIL_SELECT_CACHE.get(page.getSeedName());
		// 如果fetch.resource.selector配置了all或者none，或者没有配置fetch.detail.selecotr只配置了fetch.resource.selector，那说明没有detaillink与avatar对应，也就是说只能抓取所有resource
		if (resourceselector.isConfigAll() || resourceselector.isConfigNone() || StringUtil.isNullOrBlank(detailSelect) ) {
			UrlAnalyzer.custom(page).sniffAndSetResources();
		} else { // 如果fetch.resource.selector配置了具体参数（一定是包含detaillink与avatar资源的外部的css选择器或正则，而不只是指定avatar），则表示抓取符合参数的具体资源
			detailLinkAvatar = UrlAnalyzer.custom(page).mappingDetailLinkAndAvatar();
		}

		// 4.设置Page的detailPage属性
		HashSet<String> links = UrlAnalyzer.custom(page).sniffDetailLinks();
		if (links != null && links.size() > 0) {
			HashSet<Page> detailPages = page.getDetailPages();
			for (String detailLink : links) {
				// 1.获取并设置详情页DetailPage的HtmlContent或JsonContent属性
				Page detailPage = new Page(page.getSeedName(), detailLink);
				detailPage = probe.SetContentAndCookies(detailPage);

				// 2.设置Detail Link对应的Avatar资源
				if(!detailLinkAvatar.isEmpty()){
					if(detailLinkAvatar.containsKey(detailLink)){
						detailPage.setAvatar(detailLinkAvatar.get(detailLink));
					}
				}

				// 3.获取并设置详情页DetailPage的Resource属性
				UrlAnalyzer.custom(detailPage).sniffAndSetResources();

				// 4.设置详情页DetailPage其它属性
				detailPage.setFetchTime(DateUtil.getCurrentDate());
				if (detailPage.isHtmlContent()) {// Html格式
					detailPage.setTitle(UrlAnalyzer.getTitle(detailPage.getHtmlContent()));
				} else { // Json格式：程序不知道具体哪个字段是title字段

				}

				// 4.将列表页Page与详情页DetailPage关联起来
				detailPages.add(detailPage);
			}
			page.setDetailPages(detailPages);

		}

		logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子Seed[" + page.getSeedName() + "]LIST_DETAIL页面完成。");
	}

	public static void main(String[] args) {
		// String sts=
		// "'pagination':{'total':2153,'pageSize':10,'pageIndex':1}";
		String x = "www.baidu.om$.asdf";

		System.out.println(x.split("\\" + Constants.JSON_PATH_PREFIX)[1]);
	}
}
