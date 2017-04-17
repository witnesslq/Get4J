package com.bytegriffin.get4j.store;

import java.io.File;

import com.bytegriffin.get4j.core.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.util.ConcurrentQueue;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.UrlQueue;

/**
 * 坏链存储器<br>
 * 负责爬虫在爬取过程中访问不了或者根本不是链接的坏链存储在本地文件中
 */
public final class FailUrlStorage {

    private static final Logger logger = LogManager.getLogger(FailUrlStorage.class);
    private static final String filename = "fail_url";
    private static File failUrlFile = null;

    public static void init() {
        failUrlFile = FileUtil.makeDumpDir(Constants.dump_folder, filename);
        logger.info("爬虫系统的坏链文件的初始化完成。");
    }

    public static void dumpFile(String seedName) {
        ConcurrentQueue<String> failurls = UrlQueue.getFailVisitedUrl(seedName);
        if (failurls != null && failurls.size() > 0 && failurls.list.size() > 0) {
            FileUtil.append(failUrlFile, failurls.list);
            logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子[" + seedName + "]时一共有[" + failurls.size() + "]个坏链产生。");
        }
    }

}
