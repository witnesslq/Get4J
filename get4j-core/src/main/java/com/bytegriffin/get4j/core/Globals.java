package com.bytegriffin.get4j.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.lucene.index.IndexWriter;
import org.bson.Document;

import com.bytegriffin.get4j.fetch.FetchResourceSelector;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.HttpProxySelector;
import com.bytegriffin.get4j.net.http.SleepRandomSelector;
import com.bytegriffin.get4j.net.http.UserAgentSelector;
import com.bytegriffin.get4j.parse.PageParser;
import com.bytegriffin.get4j.probe.PageChangeProber;
import com.bytegriffin.get4j.send.EmailSender;
import com.gargoylesoftware.htmlunit.WebClient;
import com.mongodb.client.MongoCollection;

/**
 * 全局变量：缓存供全局访问的变量
 */
public final class Globals {

    /**
     * 当PageMode存放list列表url key：seed_name value：该seed下所有的列表url
     */
    public static Map<String, List<String>> LIST_URLS_CACHE = new HashMap<>();

    /**
     * 全局chain工作流缓存 key:seed_name value: site
     */
    public static final Map<String, Chain> CHAIN_CACHE = new HashMap<>();

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
    public static final Map<String, Integer> FETCH_SLEEP_CACHE = new HashMap<>();

    /**
     * 抓取页面变化探测器缓存 key:seed_name value: PageChangeProber
     */
    public static final Map<String, PageChangeProber> FETCH_PROBE_CACHE = new HashMap<>();

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
     * 全局lucene index dir缓存 key:seed_name value: lucene index dir
     */
    public static final Map<String, String> LUCENE_INDEX_DIR_CACHE = new HashMap<>();

    /**
     * 全局PageMode缓存 key:seed_name value: PageMode
     */
    public static final Map<String, PageMode> FETCH_PAGE_MODE_CACHE = new HashMap<>();

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
     * key : seedName value: IndexWriter
     */
    public static Map<String, IndexWriter> INDEX_WRITER_CACHE = new HashMap<>();

    /**
     * 全局http_proxy缓存 key:seed_name value: WebClient
     */
    public static final Map<String, WebClient> WEBCLIENT_CACHE = new HashMap<>();

    /**
     * 全局Http探针缓存 key:seed_name value: HttpEngine
     */
    public static final Map<String, HttpEngine> HTTP_ENGINE_CACHE = new HashMap<>();

    /**
     * 全局Email发送器，如果配置了email.recipient就表示当系统出现异常发送邮件提醒
     */
    public static EmailSender emailSender;
    
}
