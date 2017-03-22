package com.bytegriffin.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.conf.Configuration;
import com.bytegriffin.conf.Seed;
import com.bytegriffin.download.DiskDownloader;
import com.bytegriffin.download.HdfsDownloader;
import com.bytegriffin.fetch.CascadeFetcher;
import com.bytegriffin.fetch.FetchMode;
import com.bytegriffin.fetch.ListDetailFetcher;
import com.bytegriffin.fetch.SingleFetcher;
import com.bytegriffin.fetch.SiteFetcher;
import com.bytegriffin.net.http.HttpClientEngine;
import com.bytegriffin.net.http.HttpProxy;
import com.bytegriffin.net.http.HttpUnitEngine;
import com.bytegriffin.net.http.HttpEngine;
import com.bytegriffin.parse.AutoDelegateParser;
import com.bytegriffin.store.DBStorage;
import com.bytegriffin.util.DateUtil;
import com.bytegriffin.util.StringUtil;
import com.bytegriffin.util.UrlQueue;

/**
 * 爬虫配置创建器 <br/>
 * 执行前的准备工作：组建工作流程
 */
public class SpiderEngine {

	public static SpiderEngine me;
	private List<Seed> seeds;
	private Configuration configuration;

	private static final Logger logger = LogManager.getLogger(SpiderEngine.class);

	private SpiderEngine() {
        super();
    }

	public static SpiderEngine create() {
		if(me == null){
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
	 * @param seed
	 * @return
	 */
	public SpiderEngine setSeed(Seed seed) {
		List<Seed> seeds= new ArrayList<Seed>();
		seeds.add(seed);
		this.seeds = seeds;
		return this;
	}
	
	/**
	 * 设置种子Seed列表
	 * @param seeds
	 * @return
	 */
	public SpiderEngine setSeeds(List<Seed> seeds) {
		this.seeds = seeds;
		return this;
	}

	/**
	 * 设置configuration配置
	 * @param configuration
	 * @return
	 */
	public SpiderEngine setConfiguration(Configuration configuration) {
		this.configuration = configuration;
		return this;
	}
	
	/**
	 * 根据配置选择具体的Http探针<br>
	 * 1.初始化Http引擎的部分参数<br>
	 * 2.测试在具体Http引擎下的代理是否可用<br>
	 * @param seed
	 */
	private void buildHttpEngine(Seed seed){
		HttpEngine probe = null;
		if(seed.isFetchJavascriptSupport()){
			probe = new HttpUnitEngine();
			logger.info("启用HttpUnit作为抓取引擎");
		} else {
			probe = new HttpClientEngine();
			logger.info("启用HttpClient作为抓取引擎");
		}
		//1.初始化httpclient部分参数
		probe.init(seed);
		//2.测试代理是否可用
		List<HttpProxy> hplist = seed.getFetchHttpProxy();
		if(hplist != null && hplist.size() > 0){
			int reached_count = 0;
			LinkedList<HttpProxy> newList = new LinkedList<HttpProxy>();
			for(HttpProxy httpProxy : hplist){
				String furl = seed.getFetchUrl().replace(Constants.FETCH_LIST_URL_VAR_LEFT, "").replace(Constants.FETCH_LIST_URL_VAR_RIGHT, "");
				boolean isReached = probe.testHttpProxy(furl, httpProxy);
				if(isReached){
					reached_count ++;
				} else {
					logger.warn("Http代理["+httpProxy.toString()+"]测试失效，请重新配置。");
					newList.add(httpProxy);
				}
			}
			if(reached_count == 0){
				logger.error("种子["+seed.getSeedName()+"]测试Http代理全部失效，请重新配置。");
				System.exit(1);
			}
		}
		Constants.HTTPPROBE_CACHE.put(seed.getSeedName(), probe);
	}

	/**
	 * 第一步：根据配置文件动态地构建爬虫工作流程
	 */
	private void buildProcess() {
		if(seeds == null || seeds.size() == 0){
			logger.error("请先设置种子Seed参数，才能构建爬虫引擎");
			System.exit(1);
		}
		for (Seed seed : seeds) {
			String seedName = seed.getSeedName();
			Chain chain = new Chain();
			if (StringUtil.isNullOrBlank(seed.getFetchUrl())) {
				logger.error("种子[" + seed.getSeedName() + "]-[fetch.url]参数为必填项。");
				System.exit(1);
			}
			//1.构建http探针
			buildHttpEngine(seed);

			//2.设置流程
			StringBuilder subProcess = new StringBuilder();
			if (FetchMode.single.equals(seed.getFetchMode())) {
				SingleFetcher fe = new SingleFetcher();
				fe.init(seed);
				chain.addProcess(fe);
				subProcess.append("SingleFetcher");
			} else if (FetchMode.cascade.equals(seed.getFetchMode())) {
				CascadeFetcher mu = new CascadeFetcher();
				mu.init(seed);
				chain.addProcess(mu);
				subProcess.append("CascadeFetcher");
			} else if (FetchMode.site.equals(seed.getFetchMode())) {
				SiteFetcher ld = new SiteFetcher();
				ld.init(seed);
				chain.addProcess(ld);
				subProcess.append("SiteFetcher");
			} else if (FetchMode.list_detail.equals(seed.getFetchMode()) || seed.isListDetailMode()) {
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

//			if (!StringUtil.isNullOrBlank(seed.getExtractClassImpl())) {
//				chain.addProcess(new ExtractDispatcher());
//				subProcess.append("-Extract");
//			}

			if (!StringUtil.isNullOrBlank(seed.getParseClassImpl())) {
				AutoDelegateParser dp = new AutoDelegateParser();
				chain.addProcess(dp);
				dp.init(seed);
				subProcess.append("-CustomPageParser");
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
			} 
//			else if (!StringUtil.isNullOrBlank(seed.getStoreRedis())) {
//				chain.addProcess(new RedisStorage());
//				subProcess.append("-RedisStorage");
//			} else if (!StringUtil.isNullOrBlank(seed.getStoreLuceneIndex())) {
//				chain.addProcess(new LuceneIndexStorage());
//				subProcess.append("-LuceneIndexStorage");
//			}


			if (!FetchMode.list_detail.equals(seed.getFetchMode())) {
				// 添加每个seed对应的未访问url
				UrlQueue.newUnVisitedLink(seedName, seed.getFetchUrl());
			}

			Constants.FETCH_MODE_CACHE.put(seedName, seed.getFetchMode());

			if (chain.list.size() > 0) {
				// 缓存每个site的工作流程
				Constants.CHAIN_CACHE.put(seed.getSeedName(), chain);
				logger.info("site[" + seed.getSeedName() + "]流程[" + subProcess.toString() + "]设置完成。");
			} else {
				logger.error("site[" + seed.getSeedName() + "]流程设置失败，没有任何子流程加入，请检查配置文件。");
				System.exit(1);
			}
		}
	}

	/**
	 * 第二步：创建工作环境
	 */
	private void buildConfiguration() {
		if(configuration == null){
			Constants.IS_KEEP_FILE_URL = false;
		}else if (Configuration.Auto_Download_File_Name.equals(configuration.getDownloadFileNameRule())) {
			Constants.IS_KEEP_FILE_URL = false;
		} else {
			Constants.IS_KEEP_FILE_URL = true;
		}
	}

	/**
	 * 第三步：启动定时器，按照配置的时间启动抓取任务
	 */
	public void buildTimer() {
		for (Seed seed : seeds) {
			String interval = seed.getFetchInterval();
			String starttime = seed.getFetchStart();
			JobController job = new JobController(seed.getSeedName(), seed.getThreadNumber());
			Timer timer = new Timer();
			if (StringUtil.isNullOrBlank(interval) && StringUtil.isNullOrBlank(starttime)) {
				logger.info("爬虫开始抓取[" + seed.getSeedName() + "]。。。");
				timer.schedule(job, 0l);
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
