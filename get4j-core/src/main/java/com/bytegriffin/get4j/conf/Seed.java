package com.bytegriffin.get4j.conf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.PageMode;
import com.bytegriffin.get4j.fetch.FetchResourceSelector;
import com.bytegriffin.get4j.net.http.HttpClientEngine;
import com.bytegriffin.get4j.net.http.HttpProxy;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.StringUtil;

/**
 * 抓取站点，对应配置文件中的seed元素
 */
public class Seed {

	/**
	 * 必填项。区别于系统中不同的site，如果需求是抓取多个site：site1、site2，
	 * 那么site之间的xml定义有先后关系，先定义的site1会先被解析并执行，之后才是执行site2，
	 * 假设site2没有定义time.start选项，那么系统会在抓取完site1后再抓取site2。
	 */
	private String seedName;
	/**
	 * 非必填项。爬虫工作线程数量，默认值是1
	 */
	private int threadNumber = 1;
	/**
	 * 页面模型：list_detail（页面格式）、single（单个页面)、cascade（页面中给出所有的url链接）、site（单个站点）默认值是site
	 */
	private PageMode pageMode;
	/**
	 * 要抓取的Url，如果抓取模式fetch.mode为list_detail，该值为列表Url，
	 * 将可变的PageNum用大括号括起来，而页面中的PageSize默认该url中的页数自增1来处理。
	 */
	private String fetchUrl;
	/**
	 * 当抓取模式fetch.mode为list_detail时为必填项，表示详情页面的url，其中可变的字符串要用大括号括起来。
	 */
	private String fetchDetailSelector;
	/**
	 * 当抓取模式fetch.mode为list_detail时为必填项，表示列表总页数，可以是整数，
	 * 也可以是selector字符串
	 */
	private String fetchTotalPages = "1";
	/**
	 * 是否支持javascript
	 */
	private boolean fetchJavascriptSupport = false;
	/**
	 * url对应的登录名
	 */
	private String fetchUsername;
	/**
	 * url对应的登录密码
	 */
	private String fetchPassword;
	/**
	 * 每次抓取的时间间隔(单位：秒)
	 */
	private String fetchInterval;
	/**
	 * 开始抓取时间(yyyy-MM-dd HH:mm:ss)
	 */
	private String fetchStart;
	/**
	 * 抓取网站所需的http代理
	 */
	private List<HttpProxy> fetchHttpProxy;
	/**
	 * 抓取网站所需的User Agent
	 */
	private List<String> fetchUserAgent;
	/**
	 * 抓取该url站点所需的cookie，更像是人为操作
	 */
	private Map<String, String> fetchCookies = new LinkedHashMap<String, String>();
	/**
	 * 每次http请求之后都要延迟固定时间(毫秒)再次请求
	 */
	private Long fetchSleepTimeout;
	/**
	 * 非必填项。页面资源的抓取选择器，支持Jsoup原生的css选择器和正则表达式或Jsonpath，一般用于在LIST_DETAIL模式，抓取每个详情页的avatar图。
	 * 例想抓取图片：[*.jpg]或者$.data[*].img。当此内容为JsonPath字符串的时候，如果json中提供的detail的链接是相对路径，那么此时这个值的格式为：链接前缀+jsonpath，
	 * 如果需要抓取多个url可以用逗号","隔开，默认不填是全抓取。
	 */
	private List<String> fetchResourceSelectors;
	/**
	 * 非必填项。如果业务需要下载url站点的页面及资源文件，则将此url站点保存的磁盘目录，默认值classpath:/data/download/seedName/，绝对路径不用写classpath
	 */
	private String downloadDisk;
	/**
	 * 如果业务需要下载url站点，则将此url站点保存的hdfs系统
	 */
	private String downloadHdfs;
	/**
	 * 抽取实现类
	 * 如果业务需要先下载到本地，然后再从本地进行抽取内容，可配置此项。
	 */
	private String extractClassImpl;
	/**
	 * 自定义解析实现类，只能配置一个自定义解析实现类或内置解析类。自定义的解析类需要写具体package路径，例如：com.anspider.paser.CustomPageParser
	 */
	private String parseClassImpl;
	/**
	 * 内置解析实现类，目前只有一种内置类，支持：jsoup的css选择器、正则表达式（html内容）或jsonpath（json内容）
	 */
	private String parseElementSelector;
	/**
	 * 保存到redis内存数据库中
	 */
	private String storeRedis;
	/**
	 * 连接jdbc保存到关系型数据库中
	 */
	private String storeJdbc;
	/**
	 * 保存结果到lucene索引中，默认值/data/index/seedName/，绝对路径不用写classpath
	 */
	private String storeLuceneIndex;
	
	public Seed(){
	}

	public Seed(String seedName){
		this.seedName = seedName;
	}

	/**
	 * 设置useragent文件 读取user agent文件到内存中
	 * @param userAgentFile
	 */
	public void setFetchUserAgentFile(String userAgentFile) {
		if(!StringUtil.isNullOrBlank(userAgentFile)){
			this.fetchUserAgent = FileUtil.readUserAgentFile(userAgentFile);
		}
	}

	/**
	 * 设置httpproxy文件 读取httpproxy代理文件到内存中
	 * @param httpProxyFile
	 */
	public void setFetchHttpProxyFile(String httpProxyFile) {
		if(!StringUtil.isNullOrBlank(httpProxyFile)){
			this.fetchHttpProxy =  FileUtil.readHttpProxyFile(httpProxyFile);
		}
	}

	/**
	 * 设置抓取模式，默认是site抓取
	 * @param pageMode
	 */
	public void setPageMode(String pageMode) {
		if(!StringUtil.isNullOrBlank(pageMode)){
			this.pageMode = PageMode.valueOf(pageMode);
			return;
		} 
		if(isListDetailMode()){
			this.pageMode = PageMode.list_detail;
		} else {
			this.pageMode = PageMode.single;
		}
	}

	/**
	 * 当用户没有设置PageMode时候
	 * 也可以判断当前是否是list_detail模式
	 * @return
	 */
	public boolean isListDetailMode(){
		if(!StringUtil.isNullOrBlank(this.fetchDetailSelector)){
			return true;
		}
		return false;
	}

	/**
	 * 设置Fetch Resource Selector，默认值是all
	 * @param fetchResourcceSelectorsStr 多个resource selector用逗号拼接成的字符串
	 */
	public void setFetchResourceSelectors(String fetchResourcceSelectorsStr) {	
		List<String> fetchOtherUrlSelectors = new ArrayList<String>();
		if(!StringUtil.isNullOrBlank(fetchResourcceSelectorsStr)){
			String[] array = fetchResourcceSelectorsStr.split(Constants.FETCH_RESOURCE_SPLIT);
			for(String str : array){
				fetchOtherUrlSelectors.add(str);
			}		
		} else {
			fetchOtherUrlSelectors.add(FetchResourceSelector.ALL_RESOURCE_FILTER);
		}
		this.fetchResourceSelectors = fetchOtherUrlSelectors;
	}

	/**
	 * 格式化FetchUrl
	 * @param fetchUrl
	 */
	public void setFetchUrl(String fetchUrl) {
		fetchUrl = HttpClientEngine.addUrlSchema(fetchUrl);
		if (!PageMode.list_detail.equals(this.pageMode)){
			fetchUrl = fetchUrl.replace(Constants.FETCH_LIST_URL_VAR_LEFT, "").replace(Constants.FETCH_LIST_URL_VAR_RIGHT, "");
		}
		this.fetchUrl = fetchUrl;
	}

	public void setDownloadDisk(String downloadDisk) {
		this.downloadDisk = downloadDisk;
	}

	public void setStoreLuceneIndex(String storeLuceneIndex) {
		this.storeLuceneIndex = storeLuceneIndex;
	}
	
	public String getSeedName() {
		return seedName;
	}

	public void setSeedName(String seedName) {
		this.seedName = seedName;
	}

	public String getFetchUrl() {
		return fetchUrl;
	}

	public String getFetchUsername() {
		return fetchUsername;
	}

	public void setFetchUsername(String fetchUsername) {
		this.fetchUsername = fetchUsername;
	}

	public String getFetchPassword() {
		return fetchPassword;
	}

	public void setFetchPassword(String fetchPassword) {
		this.fetchPassword = fetchPassword;
	}

	public String getFetchInterval() {
		return fetchInterval;
	}

	public void setFetchInterval(String fetchInterval) {
		this.fetchInterval = fetchInterval;
	}

	public String getFetchStart() {
		return fetchStart;
	}

	public List<HttpProxy> getFetchHttpProxy() {
		return fetchHttpProxy;
	}

	public void setFetchHttpProxy(List<HttpProxy> fetchHttpProxy) {
		this.fetchHttpProxy = fetchHttpProxy;
	}

	public List<String> getFetchUserAgent() {
		return fetchUserAgent;
	}

	public void setFetchUserAgent(List<String> fetchUserAgent) {
		this.fetchUserAgent = fetchUserAgent;
	}

	public void setFetchResourceSelectors(List<String> fetchResourceSelectors) {
		this.fetchResourceSelectors = fetchResourceSelectors;
	}

	public void setFetchStart(String fetchStart) {
		this.fetchStart = fetchStart;
	}

	public Map<String, String> getFetchCookies() {
		return fetchCookies;
	}

	public void setFetchCookies(Map<String, String> fetchCookies) {
		this.fetchCookies = fetchCookies;
	}

	public String getDownloadDisk() {
		return downloadDisk;
	}

	public String getDownloadHdfs() {
		return downloadHdfs;
	}

	public void setDownloadHdfs(String downloadHdfs) {
		this.downloadHdfs = downloadHdfs;
	}

	public String getParseClassImpl() {
		return parseClassImpl;
	}

	public void setParseClassImpl(String parseClassImpl) {
		this.parseClassImpl = parseClassImpl;
	}

	public String getStoreRedis() {
		return storeRedis;
	}

	public void setStoreRedis(String storeRedis) {
		this.storeRedis = storeRedis;
	}

	public String getStoreJdbc() {
		return storeJdbc;
	}

	public void setStoreJdbc(String storeJdbc) {
		this.storeJdbc = storeJdbc;
	}


	public PageMode getPageMode() {
		return pageMode;
	}

	public String getExtractClassImpl() {
		return extractClassImpl;
	}

	public void setExtractClassImpl(String extractClassImpl) {
		this.extractClassImpl = extractClassImpl;
	}

	public String getStoreLuceneIndex() {
		return storeLuceneIndex;
	}
	public int getThreadNumber() {
		return threadNumber;
	}
	public void setThreadNumber(int threadNumber) {
		this.threadNumber = threadNumber;
	}
	public String getFetchTotalPages() {
		return fetchTotalPages;
	}
	public void setFetchTotalPages(String fetchTotalPages) {
		this.fetchTotalPages = fetchTotalPages;
	}

	public void setFetchUserAgent(LinkedList<String> fetchUserAgent) {
		this.fetchUserAgent = fetchUserAgent;
	}

	public void setPageMode(PageMode pageMode) {
		this.pageMode = pageMode;
	}

	public String getFetchDetailSelector() {
		return fetchDetailSelector;
	}

	public void setFetchDetailSelector(String fetchDetailSelector) {
		this.fetchDetailSelector = fetchDetailSelector;
	}

	public List<String> getFetchResourceSelectors() {
		return fetchResourceSelectors;
	}
	
	public String getParseElementSelector() {
		return parseElementSelector;
	}
	public void setParseElementSelector(String parseElementSelector) {
		this.parseElementSelector = parseElementSelector;
	}


	public Long getFetchSleepTimeout() {
		return fetchSleepTimeout;
	}

	public void setFetchSleepTimeout(Long fetchSleepTimeout) {
		this.fetchSleepTimeout = fetchSleepTimeout;
	}

	public boolean isFetchJavascriptSupport() {
		return fetchJavascriptSupport;
	}

	public void setFetchJavascriptSupport(boolean fetchJavascriptSupport) {
		this.fetchJavascriptSupport = fetchJavascriptSupport;
	}

}
