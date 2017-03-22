package com.bytegriffin.download;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.conf.Seed;
import com.bytegriffin.core.Constants;
import com.bytegriffin.core.Page;
import com.bytegriffin.core.Process;
import com.bytegriffin.net.http.HttpClientEngine;
import com.bytegriffin.util.FileUtil;
import com.bytegriffin.util.StringUtil;

/**
 * 磁盘下载器，负责下载页面以及页面上的资源文件，它的功能是避免了开发者在PageParser中手工写下载页面或资源文件的代码。<br>
 * 注意：当启用List_detail模式时，它特指的是avatar资源，即：与Detail_Link一一对应的资源，<br>
 * 而且fetch.resource.selector配置的是包含detail_link与avatar的css选择器或正则表达式，<br>
 * 如果在List_Detail模式下有特殊的资源下载方式，可以重新创建一种新的downloader或者
 * 在PageParser中自己实现下载代码。<br>
 */
public class DiskDownloader implements Process{

	private static final Logger logger = LogManager.getLogger(DiskDownloader.class);
	// 静态资源服务器地址
	private static String staticServer = "";
	// 默认avatar存储路径，当配置静态服务器时系统会自动调用它暂存图片
	private static String defaultAvatarPath = "";
	// 当启动list_detail模式，并且配置resource.selector时，是否会默认下载页面 true：会 flase:不会
	public static final boolean isDownloadPage = false;

	// 创建每个site的文件夹
	public void init(Seed seed) {
		String diskpath = seed.getDownloadDisk();
		String folderName = null;
		if(diskpath.startsWith("http")){
			defaultAvatarPath = System.getProperty("user.dir") + File.separator + "data" + File.separator + "download" + File.separator+ seed.getSeedName() + File.separator;
			staticServer = diskpath.endsWith("/") ? diskpath : diskpath +"/";// 静态资源服务器地址
			folderName =  FileUtil.makeDownloadDir(defaultAvatarPath);// 获取默认的服务器磁盘地址
		} else {
			folderName = FileUtil.makeDownloadDir(diskpath);//获取用户配置的磁盘地址
		}
		Constants.DOWNLOAD_DIR_CACHE.put(seed.getSeedName(), folderName);
		logger.info("Seed[" + seed.getSeedName() + "]的组件ResourceDiskDownloader的初始化完成。");
	}

	@Override
	public void execute(Page page){
		HashSet<Page> detailPages = page.getDetailPages();
		// 注意：此时不能用FetchMode来判断，因为list_detail模式下有种情况就是只包含list页面，没有detail页面的情况
		if(detailPages != null && detailPages.size() > 0){
			// 1.在磁盘上生成list页面
			if(isDownloadPage){
				FileUtil.downloadPagesToDisk(page);
			}
			// 2.下载list页面中的资源文件
			HttpClientEngine.downloadResources(page);
			// 3.在磁盘上生成detail页面 以及 在磁盘上下载detail页面的资源文件
			boolean isSync = false;
			for(Page detailPage : detailPages){// 下载资源文件
				//3.1 下载detail页面
				if(isDownloadPage){
					FileUtil.downloadPagesToDisk(detailPage);
				}
				//3.2 判断是否启动了list_detail模式下的avatar资源抓取
				if(!StringUtil.isNullOrBlank(detailPage.getAvatar())){
					isSync = true;
					HttpClientEngine.downloadAvatar(detailPage);//下载avatar资源
					String avatar = detailPage.getAvatar().replace(defaultAvatarPath, staticServer);
					detailPage.setAvatar(avatar);//将本地avatar资源文件的路径修改为静态服务器地址
				}
				//3.3 如果fetch.resource.selector配置了all或者none
				HttpClientEngine.downloadResources(detailPage);
			}
			// 4.另开一个线程专门负责启用脚本同步avatar资源文件
			if(isSync && !StringUtil.isNullOrBlank(defaultAvatarPath)){
				ExecutorService executorService = Executors.newCachedThreadPool();  
				executorService.submit(new SyncAvatar(defaultAvatarPath));
				executorService.shutdown();
			}
			// 5.设置page的资源保存路径属性
			page.setResourceSavePath(Constants.DOWNLOAD_DIR_CACHE.get(page.getSeedName()));
		} else {// 其它情况都只生成主页面
			//1.在磁盘上生成页面
			if(isDownloadPage){
				FileUtil.downloadPagesToDisk(page);
			}
			//2.下载页面中的资源文件
			HttpClientEngine.downloadResources(page);
		}
		logger.info("线程[" + Thread.currentThread().getName() + "]下载种子Seed["+page.getSeedName()+"]完成。");
	}

	/**
	 * 同步Avatar资源文件
	 */
	static class SyncAvatar implements Runnable{

		public String syncPath;

		SyncAvatar(String syncPath){
			this.syncPath = syncPath;
		}

		@Override
		public void run() {

		}

	}
	
	public static void main(String... args){
		ExecutorService executorService = Executors.newCachedThreadPool();  
		executorService.submit(new SyncAvatar(""));System.err.println("==============");
		executorService.shutdown();
	}

}
