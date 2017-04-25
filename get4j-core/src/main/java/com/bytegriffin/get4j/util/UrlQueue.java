package com.bytegriffin.get4j.util;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Url队列 <br>
 * 负责爬虫中全部的url处理 <br>
 */
public final class UrlQueue {

    //key: seed name  value: 该seed所涉及的所有已访问过的链接
    private static ConcurrentHashMap<String, ConcurrentQueue<String>> VISITED_LINKS = new ConcurrentHashMap<>();
    //key: seed name  value: 该seed所涉及的所有未访问过的链接
    private static ConcurrentHashMap<String, ConcurrentQueue<String>> UN_VISITED_LINKS = new ConcurrentHashMap<>();
    //key: seed name  value: 该seed所涉及的所有访问失败的所有url
    private static ConcurrentHashMap<String, ConcurrentQueue<String>> FAIL_VISITED_URLS = new ConcurrentHashMap<>();
    //key: seed name  value: 该seed所涉及的所有已访问过的资源文件 css/js等
    private static ConcurrentHashMap<String, ConcurrentQueue<String>> VISITED_RESOURCES = new ConcurrentHashMap<>();
    //key: seed name  value: 该seed所涉及的所有未访问过的资源文件 css/js等
    private static ConcurrentHashMap<String, ConcurrentQueue<String>> UN_VISITED_RESOURCES = new ConcurrentHashMap<>();

    /**
     * 追加未访问的links队列<br/>
     * 首先判断新抓取的link是否在已访问的队列中，<br/>
     * 然后判断是否在未抓取的队列中<br/>
     * 如果都不在的话则将其加进未访问的队列中<br/>
     *
     * @param seedName String
     * @param links    HashSet<String>
     */
    public static void addUnVisitedLinks(String seedName, HashSet<String> links) {
        if (links == null || links.size() == 0) {
            return;
        }
        for (String link : links) {
            ConcurrentQueue<String> constr = getVisitedLink(seedName);
            if (constr == null || !constr.contains(link)) {
                newUnVisitedLink(seedName, link);
            }
        }
    }

    /**
     * 追加已访问的links队列<br/>
     * 首先判断新抓取的link是否在已访问的队列中，<br/>
     * 然后判断是否在未抓取的队列中<br/>
     * 如果都不在的话则将其加进未访问的队列中<br/>
     *
     * @param seedName String
     * @param links    HashSet<String>
     */
    public static void addVisitedLinks(String seedName, HashSet<String> links) {
        if (links == null || links.size() == 0) {
            return;
        }
        for (String link : links) {
            ConcurrentQueue<String> constr = getVisitedLink(seedName);
            if (constr == null || !constr.contains(link)) {
                newVisitedLink(seedName, link);
            }
        }
    }

    /**
     * 新增未访问的link到队列中
     *
     * @param seedName String
     * @param link     String
     */
    public static void newUnVisitedLink(String seedName, String link) {
        ConcurrentQueue<String> queue = UrlQueue.UN_VISITED_LINKS.get(seedName);
        if (queue == null) {
            queue = new ConcurrentQueue<>();
            UrlQueue.UN_VISITED_LINKS.put(seedName, queue);
        }
        queue.add(link);
    }

    /**
     * 新增未访问的resource到队列中
     *
     * @param seedName     String
     * @param resourceLink String
     */
    public static void newUnVisitedResource(String seedName, String resourceLink) {
        ConcurrentQueue<String> queue = UrlQueue.UN_VISITED_RESOURCES.get(seedName);
        if (queue == null) {
            queue = new ConcurrentQueue<>();
            UrlQueue.UN_VISITED_RESOURCES.put(seedName, queue);
        }
        queue.add(resourceLink);
    }

    /**
     * 新增已访问的link到队列中
     *
     * @param seedName String
     * @param link     String
     */
    public static void newVisitedLink(String seedName, String link) {
        ConcurrentQueue<String> queue = UrlQueue.VISITED_LINKS.get(seedName);
        if (queue == null) {
            queue = new ConcurrentQueue<>();
            UrlQueue.VISITED_LINKS.put(seedName, queue);
        }
        queue.add(link);
    }

    /**
     * 新增已访问的resource到队列中
     *
     * @param seedName String
     * @param resource String
     */
    public static void newVisitedResource(String seedName, String resource) {
        ConcurrentQueue<String> queue = UrlQueue.VISITED_RESOURCES.get(seedName);
        if (queue == null) {
            queue = new ConcurrentQueue<>();
            UrlQueue.VISITED_RESOURCES.put(seedName, queue);
        }
        queue.add(resource);
    }

    /**
     * 增加已访问失败的url（包括link、资源文件）
     *
     * @param seedName String
     * @param failurl  String
     */
    public static void newFailVisitedUrl(String seedName, String failurl) {
        ConcurrentQueue<String> queue = UrlQueue.FAIL_VISITED_URLS.get(seedName);
        if (queue == null) {
            queue = new ConcurrentQueue<>();
            UrlQueue.FAIL_VISITED_URLS.put(seedName, queue);
        }
        queue.add(failurl);
    }

    /**
     * 获取未访问link队列
     *
     * @param seedName String
     * @return ConcurrentQueue<String>
     */
    public static ConcurrentQueue<String> getUnVisitedLink(String seedName) {
        return UN_VISITED_LINKS.get(seedName);
    }

    /**
     * 获取未访问resource队列
     *
     * @param seedName String
     * @return ConcurrentQueue<String>
     */
    public static ConcurrentQueue<String> getUnVisitedResource(String seedName) {
        return UN_VISITED_RESOURCES.get(seedName);
    }


    /**
     * 获取已访问link队列
     *
     * @param seedName String
     * @return ConcurrentQueue<String>
     */
    public static ConcurrentQueue<String> getVisitedLink(String seedName) {
        return VISITED_LINKS.get(seedName);
    }

    /**
     * 获取已访问resource队列
     *
     * @param seedName String
     * @return ConcurrentQueue<String>
     */
    public static ConcurrentQueue<String> getVisitedResource(String seedName) {
        return VISITED_RESOURCES.get(seedName);
    }

    /**
     * 获取已访问失败的url队列
     *
     * @param seedName String
     * @return ConcurrentQueue<String>
     */
    public static ConcurrentQueue<String> getFailVisitedUrl(String seedName) {
        return FAIL_VISITED_URLS.get(seedName);
    }

}
