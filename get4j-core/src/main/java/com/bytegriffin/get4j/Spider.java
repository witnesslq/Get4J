package com.bytegriffin.get4j;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.annotation.ListDetail;
import com.bytegriffin.get4j.annotation.Single;
import com.bytegriffin.get4j.annotation.Site;
import com.bytegriffin.get4j.conf.Configuration;
import com.bytegriffin.get4j.conf.ConfigurationXmlHandler;
import com.bytegriffin.get4j.conf.Context;
import com.bytegriffin.get4j.conf.CoreSeedsXmlHandler;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.PageMode;
import com.bytegriffin.get4j.core.SpiderEngine;
import com.bytegriffin.get4j.net.http.HttpProxy;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.MD5Util;
import com.bytegriffin.get4j.util.StringUtil;

/**
 * 爬虫入口类兼Api<br>
 * 主要负责对内xml加载运行 和 对外的API调用
 */
public class Spider {

	private static final Logger logger = LogManager.getLogger(Spider.class);

	private static Spider me;

	private static Seed seed;

	/**
	 * 设置种子名称<br>
	 * 每个种子名称要唯一<br>
	 * @param seedName
	 * @return
	 */
	public Spider seedName(String seedName){
		seed.setSeedName(seedName);
		return this;
	}


	/**
	 * 抓取的页面模型<br>
	 * 必填项。list_detail（列表-详情页面）、single（单个页面)、site（单个站点）、cascade（单页面上的所有链接）
	 * 默认值是single。
	 * @param pageMode
	 * @return
	 */
	public Spider pageMode(PageMode pageMode) {
		seed.setPageMode(pageMode);
		return this;
	}

	/**
	 * 设置抓取url <br>
	 * 必填项。表示要抓取的Url，如果抓取模式pageMode为list_detail，该值为列表Url，
	 * 其中可变的页数PageNum需要用大括号{}括起来
	 * @param fetchUrl
	 * @return
	 */
	public Spider fetchUrl(String fetchUrl) {
		seed.setFetchUrl(fetchUrl);
		return this;
	}

	/**
	 * 爬虫工作线程数量，非必填项。默认值是1。
	 * @param num
	 * @return
	 */
	public Spider thread(int num) {
		seed.setThreadNumber(num);
		return this;
	}

	/**
	 * 抓取的详情页面链接选择器 <br>
	 * 非必填项，当抓取的页面格式属于【列表-详情】页时使用，支持Jsoup原生的选择器（html内容）或Jsonpath（json内容）。 <br>
	 * 当此内容为JsonPath字符串的时候，如果list页面的json中提供的detail的链接是相对路径，那么此时这个值的格式为：链接前缀+jsonpath，
	 * 例如：http://www.aaa.com/bbb$.data.url；当此内容为空时，说明抓取的页面格式是普通页面，不存在详情页面。<br>
	 * 注意：有种特殊情况，当Json属性中的内容是Html格式，并且Html里包含着详情页的链接时候，此时需要先写Jsonpath
	 * 再写Jsoup选择器字符串，中间用竖杠隔开，例如： $.data.*|a.class[href]。
	 * @param detailSelector
	 * @return
	 */
	public Spider detailSelector(String detailSelector) {
		seed.setFetchDetailSelector(detailSelector);
		return this;
	}

	/**
	 * 抓取的列表总页数 <br>
	 * 非必填项，当抓取的页面格式属于【列表-详情】页时使用，动态获取页面中显示的总页数。
	 * 支持Jsoup原生的选择器（html内容）或Jsonpath（json内容），默认值是1。
	 * @param totalPageSelector
	 * @return
	 */
	public Spider totalPages(String totalPageSelector) {
		seed.setFetchTotalPages(totalPageSelector);
		return this;
	}

	/**
	 * 抓取的列表总页数 <br>
	 * 非必填项，当抓取的页面格式属于【列表-详情】页时使用，直接定义抓取页数，默认值是1。
	 * @param totalPageNum
	 * @return
	 */
	public Spider totalPages(int totalPageNum) {
		seed.setFetchTotalPages(String.valueOf(totalPageNum));
		return this;
	}

	/**
	 * 抓取延迟<br>
	 * 非必填项。表示每两次http请求之间的间隔时间(毫秒)，以防止频繁访问站点抓取不到内容，默认值为0
	 * @param timeout
	 * @return
	 */
	public Spider sleep(Long timeout) {
		seed.setFetchSleepTimeout(timeout);
		return this;
	}

	/**
	 * 抓取启动器<br>
	 * startTime表示爬虫抓取的开始时间，格式为：2001-10-10 23:29:02，如果startTime已经过时，爬虫会立刻执行
	 * @param startTime
	 * @return
	 */
	public Spider timer(String startTime) {
		seed.setFetchStart(startTime);
		return this;
	}

	/**
	 * 抓取启动器<br>
	 * firstTime表示爬虫第一次的抓取时间，格式为：2001-10-10 23:29:02，如果firstTime已经过时，爬虫会立刻执行 <br>
	 * interval表示爬虫重复抓取的时间间隔，单位是秒
	 * @param firstTime
	 * @param interval
	 * @return
	 */
	public Spider timer(String firstTime, Long interval) {
		seed.setFetchStart(firstTime);
		if (interval != null) {
			seed.setFetchInterval(String.valueOf(interval));
		}
		return this;
	}

	/**
	 * 资源选择器，支持Jsoup原生的选择器（html内容）或Jsonpath（json内容）
	 * @param resourceSelector
	 * @return
	 */
	public Spider resourceSelector(String resourceSelector) {
		seed.setFetchResourceSelectors(resourceSelector);
		return this;
	}

	/**
	 * 是否支持Javascript，有些网站需要等待javascript来生成结果，此时可以将此属性设为true，
	 * 默认值是false，慎用：抓取效率会变慢
	 * @param isSupport
	 * @return
	 */
	public Spider javascriptSupport(boolean isSupport) {
		seed.setFetchJavascriptSupport(isSupport);
		return this;
	}

	/**
	 * 设置代理<br>
	 * 爬虫会自动检测，如果代理不能用，会立刻停止
	 * @param ip
	 * @param port
	 * @return
	 */
	public Spider proxy(String ip, Integer port) {
		HttpProxy hp = new HttpProxy(ip, port);
		List<HttpProxy> list = new ArrayList<HttpProxy>();
		list.add(hp);
		seed.setFetchHttpProxy(list);
		return this;
	}

	/**
	 * 设置一组代理<br>
	 * 爬虫会自动检测，如果代理不能用，会立刻停止
	 * @param list
	 * @return
	 */
	public Spider proxys(List<HttpProxy> list) {
		seed.setFetchHttpProxy(list);
		return this;
	}

	/**
	 * 自定义一个UserAgent
	 * @param userAgent
	 * @return
	 */
	public Spider userAgent(String userAgent) {
		List<String> list = new LinkedList<String>();
		list.add(userAgent);
		seed.setFetchUserAgent(list);
		return this;
	}
	
	/**
	 * 定义一个列User_Agent
	 * @param userAgents
	 * @return
	 */
	public Spider userAgents(List<String> userAgents) {
		seed.setFetchUserAgent(userAgents);
		return this;
	}

	/**
	 * 下载本地路径，默认地址为$path/data/download/${seedname}
	 * @param disk
	 * @return
	 */
	public Spider downloadDisk(String disk) {
		seed.setDownloadDisk(disk);
		return this;
	}

	/**
	 * 下载到hdfs路径
	 * @param path
	 * @return
	 */
	public Spider downloadHdfs(String hdfs) {
		seed.setDownloadDisk(hdfs);
		return this;
	}

	/**
	 * 自定义页面解析类
	 * @param parser
	 * @return
	 */
	public Spider parser(Class<?> parser) {
		seed.setParseClassImpl(parser.getName());
		return this;
	}
	
	/**
	 * 单个页面元素解析内部类，设置了此项就不能设置自定义的解析类了
	 * @param elementSelector
	 * @return
	 */
	public Spider elementSelectParser(String elementSelector) {
		seed.setParseElementSelector(elementSelector);
		return this;
	}

	/**
	 * 将解析的结果保存到Mysql中，
	 * jdbc格式：jdbc:mysql://localhost:3306/spider?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root
	 * @param jdbc
	 * @return
	 */
	public Spider jdbc(String jdbc) {
		seed.setStoreJdbc(jdbc);
		return this;
	}

	/**
	 * 将解析结果保存到Lucene索引
	 * @param indexPath
	 * @return
	 */
	public Spider lucene(String indexPath) {
		seed.setStoreLuceneIndex(indexPath);
		return this;
	}
	
	/**
	 * 将解析结构保存到hbase数据库中
	 * @param address
	 * @return
	 */
	public Spider hbase(String address) {
		seed.setStoreLuceneIndex(address);
		return this;
	}
	
	/**
	 * annotation入口，如果不像一项一项设置seed属性，也可以写一个annotation
	 * @param clazz
	 * @return
	 * @throws Exception 
	 */
	public Spider annotation(Class<?> clazz) throws Exception{
		Annotation[] ans = clazz.getDeclaredAnnotations();
		if(ans == null || ans.length == 0){
			logger.error("类["+clazz.getName()+"]没有配置任何Annotation。");
			System.exit(1);
		}
		String type = ans[0].annotationType().getSimpleName();
		if("ListDetail".equalsIgnoreCase(type)){
			boolean anno = clazz.isAnnotationPresent(ListDetail.class);
			if(anno){
				ListDetail seed = (ListDetail) clazz.getAnnotation(ListDetail.class);
				this.pageMode(PageMode.list_detail);
				this.fetchUrl(seed.url());
				this.detailSelector(seed.detailSelector());
				this.totalPages(seed.totolPages());
				this.thread(seed.thread());
				this.timer(seed.startTime(), seed.interval());
				this.sleep(seed.sleep());
				HttpProxy hp = FileUtil.formatProxy(seed.proxy());
				this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
				this.userAgent(seed.userAgent());
				this.resourceSelector(seed.resourceSelector());
				this.downloadDisk(seed.downloadDisk());
				this.downloadHdfs(seed.downloadHdfs());
				this.javascriptSupport(seed.javascriptSupport());
				this.jdbc(seed.jdbc());
				this.lucene(seed.lucene());
				this.hbase(seed.hbase());
			}
		} else if("Site".equalsIgnoreCase(type)){
			boolean anno = clazz.isAnnotationPresent(Site.class);
			if(anno){
				Site seed = (Site) clazz.getAnnotation(Site.class);
				this.pageMode(PageMode.site);
				this.fetchUrl(seed.url());
				this.thread(seed.thread());
				this.timer(seed.startTime(), seed.interval());
				this.sleep(seed.sleep());
				HttpProxy hp = FileUtil.formatProxy(seed.proxy());
				this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
				this.userAgent(seed.userAgent());
				this.resourceSelector(seed.resourceSelector());
				this.downloadDisk(seed.downloadDisk());
				this.downloadHdfs(seed.downloadHdfs());
				this.javascriptSupport(seed.javascriptSupport());
				this.jdbc(seed.jdbc());
				this.lucene(seed.lucene());
				this.hbase(seed.hbase());
				this.elementSelectParser(seed.parser());
			}
		} else if("Single".equalsIgnoreCase(type)){
			boolean anno = clazz.isAnnotationPresent(Single.class);
			if(anno){
				Single seed = (Single) clazz.getAnnotation(Single.class);
				this.pageMode(PageMode.single);
				this.fetchUrl(seed.url());
				this.thread(seed.thread());
				this.timer(seed.startTime(), seed.interval());
				this.sleep(seed.sleep());
				HttpProxy hp = FileUtil.formatProxy(seed.proxy());
				this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
				this.userAgent(seed.userAgent());
				this.resourceSelector(seed.resourceSelector());
				this.downloadDisk(seed.downloadDisk());
				this.downloadHdfs(seed.downloadHdfs());
				this.javascriptSupport(seed.javascriptSupport());
				this.jdbc(seed.jdbc());
				this.lucene(seed.lucene());
				this.hbase(seed.hbase());
			}
		} else if("Cascade".equalsIgnoreCase(type)){
			boolean anno = clazz.isAnnotationPresent(Site.class);
			if(anno){//有两个Seed类，一个是annotation，一个是实体类
				Site seed = (Site) clazz.getAnnotation(Site.class);
				this.pageMode(PageMode.cascade);
				this.fetchUrl(seed.url());
				this.thread(seed.thread());
				this.timer(seed.startTime(), seed.interval());
				this.sleep(seed.sleep());
				HttpProxy hp = FileUtil.formatProxy(seed.proxy());
				this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
				this.userAgent(seed.userAgent());
				this.resourceSelector(seed.resourceSelector());
				this.downloadDisk(seed.downloadDisk());
				this.downloadHdfs(seed.downloadHdfs());
				this.javascriptSupport(seed.javascriptSupport());
				this.jdbc(seed.jdbc());
				this.lucene(seed.lucene());
				this.hbase(seed.hbase());
			}
		}

		this.parser(clazz);
		
		return this;
	}

	/**
	 * 创建爬虫
	 * @return
	 */
	public static Spider create() {
		// 关闭httpclient中的日志，否则信息打印太多了。
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		seed = new Seed(MD5Util.generateID());
		me = new Spider();
		return me;
	}

	/**
	 * 爬虫开启运行
	 * 检查Api设置是否设置正确，否则启动失败
	 */
	public void start(){
		if(StringUtil.isNullOrBlank(seed.getFetchUrl())){
			logger.error("没有配置要抓取的url。");
			System.exit(1);
		}
		SpiderEngine.create().setSeed(seed).build();
	}

	/**
	 * 通过配置文件启动爬虫的入口方法
	 * @param args
	 */
	public static void main(String... args) {
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		Context context = new Context(new CoreSeedsXmlHandler());
		List<Seed> seeds = context.load();

		context = new Context(new ConfigurationXmlHandler());
		Configuration configuration = context.load();

		SpiderEngine.create().setSeeds(seeds).setConfiguration(configuration).build();
		logger.info("爬虫启动开始...");
	}

}
