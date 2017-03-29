package com.bytegriffin.get4j.conf;

import java.io.File;

/**
 * 配置文件
 */
public abstract class AbstractConfig {

	/**
	 * 配置文件加载
	 */
	abstract Object load();

	// xml配置文件
	protected final static String core_seeds_xml_file = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "core-seeds.xml";
	// xml格式检验文件
	protected final static String core_seeds_xsd_file = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "core-seeds.xsd";
	// 全局xml配置文件
	protected final static String configuration_xml_file = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "configuration.xml";
	// user agent 配置文件
	public final static String user_agent_file = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "user_agent";
	// http proxy 配置文件
	public final static String http_proxy_file = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "http_proxy";

	/******** xml node ************/
	protected static final String seed_node = "seed";
	protected static final String name_node = "name";
	protected static final String property_node = "property";
	protected static final String value_node = "value";
	protected static final String configuration_node = "configuration";

	/******** xml node name ************/
	protected static final String woker_thread_number = "worker.thread.number";
	protected static final String fetch_page_mode = "fetch.page.mode";
	protected static final String fetch_url = "fetch.url";
	protected static final String fetch_detail_selector = "fetch.detail.selector";
	protected static final String fetch_total_pages = "fetch.total.pages";
	protected static final String fetch_login_username = "fetch.login.username";
	protected static final String fetch_login_password = "fetch.login.password";
	protected static final String fetch_timer_interval = "fetch.timer.interval";
	protected static final String fetch_timer_start = "fetch.timer.start";
	protected static final String fetch_http_proxy = "fetch.http.proxy";
	protected static final String fetch_http_user_agent = "fetch.http.user_agent";
	protected static final String fetch_sleep_timeout = "fetch.sleep.timeout";
	protected static final String fetch_javascript_support = "fetch.javascript.support";
	protected static final String fetch_resource_selector = "fetch.resource.selector";
	protected static final String download_disk = "download.disk";
	protected static final String download_resource_disk = "download.resource.disk";
	protected static final String download_hdfs = "download.hdfs";
	protected static final String parse_class_impl = "parse.class.impl";
	protected static final String parse_element_selector = "parse.element.selector";
	protected static final String store_redis = "store.redis";
	protected static final String store_jdbc = "store.jdbc";
	protected static final String store_lucene_index = "store.lucene.index";
	protected static final String download_file_name = "download.file.name";

}
