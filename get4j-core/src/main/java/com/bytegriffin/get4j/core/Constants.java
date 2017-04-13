package com.bytegriffin.get4j.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.Document;

import com.bytegriffin.get4j.fetch.FetchResourceSelector;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.HttpProxySelector;
import com.bytegriffin.get4j.net.http.SleepRandomSelector;
import com.bytegriffin.get4j.net.http.UserAgentSelector;
import com.bytegriffin.get4j.net.sync.Syncer;
import com.bytegriffin.get4j.parse.PageParser;
import com.gargoylesoftware.htmlunit.WebClient;
import com.mongodb.client.MongoCollection;

/**
 * 全局变量
 */
public final class Constants {

	/**
	 * 默认配置：比如下载目录为default时指的是/data/download/${seedname}目录下
	 */
	public final static String default_config = "default";
    /**
     * 爬虫 user agent 配置文件
     */
    public final static String user_agent_file = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "user_agent";
    /**
     * 爬虫 http proxy 配置文件
     */
    public final static String http_proxy_file = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "http_proxy";
    /**
     * 爬虫dump文件存储位置
     */
    public static final String dump_folder = System.getProperty("user.dir") + File.separator + "data" + File.separator + "dump" + File.separator;

    /**
     * 获取相应种子的下载地址
     *
     * @param seedName 种子名称
     * @return String
     */
    public static String getDownloadDisk(String seedName) {
        return System.getProperty("user.dir") + File.separator + "data" + File.separator + "download" + File.separator + seedName;
    }

    /**
     * 全局chain工作流缓存 key:seed_name value: site
     */
    static final Map<String, Chain> CHAIN_CACHE = new HashMap<>();

    /**
     * 全局http_proxy缓存 key:seed_name value: HttpProxySelector
     */
    public static final Map<String, HttpProxySelector> HTTP_PROXY_CACHE = new HashMap<>();

    /**
     * 全局user_agent缓存 key:seed_name value: UserAgentSelector
     */
    public static final Map<String, UserAgentSelector> USER_AGENT_CACHE = new HashMap<>();

    /**
     * 全局http请求间隔缓存 key:seed_name value: fetch.sleep
     */
    public static final Map<String, Long> FETCH_SLEEP_CACHE = new HashMap<>();

    /**
     * 全局sleep_selector缓存 key:seed_name value: SleepRangeSelector
     */
    public static final Map<String, SleepRandomSelector> FETCH_SLEEP_RANGE_CACHE = new HashMap<>();

    /**
     * 全局httpclientbuilder缓存 key:seed_name value: HttpClientBuilder
     */
    public static final Map<String, HttpClientBuilder> HTTP_CLIENT_BUILDER_CACHE = new HashMap<>();

    /**
     * 全局download dir缓存 key:seed_name value: download dir
     */
    public static final Map<String, String> DOWNLOAD_DIR_CACHE = new HashMap<>();

    /**
     * 全局PageMode缓存 key:seed_name value: PageMode
     */
    public static final Map<String, PageMode> FETCH_PAGE_MODE_CACHE = new HashMap<>();

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
     * Xml类型ConentType页面格式
     */
    public static final String XML_PAGE_SUFFIX = "xml";

    /**
     * LIST_DETAIL模式下的详情页面url选择
     */
    public static final Map<String, String> FETCH_DETAIL_SELECT_CACHE = new HashMap<>();

    /**
     * 全局FetchFilter缓存 key:seed_name value: FetchResourceUrl
     */
    public static final Map<String, FetchResourceSelector> FETCH_RESOURCE_SELECTOR_CACHE = new HashMap<>();

    /**
     * 自定义的page_parser解析器缓存
     */
    public static final Map<String, PageParser> PAGE_PARSER_CACHE = new HashMap<>();

    /**
     * key : seedName value: datasource
     */
    public static HashMap<String, DataSource> DATASOURCE_CACHE = new HashMap<>();

    /**
     * key : seedName value: MongoCollection
     */
    public static HashMap<String, MongoCollection<Document>> MONGO_COLLECTION_CACHE = new HashMap<>();

    /**
     * 全局http_proxy缓存 key:seed_name value: WebClient
     */
    public static final Map<String, WebClient> WEBCLIENT_CACHE = new HashMap<>();

    /**
     * 全局Http探针缓存 key:seed_name value: HttpProbe
     */
    public static final Map<String, HttpEngine> HTTP_ENGINE_CACHE = new HashMap<>();

    /**
     * 是否开启资源同步
     */
    public static boolean SYNC_OPEN;
    
    /**
     * 全局资源同步器
     */
    public static Syncer RESOURCE_SYNCHRONIZER;
    
    /**
     * 每次同步的最大值
     */
    public static int SYNC_PER_MAX_COUNT;
    
    /**
     * 每次同步的最大时间间隔，单位是秒
     */
    public static int SYNC_PER_MAX_INTERVAL;

}
