package com.bytegriffin.get4j.core;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.bytegriffin.get4j.util.StringUtil;
import com.jayway.jsonpath.JsonPath;

/**
 * Page对象
 */
public class Page {
	/**
	 * 数据库中主键
	 */
	private String id;
	/**
	 * 抓取url所属的网站
	 */
	private String siteUrl;
	/**
	 * 当前seed名称：对应于Seed，如果要抓取多个种子，请保证此名称唯一
	 */
	private String seedName;
	/**
	 * 当前页面title信息
	 */
	private String title;
	/**
	 * 当前页面的html内容
	 */
	private String htmlContent;
	/**
	 * 当前页面的json内容
	 */
	private String jsonContent;
	/**
	 * 当前页面的url
	 */
	private String url;
	/**
	 * Response的cookie字符串
	 */
	private String cookies;
	/**
	 * 当前页面中的资源文件：js、jpg、css等文件
	 */
	private HashSet<String> resources = new HashSet<String>();
	/**
	 * 当前资源文件存储路径
	 */
	private String resourceSavePath;
	/**
	 * 抓取时间
	 */
	private String fetchTime;
	/**
	 * 当启动list_detail抓取模式时，每个详情页对应的avatar资源
	 */
	private String avatar;
	/**
	 * 当启动list_detail抓取模式时，每个列表所对应的详情页
	 */
	private HashSet<Page> detailPages = new HashSet<Page>();

	/**
	 * 自定义动态字段
	 */
	private Map<String, Object> field = new HashMap<String, Object>();

	public Page() {
	}

	public Page(String seedName, String url) {
		this.seedName = seedName;
		this.url = url;
	}

	/**
	 * 包含Json格式
	 * 
	 * @return
	 */
	public boolean isJsonContent() {
		if (!StringUtil.isNullOrBlank(this.jsonContent)) {
			return true;
		}
		return false;
	}

	/**
	 * 包含Html格式
	 * 
	 * @return
	 */
	public boolean isHtmlContent() {
		if (!StringUtil.isNullOrBlank(this.htmlContent)) {
			return true;
		}
		return false;
	}

	/**
	 * 保证同一个SeedName下detailPages的唯一性
	 */
	@Override
	public int hashCode() {
		return 1;
	}

	/**
	 * 保证同一个SeedName下detailPages的唯一性
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Page p = (Page) obj;
		if (!this.seedName.equals(p.seedName)) {
			return false;
		}
		if (!this.url.equals(p.url)) {
			return false;
		}
		return true;
	}

	/**
	 * 根据JsonPath解析JsonContent
	 * 
	 * @param jsonPath
	 * @return
	 */
	public Object json(String jsonPath) {
		if (!this.isHtmlContent()) {
			return null;
		}
		Object obj = JsonPath.read(jsonContent, jsonPath);
		return obj;
	}

	/**
	 * 根据Jsoup原生支持的cssSelect或正则表达式解析Html
	 * 
	 * @param jsoupSelect
	 * @return
	 */
	public String jsoupText(String jsoupSelect) {
		Document doc = Jsoup.parse(this.htmlContent);
		Elements eles = doc.select(jsoupSelect);
		return eles.text();
	}

	/**
	 * 根据Jsoup原生支持的cssSelect或正则表达式解析Html
	 * 
	 * @param jsoupSelect
	 * @return
	 */
	public Elements jsoup(String jsoupSelect) {
		Document doc = Jsoup.parse(this.htmlContent);
		Elements eles = doc.select(jsoupSelect);
		return eles;
	}

	/**
	 * 根据抓取url来获取域名
	 * 
	 * @return
	 */
	public String getSiteUrl() {
		if (StringUtil.isNullOrBlank(this.url)) {
			return this.siteUrl;
		}
		try {
			URI uri = new URI(this.url);
			return uri.getAuthority();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return this.siteUrl;
	}

	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}

	/**
	 * 是否需要更新数据库中的page数据
	 * 
	 * @param page
	 * @return
	 */
	public boolean isRequireUpdate(Page dbPage) {
		boolean flag = false;
		if (null == dbPage) {
			return flag;
		}
		try {
			// title中也可能带有单引号之类的特殊字符，所以需要转义处理
			if (!(this.getTitle() == null ? dbPage.getTitle() == null : URLEncoder.encode(this.getTitle(),"UTF-8").equals(dbPage.getTitle()))) {
				flag = true;
			}
			//html页面首先要解码，因为之前存进去的时候是编码的（为了过滤某些单引号之类的字符串）
			if (!(this.getHtmlContent() == null ? dbPage.getHtmlContent() == null : URLEncoder.encode(this.getHtmlContent(),"UTF-8").equals(dbPage.getHtmlContent()))) {
				flag = true;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		if (!(this.getJsonContent() == null ? dbPage.getJsonContent() == null
				: this.getJsonContent().equals(dbPage.getJsonContent()))) {
			flag = true;
		}

		if (!(this.getCookies() == null ? dbPage.getCookies() == null : this.getCookies().equals(dbPage.getCookies()))) {
			flag = true;
		}
		
		if (!(this.getAvatar() == null ? dbPage.getAvatar() == null : this.getAvatar().equals(dbPage.getAvatar()))) {
			flag = true;
		}
		return flag;
	}

	public Object getField(String fieldKey) {
		return field.get(fieldKey);
	}

	public void putField(String fieldKey, Object fieldValue) {
		field.put(fieldKey, fieldValue);
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public String getHtmlContent() {
		return htmlContent;
	}

	public void setHtmlContent(String htmlContent) {
		this.htmlContent = htmlContent;
	}

	public String getJsonContent() {
		return jsonContent;
	}

	public void setJsonContent(String jsonContent) {
		this.jsonContent = jsonContent;
	}

	public String getSeedName() {
		return seedName;
	}

	public void setSeedName(String seedName) {
		this.seedName = seedName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public HashSet<String> getResources() {
		return resources;
	}

	public void setResources(HashSet<String> resources) {
		this.resources = resources;
	}

	public HashSet<Page> getDetailPages() {
		return detailPages;
	}

	public void setDetailPages(HashSet<Page> detailPages) {
		this.detailPages = detailPages;
	}



	public String getFetchTime() {
		return fetchTime;
	}

	public void setFetchTime(String fetchTime) {
		this.fetchTime = fetchTime;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getResourceSavePath() {
		return resourceSavePath;
	}

	public void setResourceSavePath(String resourceSavePath) {
		this.resourceSavePath = resourceSavePath;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCookies() {
		return cookies;
	}

	public void setCookies(String cookies) {
		this.cookies = cookies;
	}

}
