package com.bytegriffin.get4j.download;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.PageMode;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.net.http.HttpClientEngine;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.StringUtil;

/**
 * 磁盘下载器，负责下载页面以及页面上的资源文件，它的功能是避免了开发者在PageParser中手工写下载页面或资源文件的代码。<br>
 * 注意：当启用List_detail模式并且detail页面存在的情况下，它特指的是avatar资源，即：与Detail_Link一一对应的资源，<br>
 * 而且fetch.resource.selector配置的是包含detail_link与avatar的css选择器或正则表达式，<br>
 * 如果在List_Detail模式下有特殊的资源下载方式，可以重新创建一种新的downloader或者 在PageParser中自己实现下载代码。<br>
 */
public class DiskDownloader implements Process {

    private static final Logger logger = LogManager.getLogger(DiskDownloader.class);
    // 静态资源服务器地址
    private static String staticServer = "";
    // 默认avatar存储路径，当配置静态服务器时系统会自动调用它暂存图片
    private static String defaultAvatarPath = "";

    // 创建每个site的文件夹
    public void init(Seed seed) {
        String diskpath = seed.getDownloadDisk();
        String folderName;
        if (diskpath.startsWith("http")) {
            defaultAvatarPath = System.getProperty("user.dir") + File.separator + "data" + File.separator + "download"
                    + File.separator + seed.getSeedName();
            staticServer = diskpath.endsWith("/") ? diskpath : diskpath + "/";// 静态资源服务器地址
            folderName = FileUtil.makeDownloadDir(defaultAvatarPath);// 获取默认的服务器磁盘地址
        } else {
            folderName = FileUtil.makeDownloadDir(diskpath);// 获取用户配置的磁盘地址
        }
        folderName = folderName.endsWith(File.separator)? folderName : folderName + File.separator;
        Constants.DOWNLOAD_DIR_CACHE.put(seed.getSeedName(), folderName);
        logger.info("Seed[" + seed.getSeedName() + "]的组件ResourceDiskDownloader的初始化完成。");
    }

    @Override
    public void execute(Page page) {
        // 1.在磁盘上生成页面
        PageMode fm = Constants.FETCH_PAGE_MODE_CACHE.get(page.getSeedName());
        if (!PageMode.list_detail.equals(fm)) {// 当启动list_detail模式，默认不会下载页面的
            FileUtil.downloadPagesToDisk(page);
        }

        // 2.下载页面中的资源文件
        HttpClientEngine.downloadResources(page);

        // 3.判断是否包含avatar资源，有的话就下载
        boolean isSync = false;
        if (!StringUtil.isNullOrBlank(page.getAvatar())) {
            isSync = true;
            HttpClientEngine.downloadAvatar(page);// 下载avatar资源
            String avatar = page.getAvatar().replace(defaultAvatarPath, staticServer);
            page.setAvatar(avatar);// 将本地avatar资源文件的路径修改为静态服务器地址
        }

        // 4.另开一个线程专门负责启用脚本同步avatar资源文件
        if (isSync && !StringUtil.isNullOrBlank(defaultAvatarPath)) {
            ExecutorService executorService = Executors.newCachedThreadPool();
            executorService.submit(new SyncAvatar(defaultAvatarPath));
            executorService.shutdown();
        }

        // 5.设置page的资源保存路径属性
        page.setResourceSavePath(Constants.DOWNLOAD_DIR_CACHE.get(page.getSeedName()));
        logger.info("线程[" + Thread.currentThread().getName() + "]下载种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]完成。");
    }

    /**
     * 同步Avatar资源文件
     */
    static class SyncAvatar implements Runnable {

        private String syncPath;

        SyncAvatar(String syncPath) {
            this.syncPath = syncPath;
        }

        @Override
        public void run() {

        }

    }

    public static void main(String... args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(new SyncAvatar(""));
        System.err.println("==============");
        executorService.shutdown();
    }

}
