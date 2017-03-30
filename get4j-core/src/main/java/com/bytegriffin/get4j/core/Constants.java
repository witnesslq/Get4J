package com.bytegriffin.get4j.core;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.http.impl.client.HttpClientBuilder;

import com.bytegriffin.get4j.core.PageMode;
import com.bytegriffin.get4j.fetch.FetchResourceSelector;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.HttpProxySelector;
import com.bytegriffin.get4j.net.http.UserAgentSelector;
import com.bytegriffin.get4j.parse.PageParser;
import com.gargoylesoftware.htmlunit.WebClient;

/**
 * 全局变量
 */
public final class Constants {

	/**
	 * 全局chain工作流缓存 key:seed_name value: site
	 */
	public static final Map<String, Chain> CHAIN_CACHE = new HashMap<String, Chain>();

	/**
	 * 全局http_proxy缓存 key:seed_name value: HttpProxySelector
	 */
	public static final Map<String, HttpProxySelector> HTTP_PROXY_CACHE = new HashMap<String, HttpProxySelector>();

	/**
	 * 全局user_agent缓存 key:seed_name value: UserAgentLooper
	 */
	public static final Map<String, UserAgentSelector> USER_AGENT_CACHE = new HashMap<String, UserAgentSelector>();

	/**
	 * 全局httpclientbuilder缓存 key:seed_name value: HttpClientBuilder
	 */
	public static final Map<String, HttpClientBuilder> HTTP_CLIENT_BUILDER_CACHE = new HashMap<String, HttpClientBuilder>();

	/**
	 * 全局download dir缓存 key:seed_name value: download dir
	 */
	public static final Map<String, String> DOWNLOAD_DIR_CACHE = new HashMap<String, String>();

	/**
	 * 全局PageMode缓存 key:seed_name value: PageMode
	 */
	public static final Map<String, PageMode> FETCH_PAGE_MODE_CACHE = new HashMap<String, PageMode>();

	/**
	 * 抓取list url的左边可变字符串部分
	 */
	public static final String FETCH_LIST_URL_VAR_LEFT = "{";

	/**
	 * 抓取list url的右边可变字符串部分
	 */
	public static final String FETCH_LIST_URL_VAR_RIGHT = "}";
	
	/**
	 * 当fetch.resource.selector和fetch.resource.filter有多个值的时候之间用逗号隔开
	 */
	public static final String FETCH_RESOURCE_SPLIT = ",";

	/**
	 * JsonPath解析字符串的前缀，来判断是否是Jsoup的cssSelect还是JsonPath字符串
	 */
	public static final String JSON_PATH_PREFIX = "$.";
	
	/**
	 * 当fetch.detail.selector选择Json属性中内容是Html，并且Html中包含detail Link时，
	 * 这种特殊情况需要先写Jsonpath再写Jsoup选择器字符串，中间用竖杠隔开，例如： $.data.*|a.class[href]
	 */
	public static final String FETCH_DETAIL_JSON_HTML_SPLIT = "|";

	/**
	 * 是否为下载后的文件保留其url前缀 <br>
	 * false：表示自动补全文件名称<br>
	 * true：表示用url补全文件名称<br>
	 */
	public static boolean IS_KEEP_FILE_URL;

	/**
	 * 默认home page名称
	 */
	public static final String DEFAULT_HOME_PAGE_NAME = "index.html";

	/**
	 * 默认ContentType页面格式
	 */
	public static final String DEFAULT_PAGE_SUFFIX = "html";

	/**
	 * Json类型ContentType页面格式
	 */
	public static final String JSON_PAGE_SUFFIX = "json";

	/**
	 * LIST_DETAIL模式下的详情页面url选择
	 */
	public static final Map<String, String> FETCH_DETAIL_SELECT_CACHE = new HashMap<String, String>();
	
	/**
	 * 全局FetchFilter缓存 key:seed_name value: FetchResourceUrl
	 */
	public static final Map<String, FetchResourceSelector> FETCH_RESOURCE_SELECTOR_CACHE = new HashMap<String, FetchResourceSelector>();


	/**
	 * 自定义的page_parser解析器缓存
	 */
	public static final Map<String, PageParser> PAGE_PARSER_CACHE = new HashMap<String, PageParser>();

	/**
	 * key : seedName value: datasource
	 */
	public static HashMap<String, DataSource> DATASOURCE_CACHE = new HashMap<String, DataSource>();
	
	/**
	 * 全局http请求间隔缓存 key:seed_name value: fetch.sleep.timeout 
	 */
	public static final Map<String, Long> FETCH_SLEEP_TIMEOUT_CACHE = new HashMap<String, Long>();
	/**
	 * 全局http_proxy缓存 key:seed_name value: WebClient
	 */
	public static final Map<String, WebClient> WEBCLIENT_CACHE = new HashMap<String, WebClient>();
	/**
	 * 全局Http探针缓存 key:seed_name value: HttpProbe
	 */
	public static final Map<String, HttpEngine> HTTP_ENGINE_CACHE = new HashMap<String, HttpEngine>();
}
