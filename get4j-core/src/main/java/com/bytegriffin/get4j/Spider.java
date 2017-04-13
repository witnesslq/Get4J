package com.bytegriffin.get4j;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.annotation.Cascade;
import com.bytegriffin.get4j.annotation.ListDetail;
import com.bytegriffin.get4j.annotation.Single;
import com.bytegriffin.get4j.annotation.Site;
import com.bytegriffin.get4j.annotation.Sync;
import com.bytegriffin.get4j.conf.AbstractConfig;
import com.bytegriffin.get4j.conf.Configuration;
import com.bytegriffin.get4j.conf.ConfigurationXmlHandler;
import com.bytegriffin.get4j.conf.Context;
import com.bytegriffin.get4j.conf.CoreSeedsXmlHandler;
import com.bytegriffin.get4j.conf.ResourceSync;
import com.bytegriffin.get4j.conf.ResourceSyncYamlHandler;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
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
    
    private static ResourceSync resourceSync;

    private Spider() {
    }

    private Spider(PageMode pageMode) {
        pageMode(pageMode);
    }

    /**
     * 设置ftp为资源同步方式
     * @param host 服务器地址
     * @param port 端口号，默认为21
     * @param username 用户名（可以为空）
     * @param password 密码（可以为空）
     * @param dir ftp目录
     * @return Spider
     */
    public Spider ftp(String host,int port, String username, String password) {
    	Map<String,String> ftp = new HashMap<>();
    	ftp.put(AbstractConfig.host_node, host);
    	ftp.put(AbstractConfig.port_node, String.valueOf(port));
    	ftp.put(AbstractConfig.username_node, username);
    	ftp.put(AbstractConfig.password_node, password);
    	resourceSync.setFtp(ftp);
    	Map<String,String> sync = new HashMap<>();
    	sync.put(AbstractConfig.open_node, "true");
    	sync.put(AbstractConfig.batch_count_node, "10");
    	sync.put(AbstractConfig.batch_time_node, "10");
    	sync.put(AbstractConfig.protocal_node, AbstractConfig.ftp_node);
    	resourceSync.setSync(sync);	
        return this;
    }
    
    /**
     * 设置rsync为资源同步方式 <br>
     * 注意：暂时不支持windows
     * @param host 服务器地址
     * @param username 用户名
     * @param isModule 是否为module模式，是为true，不是则代表远程目录为false
     * @param moduleOrDir module模式或者远程dir目录，如果是module模式，密码需
     * 要在服务器端配置；如果是远程dir，需要ssh-keygen配置无密码登陆
     * @return Spider
     */
    public Spider rsync(String host,String username, boolean isModule, String moduleOrDir) {
    	Map<String,String> rsync = new HashMap<>();
    	rsync.put(AbstractConfig.host_node, host);
    	rsync.put(AbstractConfig.username_node, username);
    	if(isModule){
    		rsync.put(AbstractConfig.module_node, moduleOrDir);
    	} else {
    		rsync.put(AbstractConfig.dir_node, moduleOrDir);
    	}
    	resourceSync.setRsync(rsync);
    	Map<String,String> sync = new HashMap<>();
    	sync.put(AbstractConfig.open_node, "true");
    	sync.put(AbstractConfig.batch_count_node, "10");
    	sync.put(AbstractConfig.batch_time_node, "10");
    	sync.put(AbstractConfig.protocal_node, AbstractConfig.rsync_node);
    	resourceSync.setSync(sync);	
    	return this;
    }
    
    /**
     * 设置scp为资源同步方式 <br>
     * 需要ssh-keygen配置无密码登陆
     * @param host 服务器地址
     * @param username 登陆的用户名
     * @param dir 服务器端目录
     * @param port scp端口号，默认为22
     * @return Spider
     */
    public Spider scp(String host,String username, String dir, Integer port) {
    	Map<String,String> scp = new HashMap<>();
    	scp.put(AbstractConfig.host_node, host);
    	scp.put(AbstractConfig.username_node, username);
    	scp.put(AbstractConfig.dir_node, dir);
    	port = port == null ? 22 : port;
    	scp.put(AbstractConfig.port_node, String.valueOf(port));
    	resourceSync.setScp(scp);
    	Map<String,String> sync = new HashMap<>();
    	sync.put(AbstractConfig.open_node, "true");
    	sync.put(AbstractConfig.batch_count_node, "10");
    	sync.put(AbstractConfig.batch_time_node, "10");
    	sync.put(AbstractConfig.protocal_node, AbstractConfig.rsync_node);
    	resourceSync.setSync(sync);	
    	return this;
    }

    /**
     * 设置种子名称<br>
     * 每个种子名称要唯一<br>
     *
     * @param seedName 种子名称
     * @return Spider
     */
    public Spider seedName(String seedName) {
        seed.setSeedName(seedName);
        return this;
    }


    /**
     * 抓取的页面模型<br>
     * 必填项。list_detail（列表-详情页面）、single（单个页面)、site（单个站点）、cascade（单页面上的所有链接）
     * 默认值是single。
     *
     * @param pageMode 页面模型
     * @return Spider
     */
    private Spider pageMode(PageMode pageMode) {
        seed.setPageMode(pageMode);
        return this;
    }

    /**
     * 设置抓取url <br>
     * 必填项。表示要抓取的Url，如果抓取模式pageMode为list_detail，该值为列表Url，
     * 其中可变的页数PageNum需要用大括号{}括起来
     *
     * @param fetchUrl 抓取url
     * @return Spider
     */
    public Spider fetchUrl(String fetchUrl) {
        seed.setFetchUrl(fetchUrl);
        return this;
    }

    /**
     * 爬虫工作线程数量，非必填项。默认值是1。
     *
     * @param num 爬虫线程数
     * @return Spider
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
     *
     * @param detailSelector 详情页面选择器
     * @return Spider
     */
    public Spider detailSelector(String detailSelector) {
        seed.setFetchDetailSelector(detailSelector);
        return this;
    }

    /**
     * 抓取的列表总页数 <br>
     * 非必填项，当抓取的页面格式属于【列表-详情】页时使用，动态获取页面中显示的总页数。
     * 支持Jsoup原生的选择器（html内容）或Jsonpath（json内容），默认值是1。
     *
     * @param totalPageSelector 总页数选择器
     * @return Spider
     */
    public Spider totalPages(String totalPageSelector) {
        seed.setFetchTotalPages(totalPageSelector);
        return this;
    }

    /**
     * 抓取的列表总页数 <br>
     * 非必填项，当抓取的页面格式属于【列表-详情】页时使用，直接定义抓取页数，默认值是1。
     *
     * @param totalPageNum 总页数
     * @return Spider
     */
    public Spider totalPages(int totalPageNum) {
        seed.setFetchTotalPages(String.valueOf(totalPageNum));
        return this;
    }

    /**
     * 抓取延迟策略<br>
     * 非必填项。每两次http请求之间的间隔时间(单位：秒)，以防止频繁访问站点抓取不到内容，默认值为0
     *
     * @param seconds 每次请求延迟，单位秒
     * @return Spider
     */
    public Spider sleep(long seconds) {
        seed.setFetchSleep(seconds);
        return this;
    }

    /**
     * 抓取随机延迟策略<br>
     * 非必填项。格式：2-7 表示2秒到7秒直接随机延迟，其中范围中间用横杠连接时间的上下限<br>
     * 每两次http请求之间的间隔时间(单位：秒)范围，每次请求时间间隔都是这个范围内的随机值，
     * 主要用于某些网站防爬策略检测出固定时间间隔访问网站的请求，默认值为0
     *
     * @param lower 时间波动下限
     * @param upper 时间波动上限
     * @return Spider
     */
    public Spider sleepRange(int lower, int upper) {
        seed.setFetchSleepRange(lower + "-" + upper);
        return this;
    }

    /**
     * 抓取随机延迟策略<br>
     *
     * @param timerange 格式：2-7
     * @return Spider
     */
    private Spider sleepRange(String timerange) {
        seed.setFetchSleepRange(timerange);
        return this;
    }

    /**
     * 抓取启动器<br>
     * startTime表示爬虫抓取的开始时间，格式为：2001-10-10 23:29:02，如果startTime已经过时，爬虫会立刻执行
     *
     * @param startTime 抓取的开始时间
     * @return Spider
     */
    public Spider timer(String startTime) {
        seed.setFetchStart(startTime);
        return this;
    }

    /**
     * 抓取启动器<br>
     * firstTime表示爬虫第一次的抓取时间，格式为：2001-10-10 23:29:02，如果firstTime已经过时，爬虫会立刻执行 <br>
     * interval表示爬虫重复抓取的时间间隔，单位是秒
     *
     * @param firstTime 第一次的抓取时间
     * @param interval  下次抓取的时间间隔
     * @return Spider
     */
    public Spider timer(String firstTime, Long interval) {
        seed.setFetchStart(firstTime);
        if (interval != null) {
            seed.setFetchInterval(String.valueOf(interval));
        }
        return this;
    }

    /**
     * 资源选择器，支持Jsoup原生的选择器（html内容）或Jsonpath（json内容）<br>
     * 当此内容为JsonPath字符串的时候，如果json中提供的detail的链接是相对路径，那么此时这个值<br>
     * 的格式为：链接前缀+jsonpath。如果需要抓取多种资源可以用逗号","隔开，默认不填是全抓取。<br>
     * 注意：当启用list_detail模式时，资源特指的是avatar资源，即：与Detail_Link一一对应的资源，<br>
     * 此项必须配置为包含detail_link与avatar的定位符；当启用非list_detail模式时，可抓取多种资源，每个选择器中间用逗号隔开。
     * 
     * @param resourceSelector Jsoup选择器支持的字符串
     * @return Spider
     */
    public Spider resourceSelector(String resourceSelector) {
        seed.setFetchResourceSelectors(resourceSelector);
        return this;
    }

    /**
     * 是否支持Javascript，有些网站需要等待javascript来生成结果，此时可以将此属性设为true，
     * 默认值是false，慎用：抓取效率会变慢
     *
     * @param ifSupport 是否支持
     * @return Spider
     */
    public Spider javascriptSupport(boolean ifSupport) {
        seed.setFetchJavascriptSupport(ifSupport);
        return this;
    }

    /**
     * 设置代理<br>
     * 爬虫会自动检测，如果代理不能用，会立刻停止
     *
     * @param ip   代理ip
     * @param port 代理端口
     * @return Spider
     */
    public Spider proxy(String ip, Integer port) {
        HttpProxy hp = new HttpProxy(ip, port);
        List<HttpProxy> list = new ArrayList<>();
        list.add(hp);
        seed.setFetchHttpProxy(list);
        return this;
    }

    /**
     * 设置一组代理<br>
     * 爬虫会自动检测，如果代理不能用，会立刻停止
     *
     * @param list 代理列表
     * @return Spider
     */
    public Spider proxys(List<HttpProxy> list) {
        seed.setFetchHttpProxy(list);
        return this;
    }

    /**
     * 加载系统自带的http_proxy文件
     * 注意：调用此方法之前请确保conf/http_proxy文件中有可用代理
     *
     * @return Spider
     */
    public Spider defaultProxy() {
        seed.setFetchHttpProxyFile(Constants.http_proxy_file);
        return this;
    }

    /**
     * 伪造一个UserAgent
     *
     * @param userAgent 自定义useragent字符串
     * @return Spider
     */
    public Spider userAgent(String userAgent) {
        List<String> list = new LinkedList<>();
        list.add(userAgent);
        seed.setFetchUserAgent(list);
        return this;
    }

    /**
     * 伪造一个列表的UserAgent
     *
     * @param userAgents 自定义useragent字符串
     * @return Spider
     */
    public Spider userAgents(List<String> userAgents) {
        seed.setFetchUserAgent(userAgents);
        return this;
    }

    /**
     * 默认加载系统自带的UserAgent
     *
     * @return Spider
     */
    public Spider defaultUserAgent() {
        seed.setFetchUserAgentFile(Constants.user_agent_file);
        return this;
    }

    /**
     * 下载本地根路径，默认地址为$path/data/download/
     * 子目录是${seedName}
     * @param disk 磁盘路径
     * @return Spider
     */
    public Spider downloadDisk(String disk) {
        seed.setDownloadDisk(disk);
        return this;
    }

    private boolean isDefaultDownload = false;

    /**
     * 默认的下载本地路径
     * 默认地址：$path/data/download/${seedname}
     *
     * @return Spider
     */
    public Spider defaultDownloadDisk() {
        isDefaultDownload = true;
        return this;
    }

    /**
     * 下载到hdfs路径
     *
     * @param hdfs hdfs地址
     * @return Spider
     */
    public Spider downloadHdfs(String hdfs) {
        seed.setDownloadHdfs(hdfs);
        return this;
    }

    /**
     * 自定义页面解析类
     *
     * @param parser 自定义解析类
     * @return Spider
     */
    public Spider parser(Class<?> parser) {
        seed.setParseClassImpl(parser.getName());
        return this;
    }

    /**
     * 单个页面元素解析内部类，设置了此项就不能设置自定义的解析类了
     *
     * @param elementSelector Jsoup支持的选择器字符串
     * @return Spider
     */
    public Spider elementSelectParser(String elementSelector) {
        seed.setParseElementSelector(elementSelector);
        return this;
    }

    /**
     * 将解析的结果保存到Mysql中
     * jdbc格式：jdbc:mysql://localhost:3306/spider?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root
     *
     * @param jdbc 需要把用户名与密码也写到url的参数中
     * @return Spider
     */
    public Spider jdbc(String jdbc) {
        seed.setStoreJdbc(jdbc);
        return this;
    }

    /**
     * 将解析的结果保存到MongoDB中
     * mongodb://localhost:27017 或者加密 mongodb://user1:pwd1@host1/?authSource=db1&ssl=true 或者多个 mongodb://host1:27017,host2:27017
     *
     * @param mongodburl
     * @return
     */
    public Spider mongodb(String mongodburl) {
        seed.setStoreMongodb(mongodburl);
        return this;
    }

    /**
     * 将解析结果保存到Lucene索引
     *
     * @param indexPath Lucene索引存储的磁盘路径
     * @return Spider
     */
    public Spider lucene(String indexPath) {
        seed.setStoreLuceneIndex(indexPath);
        return this;
    }

    /**
     * 将解析结构保存到hbase数据库中
     *
     * @param address hbase地址
     * @return Spider
     */
    public Spider hbase(String address) {
        seed.setStoreLuceneIndex(address);
        return this;
    }

    /**
     * annotation入口，如果不想一项一项设置Api，也可以写一个annotation
     * annotation类：ListDetail（列表-详情页面）、Single（单个页面，不抓取页面上链接)、Cascade（单个页面，包括所有链接）、Site（单个站点）
     *
     * @param clazz 自定义Annotation类
     * @return Spider
     * @throws Exception
     */
    public static Spider annotation(Class<?> clazz) throws Exception {
        // 关闭httpclient中的日志，否则信息打印太多了。
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        me = new Spider();
        seed = new Seed();
        resourceSync = new ResourceSync();
        return me.getAnnotation(clazz);
    }

    /**
     * 动态判断Annotation
     *
     * @param clazz clazz
     * @return Spider
     */
    private Spider getAnnotation(Class<?> clazz) {
        Annotation[] ans = clazz.getDeclaredAnnotations();
        if (ans == null || ans.length == 0) {
            logger.error("类[" + clazz.getName() + "]没有配置任何Annotation。");
            System.exit(1);
        }
        for(Annotation an : ans){
        	String type = an.annotationType().getSimpleName();
            if ("ListDetail".equalsIgnoreCase(type)) {
                boolean anno = clazz.isAnnotationPresent(ListDetail.class);
                if (anno) {
                    ListDetail seed = (ListDetail) clazz.getAnnotation(ListDetail.class);
                    this.pageMode(PageMode.list_detail);
                    this.fetchUrl(seed.url());
                    this.detailSelector(seed.detailSelector());
                    this.totalPages(seed.totolPages());
                    this.thread(seed.thread());
                    this.timer(seed.startTime(), seed.interval());
                    this.sleep(seed.sleep());
                    this.sleepRange(seed.sleepRange());
                    HttpProxy hp = FileUtil.formatProxy(seed.proxy());
                    if (hp != null) {
                        this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
                    }
                    this.userAgent(seed.userAgent());
                    this.resourceSelector(seed.resourceSelector());
                    this.downloadDisk(seed.downloadDisk());
                    this.downloadHdfs(seed.downloadHdfs());
                    this.javascriptSupport(seed.javascriptSupport());
                    this.jdbc(seed.jdbc());
                    this.lucene(seed.lucene());
                    this.hbase(seed.hbase());
                }
            } else if ("Site".equalsIgnoreCase(type)) {
                boolean anno = clazz.isAnnotationPresent(Site.class);
                if (anno) {
                    Site seed = (Site) clazz.getAnnotation(Site.class);
                    this.pageMode(PageMode.site);
                    this.fetchUrl(seed.url());
                    this.thread(seed.thread());
                    this.timer(seed.startTime(), seed.interval());
                    this.sleep(seed.sleep());
                    this.sleepRange(seed.sleepRange());
                    HttpProxy hp = FileUtil.formatProxy(seed.proxy());
                    if (hp != null) {
                        this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
                    }
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
            } else if ("Single".equalsIgnoreCase(type)) {
                boolean anno = clazz.isAnnotationPresent(Single.class);
                if (anno) {
                    Single seed = (Single) clazz.getAnnotation(Single.class);
                    this.pageMode(PageMode.single);
                    this.fetchUrl(seed.url());
                    this.thread(seed.thread());
                    this.timer(seed.startTime(), seed.interval());
                    this.sleep(seed.sleep());
                    this.sleepRange(seed.sleepRange());
                    HttpProxy hp = FileUtil.formatProxy(seed.proxy());
                    if (hp != null) {
                        this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
                    }
                    this.userAgent(seed.userAgent());
                    this.resourceSelector(seed.resourceSelector());
                    this.downloadDisk(seed.downloadDisk());
                    this.downloadHdfs(seed.downloadHdfs());
                    this.javascriptSupport(seed.javascriptSupport());
                    this.jdbc(seed.jdbc());
                    this.lucene(seed.lucene());
                    this.hbase(seed.hbase());
                }
            } else if ("Cascade".equalsIgnoreCase(type)) {
                boolean anno = clazz.isAnnotationPresent(Site.class);
                if (anno) {//有两个Seed类，一个是annotation，一个是实体类
                    Cascade seed = (Cascade) clazz.getAnnotation(Site.class);
                    this.pageMode(PageMode.cascade);
                    this.fetchUrl(seed.url());
                    this.thread(seed.thread());
                    this.timer(seed.startTime(), seed.interval());
                    this.sleep(seed.sleep());
                    this.sleepRange(seed.sleepRange());
                    HttpProxy hp = FileUtil.formatProxy(seed.proxy());
                    if (hp != null) {
                        this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
                    }
                    this.userAgent(seed.userAgent());
                    this.resourceSelector(seed.resourceSelector());
                    this.downloadDisk(seed.downloadDisk());
                    this.downloadHdfs(seed.downloadHdfs());
                    this.javascriptSupport(seed.javascriptSupport());
                    this.jdbc(seed.jdbc());
                    this.lucene(seed.lucene());
                    this.hbase(seed.hbase());
                }
            }  else if ("Sync".equalsIgnoreCase(type)) {
            	boolean anno = clazz.isAnnotationPresent(Sync.class);
            	if(anno){
            		Sync sync = (Sync) clazz.getAnnotation(Sync.class);
            		if(AbstractConfig.ftp_node.equals(sync.protocal())){
            			this.ftp(sync.host(), sync.port(), sync.username(), sync.password());
            		} else if(AbstractConfig.rsync_node.equals(sync.protocal())){
            			this.rsync(sync.host(), sync.username(), sync.isModule(), sync.module());
            		} else if(AbstractConfig.scp_node.equals(sync.protocal())){
            			this.scp(sync.host(), sync.username(), sync.dir(), sync.port());
            		}
            	}
            	
            }
        }
        
        this.parser(clazz);
        return this;
    }

    /**
     * 创建针对列表-详情页面格式的爬虫<br>
     *
     * @return Spider
     */
    public static Spider list_detail() {
        // 关闭httpclient中的日志，否则信息打印太多了。
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        seed = new Seed();
        resourceSync = new ResourceSync();
        me = new Spider(PageMode.list_detail);
        return me;
    }

    /**
     * 创建针对单个页面（不抓取页面上的链接）的爬虫<br>
     *
     * @return Spider
     */
    public static Spider single() {
        // 关闭httpclient中的日志，否则信息打印太多了。
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        seed = new Seed();
        resourceSync = new ResourceSync();
        me = new Spider(PageMode.single);
        return me;
    }

    /**
     * 创建针对单页面上（包括所有链接）格式的爬虫<br>
     *
     * @return Spider
     */
    public static Spider cascade() {
        // 关闭httpclient中的日志，否则信息打印太多了。
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        seed = new Seed();
        resourceSync = new ResourceSync();
        me = new Spider(PageMode.cascade);
        return me;
    }

    /**
     * 创建针对整站（不包括外链）格式的爬虫<br>
     *
     * @return Spider
     */
    public static Spider site() {
        // 关闭httpclient中的日志，否则信息打印太多了。
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        seed = new Seed();
        resourceSync = new ResourceSync();
        me = new Spider(PageMode.site);
        return me;
    }

    /**
     * 爬虫开启运行
     * 检查Api设置是否设置正确，否则启动失败
     */
    public void start() {
        // 自动生成seed name
        if (StringUtil.isNullOrBlank(seed.getSeedName())) {
            seed.setSeedName(MD5Util.generateSeedName());
        }
        if (isDefaultDownload) {
            seed.setDownloadDisk(Constants.getDownloadDisk(seed.getSeedName()));
        }
        if (StringUtil.isNullOrBlank(seed.getFetchUrl())) {
            logger.error("种子[" + seed.getSeedName() + "]没有配置要抓取的url。");
            System.exit(1);
        }
        SpiderEngine.create().setSeed(seed).setResourceSync(resourceSync).build();
    }

    /**
     * 获取互联网上免费代理<br>
     * 并自动将可用的代理保存到本地http_proxy文件中<br>
     * 此方法一般在启动爬虫之前使用，下次在启动爬虫时需要代理时，
     * 可以直接调用defaultProxy方法即可
     */
    public static void initFreeProxy() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        List<Seed> seeds = new ArrayList<Seed>();
        Seed xicidaili = new Seed("xicidaili");
        xicidaili.setPageMode(PageMode.list_detail);
        xicidaili.setFetchUrl("http://www.xicidaili.com/nn/{1}");
        xicidaili.setThreadNumber(1);
        xicidaili.setFetchTotalPages("5");
        xicidaili.setParseClassImpl("com.bytegriffin.get4j.parse.FreeProxyPageParser");
        xicidaili.setStoreFreeProxy(Constants.http_proxy_file);
        xicidaili.setFetchUserAgentFile(Constants.user_agent_file);
        seeds.add(xicidaili);
        SpiderEngine.create().setSeeds(seeds).build();
    }

    /**
     * 通过配置文件启动爬虫的入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String... args) {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        Context context = new Context(new CoreSeedsXmlHandler());
        List<Seed> seeds = context.load();

        context = new Context(new ResourceSyncYamlHandler());
        ResourceSync synchronizer = context.load();

        context = new Context(new ConfigurationXmlHandler());
        Configuration configuration = context.load();

        SpiderEngine.create().setSeeds(seeds).setResourceSync(synchronizer).setConfiguration(configuration).build();
        logger.info("爬虫启动开始...");
    }

}
