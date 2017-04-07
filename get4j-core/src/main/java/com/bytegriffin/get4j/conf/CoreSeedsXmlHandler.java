package com.bytegriffin.get4j.conf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.bytegriffin.get4j.util.StringUtil;

/**
 * core-sites.xml配置文件处理类
 */
public class CoreSeedsXmlHandler extends AbstractConfig {

    private static Logger logger = LogManager.getLogger(CoreSeedsXmlHandler.class);

    /**
     * 加载core-seeds.xml配置文件内容到内存中
     */
    public List<Seed> load() {
        XmlHelper.validate(core_seeds_xml_file, core_seeds_xsd_file);
        logger.info("正在读取xml配置文件[" + core_seeds_xml_file + "]......");
        Document doc = XmlHelper.loadXML(core_seeds_xml_file);
        Element seedNode = null;
        try{
            seedNode = doc.getRootElement();
        }catch(NullPointerException e){
            logger.error("读取的xml配置文件[" + core_seeds_xml_file + "]内容为空。");
            System.exit(1);
        }
        List<Element> siteElements = seedNode.elements(seed_node);
        List<Seed> seeds = new ArrayList<>();
        HashSet<String> hashset = new HashSet<>();//过滤相同siteName
        for (Element element : siteElements) {
            String seedName = element.element(name_node).getStringValue();
            Seed seed = new Seed(seedName);
            hashset.add(seedName);
            List<Element> propertyElements = element.elements(property_node);
            for (Element property : propertyElements) {
                String name = property.element(name_node).getStringValue();
                String value = property.element(value_node).getStringValue();
                if (name.equalsIgnoreCase(woker_thread_number)) {
                    if (!StringUtil.isNullOrBlank(value)) {
                        seed.setThreadNumber(Integer.valueOf(value));
                    }
                } else if (name.equalsIgnoreCase(fetch_url)) {
                    seed.setFetchUrl(value);
                } else if (name.equalsIgnoreCase(fetch_detail_selector)) {
                    seed.setFetchDetailSelector(value);
                } else if (name.equalsIgnoreCase(fetch_total_pages)) {
                    seed.setFetchTotalPages(value);
                } else if (name.equalsIgnoreCase(fetch_login_username)) {
                    seed.setFetchUsername(value);
                } else if (name.equalsIgnoreCase(fetch_login_password)) {
                    seed.setFetchPassword(value);
                } else if (name.equalsIgnoreCase(fetch_timer_interval)) {
                    seed.setFetchInterval(value);
                } else if (name.equalsIgnoreCase(fetch_timer_start)) {
                    seed.setFetchStart(value);
                } else if (name.equalsIgnoreCase(fetch_http_proxy)) {
                    seed.setFetchHttpProxyFile(value);
                } else if (name.equalsIgnoreCase(fetch_http_user_agent)) {
                    seed.setFetchUserAgentFile(value);
                } else if (name.equalsIgnoreCase(fetch_sleep)) {
                    if (!StringUtil.isNullOrBlank(value)) {
                        seed.setFetchSleep(Long.valueOf(value));
                    }
                } else if (name.equalsIgnoreCase(fetch_sleep_range)) {
                    seed.setFetchSleepRange(value);
                } else if (name.equalsIgnoreCase(fetch_page_mode)) {
                    seed.setPageMode(value);
                } else if (name.equalsIgnoreCase(fetch_resource_selector)) {
                    seed.setFetchResourceSelectors(value);
                } else if (name.equalsIgnoreCase(fetch_javascript_support)) {
                    if (!StringUtil.isNullOrBlank(value)) {
                        seed.setFetchJavascriptSupport(Boolean.valueOf(value));
                    }
                } else if (name.equalsIgnoreCase(download_disk)) {
                    seed.setDownloadDisk(value);
                } else if (name.equalsIgnoreCase(download_hdfs)) {
                    seed.setDownloadHdfs(value);
                } else if (name.equalsIgnoreCase(parse_class_impl)) {
                    seed.setParseClassImpl(value);
                } else if (name.equalsIgnoreCase(parse_element_selector)) {
                    seed.setParseElementSelector(value);
                } else if (name.equalsIgnoreCase(store_jdbc)) {
                    seed.setStoreJdbc(value);
                } else if (name.equalsIgnoreCase(store_mongodb)) {
                    seed.setStoreMongodb(value);
                } else if (name.equalsIgnoreCase(store_lucene_index)) {
                    seed.setStoreLuceneIndex(value);
                }
            }
            seeds.add(seed);
        }
        if (hashset.size() < siteElements.size()) {
            logger.error("xml配置文件[" + core_seeds_xml_file + "]中不能设置相同的Site Name，请重新检查。");
            System.exit(1);
        }
        logger.info("xml配置文件[" + core_seeds_xml_file + "]读取完成。");
        return seeds;
    }

}
