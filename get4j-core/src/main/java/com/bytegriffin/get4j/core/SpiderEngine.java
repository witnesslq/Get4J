package com.bytegriffin.get4j.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Configuration;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.download.DiskDownloader;
import com.bytegriffin.get4j.download.HdfsDownloader;
import com.bytegriffin.get4j.fetch.CascadeFetcher;
import com.bytegriffin.get4j.fetch.ListDetailFetcher;
import com.bytegriffin.get4j.fetch.SingleFetcher;
import com.bytegriffin.get4j.fetch.SiteFetcher;
import com.bytegriffin.get4j.net.http.HtmlUnitEngine;
import com.bytegriffin.get4j.net.http.HttpClientEngine;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.HttpProxy;
import com.bytegriffin.get4j.parse.AutoDelegateParser;
import com.bytegriffin.get4j.store.DBStorage;
import com.bytegriffin.get4j.store.FailUrlStorage;
import com.bytegriffin.get4j.store.FreeProxyStorage;
import com.bytegriffin.get4j.store.MongodbStorage;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.util.UrlQueue;

/**
 * 爬虫配置创建器 <br/>
 * 执行前的准备工作：组建工作流程
 */
public class SpiderEngine {

    private static SpiderEngine me;
    private List<Seed> seeds;
    private Configuration configuration;

    private static final Logger logger = LogManager.getLogger(SpiderEngine.class);

    private SpiderEngine() {
        super();
    }

    public static SpiderEngine create() {
        if (me == null) {
            me = new SpiderEngine();
        }
        return me;
    }

    /**
     * 构建爬虫参数
     */
    public void build() {
        buildProcess();
        buildConfiguration();
        buildTimer();
    }

    /**
     * 设置种子Seed
     *
     * @param seed seed
     * @return SpiderEngine
     */
    public SpiderEngine setSeed(Seed seed) {
        List<Seed> seeds = new ArrayList<>();
        seeds.add(seed);
        this.seeds = seeds;
        return this;
    }

    /**
     * 设置种子Seed列表
     *
     * @param seeds List<Seed>
     * @return SpiderEngine
     */
    public SpiderEngine setSeeds(List<Seed> seeds) {
        this.seeds = seeds;
        return this;
    }

    /**
     * 设置configuration配置
     *
     * @param configuration configuration
     * @return SpiderEngine
     */
    public SpiderEngine setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * 根据配置选择具体的Http探针<br>
     * 1.初始化Http引擎的部分参数<br>
     * 2.测试在具体Http引擎下的代理是否可用<br>
     *
     * @param seed seed
     */
    private void buildHttpEngine(Seed seed) {
        HttpEngine http;
        if (seed.isFetchJavascriptSupport()) {
            http = new HtmlUnitEngine();
            logger.info("启用HtmlUnit作为抓取引擎");
        } else {
            http = new HttpClientEngine();
            logger.info("启用HttpClient作为抓取引擎");
        }
        // 1.初始化httpclient部分参数
        http.init(seed);
        // 2.测试代理是否可用
        List<HttpProxy> hplist = seed.getFetchHttpProxy();
        if (hplist != null && hplist.size() > 0) {
            LinkedList<HttpProxy> newList = new LinkedList<>();
            for (HttpProxy httpProxy : hplist) {
                String furl = seed.getFetchUrl().replace(Constants.FETCH_LIST_URL_VAR_LEFT, "")
                        .replace(Constants.FETCH_LIST_URL_VAR_RIGHT, "");
                boolean isReached = http.testHttpProxy(furl, httpProxy);
                if (!isReached) {
                    logger.warn("Http代理[" + httpProxy.toString() + "]测试失效，请重新配置。");
                    newList.add(httpProxy);
                }
            }
            if (newList.size() == 0) {
                logger.error("启动失败：种子[" + seed.getSeedName() + "]测试Http代理全部失效，请重新配置。");
                System.exit(1);
            }
        }
        Constants.HTTP_ENGINE_CACHE.put(seed.getSeedName(), http);
    }

    /**
     * 第一步：根据配置文件或api动态地构建爬虫工作流程
     */
    private void buildProcess() {
        if (seeds == null || seeds.size() == 0) {
            logger.error("启动失败：请先设置种子Seed参数，才能构建爬虫引擎");
            System.exit(1);
        }
        for (Seed seed : seeds) {
            String seedName = seed.getSeedName();
            Chain chain = new Chain();
            if (StringUtil.isNullOrBlank(seed.getFetchUrl())) {
                logger.error("启动失败：种子[" + seedName + "]-[fetch.url]参数为必填项。");
                System.exit(1);
            }
            // 1.构建http探针
            buildHttpEngine(seed);

            // 2.设置流程
            StringBuilder subProcess = new StringBuilder();
            if (PageMode.single.equals(seed.getPageMode())) {
                SingleFetcher fe = new SingleFetcher();
                fe.init(seed);
                chain.addProcess(fe);
                subProcess.append("SingleFetcher");
            } else if (PageMode.cascade.equals(seed.getPageMode())) {
                CascadeFetcher mu = new CascadeFetcher();
                mu.init(seed);
                chain.addProcess(mu);
                subProcess.append("CascadeFetcher");
            } else if (PageMode.site.equals(seed.getPageMode())) {
                SiteFetcher ld = new SiteFetcher();
                ld.init(seed);
                chain.addProcess(ld);
                subProcess.append("SiteFetcher");
            } else if (PageMode.list_detail.equals(seed.getPageMode()) || seed.isListDetailMode()) {// 配置文件设置  或者  api设置两种判断
                ListDetailFetcher ld = new ListDetailFetcher();
                ld.init(seed);
                chain.addProcess(ld);
                subProcess.append("ListDetailFetcher");
            }

            if (!StringUtil.isNullOrBlank(seed.getDownloadDisk())) {
                Process p = new DiskDownloader();
                chain.addProcess(p);
                p.init(seed);
                subProcess.append("-DiskDownloader");
            } else if (!StringUtil.isNullOrBlank(seed.getDownloadHdfs())) {
                chain.addProcess(new HdfsDownloader());
                subProcess.append("-HdfsDownloader");
            }

            // if (!StringUtil.isNullOrBlank(seed.getExtractClassImpl())) {
            // chain.addProcess(new ExtractDispatcher());
            // subProcess.append("-Extract");
            // }

            if (!StringUtil.isNullOrBlank(seed.getParseClassImpl())) {
                AutoDelegateParser dp = new AutoDelegateParser();
                chain.addProcess(dp);
                dp.init(seed);
                int index = seed.getParseClassImpl().lastIndexOf(".") + 1;
                subProcess.append("-");
                subProcess.append(seed.getParseClassImpl().substring(index));
            } else if (!StringUtil.isNullOrBlank(seed.getParseElementSelector())) {
                AutoDelegateParser dp = new AutoDelegateParser();
                chain.addProcess(dp);
                dp.init(seed);
                subProcess.append("-ElementSelectPageParser");
            }

            if (!StringUtil.isNullOrBlank(seed.getStoreJdbc())) {
                DBStorage dbstorage = new DBStorage();
                dbstorage.init(seed);
                chain.addProcess(dbstorage);
                subProcess.append("-DBStorage");
            } else if (!StringUtil.isNullOrBlank(seed.getStoreMongodb())) {
                MongodbStorage mongodb = new MongodbStorage();
                mongodb.init(seed);
                chain.addProcess(mongodb);
                subProcess.append("-MongodbStorage");
            } else if (!StringUtil.isNullOrBlank(seed.getStoreFreeProxy())) {
                FreeProxyStorage freeProxyStorage = new FreeProxyStorage();
                freeProxyStorage.init(seed);
                chain.addProcess(freeProxyStorage);
                subProcess.append("-FreeProxyStorage");
            }
            // else if (!StringUtil.isNullOrBlank(seed.getStoreRedis())) {
            // chain.addProcess(new RedisStorage());
            // subProcess.append("-RedisStorage");
            // } else if (!StringUtil.isNullOrBlank(seed.getStoreLuceneIndex()))
            // {
            // chain.addProcess(new LuceneIndexStorage());
            // subProcess.append("-LuceneIndexStorage");
            // }

            // 添加坏链接存储功能
            FailUrlStorage.init(seed);

            // list_detail 已经在init方法中保存了未访问链接了，因为分页的问题要特殊处理
            if (!PageMode.list_detail.equals(seed.getPageMode())) {
                // 添加每个seed对应的未访问url
                UrlQueue.newUnVisitedLink(seedName, seed.getFetchUrl());
            }

            Constants.FETCH_PAGE_MODE_CACHE.put(seedName, seed.getPageMode());

            if (chain.list.size() > 0) {
                // 缓存每个site的工作流程
                Constants.CHAIN_CACHE.put(seed.getSeedName(), chain);
                logger.info("种子[" + seedName + "]流程[" + subProcess.toString() + "]设置完成。");
            } else {
                logger.error("启动失败：种子[" + seedName + "]流程设置失败，没有任何子流程加入，请重新配置。");
                System.exit(1);
            }
        }
    }

    /**
     * 第二步：创建工作环境
     */
    private void buildConfiguration() {
        if (configuration == null) {
            Constants.IS_KEEP_FILE_URL = false;
        } else if (Configuration.default_download_file_name_rule.equals(configuration.getDownloadFileNameRule())) {
            Constants.IS_KEEP_FILE_URL = false;
        } else {
            Constants.IS_KEEP_FILE_URL = true;
        }
    }

    /**
     * 第三步：启动定时器，按照配置的时间启动抓取任务
     */
    private void buildTimer() {
        for (Seed seed : seeds) {
            String interval = seed.getFetchInterval();
            String starttime = seed.getFetchStart();
            JobController job = new JobController(seed.getSeedName(), seed.getThreadNumber());
            Timer timer = new Timer();
            if (StringUtil.isNullOrBlank(starttime)) {
                logger.info("爬虫开始抓取[" + seed.getSeedName() + "]。。。");
                timer.schedule(job, 0L);
            } else if (StringUtil.isNullOrBlank(interval) || interval.equals("0")) {
                logger.info("爬虫开始抓取[" + seed.getSeedName() + "]。。。");
                timer.schedule(job, DateUtil.strToDate(starttime));
            } else {
                logger.info("爬虫开始抓取[" + seed.getSeedName() + "]。。。");
                timer.schedule(job, DateUtil.strToDate(starttime), Long.valueOf(interval) * 1000);
            }
        }
    }

}
