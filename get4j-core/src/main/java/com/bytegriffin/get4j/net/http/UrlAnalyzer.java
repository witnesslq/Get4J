package com.bytegriffin.get4j.net.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.fetch.FetchResourceSelector;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.util.UrlQueue;

/**
 * Url分析器：负责解析页面所有的url
 */
public final class UrlAnalyzer {

	private static final Logger logger = LogManager.getLogger(UrlAnalyzer.class);
	private Page page;
	private final static UrlAnalyzer singleton = new UrlAnalyzer();

	private UrlAnalyzer() {
	}

	private UrlAnalyzer setPage(Page page) {
		this.page = page;
		return this;
	}

	/**
	 * 自定义设置JsoupHelper
	 * 
	 * @param page
	 * @param resourceSelector
	 * @return
	 */
	public static UrlAnalyzer custom(Page page) {
		return singleton.setPage(page);
	}

	/**
	 * 获取html中的title属性
	 * 
	 * @param html
	 * @return
	 */
	public static String getTitle(String html) {
		Document document = Jsoup.parse(html);
		String title = document.title();
		return title;
	}

	/**
	 * 获取html中指定的元素
	 * 
	 * @param page
	 * @param select
	 * @return
	 */
	public static String select(Page page, String select) {
		Document document = Jsoup.parse(page.getHtmlContent(), page.getUrl());
		Elements eles = document.select(select);
		return eles.text();
	}
	
	/**
	 * 判断Html中select标签中option的值<br>
	 * [option value='http://www.aaa.com/bbb']这种情况下出现的url比较特殊，<br>
	 * 一是出现url的情况不多，二是不好判断哪个属于相对路径，只能判断出绝对路径来
	 * @param doc
	 * @param urls
	 */
	private void addOptionUrl(Document doc, HashSet<String> urls){
		Elements opteles = doc.select("option[value]");// select_option
		for (Element link : opteles) {
			String absLink = link.absUrl("value");
			if (isStartHttpUrl(absLink)) {
				urls.add(absLink);
			}
		}
	}

	/**
	 * link链接嗅探：<br>
	 * 获取并设置Html页面中的所有非资源的链接 <br/>
	 * 用HashSet保存来保证url的唯一性 <br/>
	 * 如果该url是一个资源文件（图片、js、css等）的话，那么将其保存到resource中。 <br/>
	 * 如果该url就是普通链接的话，返回并将其加载到内存中
	 * 
	 * @return
	 */
	public final HashSet<String> sniffAllLinks() {
		HashSet<String> urls = new HashSet<String>();
		if (page.isHtmlContent()) {// html格式会启动jsoup抓取各种资源的src和href
			String siteUrl = page.getUrl();
			Document doc = Jsoup.parse(page.getHtmlContent(), siteUrl);
			Elements eles = doc.select("a[href], frame[src], iframe[src], area[src]");// a标签、frame标签、iframe标签、map>area标签
			HashSet<String> href = getAllUrlByElement(eles);// a标签、frame标签、iframe标签、map>area标签
			HashSet<String> resources = page.getResources();
			// 过滤<a>标签中的资源
			for (String link : href) {
				if (!FetchResourceSelector.isFindResources(link)) {
					urls.add(link);
				} else {
					resources.add(link);
				}
			}

			// 寻找并增加<select> <options value="http://www.aaa.com/bb">中的url
			addOptionUrl(doc, urls);

		} else { // json格式：程序会自动嗅探出所有非资源文件的带绝对路径的url
			HashSet<String> url = sniffUrlFromJson(page.getJsonContent(), false);
			urls.addAll(url);
		}

		return urls;
	}

	/**
	 * 单个site站点中Html页面（非Json格式）的link链接嗅探：<br>
	 * 获取并设置Html页面中的所有非资源的链接 <br/>
	 * 用HashSet保存来保证url的唯一性 <br/>
	 * 如果该url是一个资源文件（图片、js、css等）的话，那么将其保存到resource中。 <br/>
	 * 如果该url就是普通链接的话，返回并将其加载到内存中
	 * 
	 * @return
	 */
	public final HashSet<String> sniffSiteLinks() {
		HashSet<String> urls = new HashSet<String>();
		String siteUrl = page.getUrl();
		String siteprefix = "";// 网站域名前缀，如果不是这个前缀开头，程序会认为是外链，不会抓取
		try {
			URI uri = new URI(siteUrl);
			String domain = uri.getAuthority();
			String path = uri.getPath();
			siteprefix = domain + path.substring(0, path.lastIndexOf("/") + 1);
		} catch (URISyntaxException e) {
			logger.error("线程[" + Thread.currentThread().getName() + "]嗅探种子[" + page.getSeedName() + "]在嗅探整站链接提取url前缀时出错：", e);
		}
		if (page.isHtmlContent()) {// html格式会启动jsoup抓取各种资源的src和href

			String content = page.getHtmlContent();
			if (StringUtil.isNullOrBlank(content)) {
				return null;
			}
			Document doc = Jsoup.parse(content, siteUrl);
			Elements eles = doc.select("a[href], frame[src], iframe[src], area[src]");// a标签、frame标签、iframe标签、map>area标签
			HashSet<String> href = getAllUrlByElement(eles);// a标签、frame标签、iframe标签、map>area标签
			HashSet<String> resources = page.getResources();
			
			// 过滤<a>标签中的资源，剩下来的都是可访问的url链接，但是有些动态资源文件也是没有后缀名的链接，
			// 对于这种情况，需要请求服务器根据返回的Response.ContentType来判断处理才行，因此默认会把这种链接当成非资源型的link来处理
			for (String link : href) {
				if (link.contains(siteprefix) && !FetchResourceSelector.isFindResources(link)) {
					urls.add(link);
				} else {
					resources.add(link);
				}
			}

			// 寻找并增加<select> <options value="http://www.aaa.com/bb">中的url
			addOptionUrl(doc, urls);

		} else { // json格式
			HashSet<String> url = sniffUrlFromJson(page.getJsonContent(), false);
			urls.addAll(url);
		}

		return urls;
	}

	/**
	 * List_Detail模式下List列表中的Detail_link链接嗅探：<br>
	 * 获取并设置Html页面中的所有非资源的链接 <br/>
	 * 用HashSet保存来保证url的唯一性 <br/>
	 * 如果该url是一个资源文件（图片、js、css等）的话，那么将其保存到resource中。 <br/>
	 * 如果该url就是普通链接的话，返回并将其加载到内存中
	 * 
	 * @return
	 */
	public final HashSet<String> sniffDetailLinks() {
		String detailSelect = Constants.FETCH_DETAIL_SELECT_CACHE.get(page.getSeedName());
		if(StringUtil.isNullOrBlank(detailSelect)){
			return null;
		}
		HashSet<String> urls = new HashSet<String>();
		if (page.isHtmlContent()) {// html格式会启动jsoup抓取detail链接
			String siteUrl = page.getUrl();
			String content = page.getHtmlContent();
			if (StringUtil.isNullOrBlank(content)) {
				return null;
			}
			Document doc = Jsoup.parse(content, siteUrl);
			Elements eles = doc.select(detailSelect);// a标签
			HashSet<String> href = getAllUrlByElement(eles);// a标签
			// 过滤<a>标签中的资源，
			for (String link : href) {
				urls.add(link);
			}
		} else { // json格式：通过jsonpath来获取detail链接
			if (detailSelect.startsWith(Constants.JSON_PATH_PREFIX)) {// json字符串里的detail页面提供的是绝对路径
				if(detailSelect.contains(Constants.FETCH_DETAIL_JSON_HTML_SPLIT)){ // 特殊情况：当Json属性中包含Html，并且Html中存在Detail Link时，之间用逗号隔开，所以需要jsonpath和jsoup两个解析
					List<String> contents = FetchResourceSelector.jsonPath2List(page.getJsonContent(), detailSelect, "");
					for(String field : contents){
						if(field == null){
							continue;
						}
						Document doc = Jsoup.parseBodyFragment(field, page.getUrl());
						Elements eles = doc.select(detailSelect);
						HashSet<String> href = getAllUrlByElement(eles);// a标签
						// 过滤<a>标签中的资源，
						for (String link : href) {
							urls.add(link);
						}
					}
				} else {
					urls = FetchResourceSelector.jsonPath(page.getJsonContent(), detailSelect, "");
				}
			} else if (detailSelect.contains(Constants.JSON_PATH_PREFIX)) { // json字符串里的detail页面提供的是相对路径
				String[] str = detailSelect.split("\\" + Constants.JSON_PATH_PREFIX);
				String urlpath = str[0];
				String jsonpath = Constants.JSON_PATH_PREFIX + str[1];
				urls = FetchResourceSelector.jsonPath(page.getJsonContent(), jsonpath, urlpath);
			}
		}
		return urls;
	}

	/**
	 * 针对Html和Json两种格式资源（图片、css、js、视频等）自动嗅探并设置：<br>
	 * 
	 * @return
	 */
	public final void sniffAndSetResources() {
		HashSet<String> resources = new HashSet<String>();
		FetchResourceSelector resourceselector = Constants.FETCH_RESOURCE_SELECTOR_CACHE.get(page.getSeedName());
		if (resourceselector == null || resourceselector.isConfigAll()) {// 如果ResourceSelector配置了all或者默认没有配置此项
			if (page.isHtmlContent()) {// html格式在all模式下会启动jsoup抓取各种资源的src和href
				String siteUrl = page.getUrl();
				Document doc = Jsoup.parse(page.getHtmlContent(), siteUrl);
				//注意link[href]有时候是xml文件，例如：<link type="application/rss+xml" href="rss"/><link type="application/wlwmanifest+xml" href="wlwmanifest.xml"/>
				Elements eles = doc.select("link[href], script[src], img[src], embed[src], video[src], audio[src], track[src]");// css、script、img、flv|mp4|mp3|ogg、srt
				Elements ahref = doc.select("a[href~=(?i)." + FetchResourceSelector.BINARY_FILTERS + "]");// 这个是有具体后缀名的链接集合，例如：<a href="abc.doc/exe/....">
				HashSet<String> resourceLink = getAllUrlByElement(eles);
				HashSet<String> abinary = getAllUrlByElement(ahref);
				if (resourceLink.size() > 0) {
					resources.addAll(resourceLink);
				}
				if (abinary.size() > 0) {
					resources.addAll(abinary);
				}
			} else if(page.isJsonContent()) { // json格式在all模式下，也会自动嗅探出资源文件
				HashSet<String> resourceurl = sniffUrlFromJson(page.getJsonContent(), true);
				resources.addAll(resourceurl);
			} else {// 这种情况就是动态url是资源文件的情况，所以htmlContent与jsonContent全部为空
				resources.add(page.getUrl());
			}
		} else if (resourceselector.isConfigNone()) {// 如果ResourceSelector配置了none
			// 什么都不做，即表示：什么资源都不抓取，全部过滤
		} else { // 如果配置了具体参数，则表示抓取符合参数的具体资源
			List<String> selectors = resourceselector.getSelectors();
			if (page.isJsonContent()) { // json格式
				for (String select : selectors) {
					String[] str = select.split("\\" + Constants.JSON_PATH_PREFIX);
					String urlpath = str[0];
					String jsonpath = Constants.JSON_PATH_PREFIX + str[1];
					HashSet<String> url = FetchResourceSelector.jsonPath(page.getJsonContent(), jsonpath, urlpath);
					resources.addAll(url);
				}
			} else {// html格式
				for (String select : selectors) {
					if(StringUtil.isNullOrBlank(select)){
						continue;
					}
					HashSet<String> url = resourceselector.cssSelect(page, select);
					resources.addAll(url);
				}
			}
		}
		page.setResources(resources);
	}

	/**
	 * 使list页面中的avatar资源与detail link一一映射
	 * @return map key：detail_link value：avatar_link
	 */
	public Map<String, String> mappingDetailLinkAndAvatar(){
		String detailSelect = Constants.FETCH_DETAIL_SELECT_CACHE.get(page.getSeedName());
		Map<String, String> map = new HashMap<String, String>();
		FetchResourceSelector resourceselector = Constants.FETCH_RESOURCE_SELECTOR_CACHE.get(page.getSeedName());
		List<String> selectors = resourceselector.getSelectors();
		if (page.isJsonContent()) { // json格式：只支持avatar资源与detaillink一一映射的结果
			List<String> detailink = FetchResourceSelector.jsonPath2List(page.getJsonContent(), detailSelect, "");
			List<String> avatars = new ArrayList<String>();
			for (String select : selectors) {// 最多只有一条记录
				String[] str = select.split("\\" + Constants.JSON_PATH_PREFIX);
				String urlpath = str[0];
				String jsonpath = Constants.JSON_PATH_PREFIX + str[1];
				avatars = FetchResourceSelector.jsonPath2List(page.getJsonContent(), jsonpath, urlpath);
			}
			if(detailink.size() == avatars.size()){//json字符串中只有detaillink与avatar资源一一对应才行，否则没办法实现映射
				for(int i=0; i < detailink.size(); i++){
					String dlink = detailink.get(i);
					if (StringUtil.isNullOrBlank(dlink) || dlink.startsWith("#") || dlink.equalsIgnoreCase("null") 
							|| dlink.contains("javascript:") || dlink.contains("mailto:") || dlink.contains("about:blank")){
						continue;
					}			
					if(dlink.startsWith(".")){
						dlink = dlink.replace(".", "");
					}
					dlink = getAbsoluteURL(page.getUrl(), dlink);
					//当然第一个img如果是空，就获取第二个
					String avatar = avatars.get(i);
					if (StringUtil.isNullOrBlank(avatar) || avatar.startsWith("#") || avatar.equalsIgnoreCase("null")){
						avatar = null;
					}
					if(!StringUtil.isNullOrBlank(avatar)){
						avatar =  getAbsoluteURL(page.getUrl(),avatar);
					}
					map.put(dlink, avatar);
				}
			}
		} else if (page.isHtmlContent()) {
			// html格式：默认只认为在list_detail模式下的resource.selector选择出来的资源
			// 是avatar与detail_link共同外面的css选择器或正则表达式，这样才能一一映射，
    	    // 缺点是：在List_Detail模式下配置fetch.resource.selector时，程序只认为它是与detail link数量一一对应的资源，而非其它资源
			for (String select : selectors) {// 最多只有一条记录
				if(StringUtil.isNullOrBlank(select)){
					continue;
				}
				Document doc = Jsoup.parse(page.getHtmlContent(), page.getUrl());
				Elements ahref = doc.select(select);
				String imgSelector = "img[src]";
				for (Element ele : ahref) {
					List<Node> nodes = ele.childNodes();
					Document d = Jsoup.parseBodyFragment(nodes.toString() , page.getUrl());
					// detailLink不管是不是绝对地址都可以把它当作key值用，以备以后与avatar映射用
					Elements detailLink = d.select(detailSelect);
					// 假设每个detail link只对应一个img图像，如果是多个img就不好处理了（默认取第一个），地址需要转换成绝对地址.....
					Elements avatarLink = d.select(imgSelector);
					String dlink = detailLink.attr("href");
					if (StringUtil.isNullOrBlank(dlink) || dlink.startsWith("#") || dlink.equalsIgnoreCase("null") 
							|| dlink.contains("javascript:") || dlink.contains("mailto:") || dlink.contains("about:blank")){
						dlink = detailLink.next().attr("href");
					}			
					if(dlink.startsWith(".")){
						dlink = dlink.replace(".", "");
					}
					dlink = getAbsoluteURL(page.getUrl(), dlink);
					String avatar = avatarLink.attr("src");
					//当然第一个img如果是空，就获取第二个
					if (StringUtil.isNullOrBlank(avatar) || avatar.startsWith("#") || avatar.equalsIgnoreCase("null")){
						avatar = avatarLink.next().attr("src");
					}
					if(!StringUtil.isNullOrBlank(avatar)){
						avatar =  getAbsoluteURL(page.getUrl(),avatar);
					}
					map.put(dlink, avatar);
				}
			}
		}
		return map;
	}
	
	/**
	 * 嗅探Json文件中所有的url链接，嗅探方法不会像Jsoup那样遍历src和href固定标签，<br>
	 * 而是遍历所有json节点找出所有的url，并粗略地判断有后缀名的是资源文件，没有的就认为不是<br>
	 * 有两种情况嗅探不出来：<br>
	 * 1.当url为相对路径时，程序判断不出是不是url。这种情况会完全忽略。<br>
	 * 2.当资源为动态url并且没有后缀名时，程序不会判断出这种情况到底是不是资源文件，只能根据后缀名判断<br>
	 * 针对第二种情况也可以通过httpclient的response的content-type来精确判断，但是这么做性能极其低下，<br>
	 * 此时可以根据程序逻辑再次循环到此url会调用httpclient，不过当Fetch.mode为single时，<br>
	 * 就不会再次循环了，也就是说这种url就会忽略了<br>
	 * @param urls  当isResource=true时，此urls存放的是资源的url，否则为普通链接url
	 * @param jsonContent json内容的字符串
	 * @param isResource  是否是资源文件
	 */
	private HashSet<String> sniffUrlFromJson(String jsonContent, boolean isResource){
		HashSet<String> urls = new HashSet<String>();
		JSONObject jsonObj = JSONObject.parseObject(page.getJsonContent());
		HashSet<String> sets = new HashSet<String>(); 
		sets = lookupUrlFromJson(jsonObj,sets);
		for(String url : sets){
			if(FetchResourceSelector.isFindResources(url) == isResource){
				urls.add(url);
			}
		}
		return urls;
	}

	/**
	 * 递归遍历Json文件查找出所有http或者https开头的url
	 * @param jsonObj
	 * @return
	 */
	private HashSet<String> lookupUrlFromJson(JSONObject jsonObj, HashSet<String> url) {
		if(jsonObj == null){
			return url;
		}
		for (Entry<String, Object> entry : jsonObj.entrySet()) {
			Object obj = entry.getValue();
			if(obj == null){
				continue;
			}
			if(obj instanceof JSONObject){
				lookupUrlFromJson((JSONObject)obj, url);
			} else if(obj instanceof JSONArray){
				Iterator<?> it = ((JSONArray) obj).iterator();
				while(it.hasNext()){
					JSONObject jo = (JSONObject)it.next();
					lookupUrlFromJson((JSONObject)jo, url);
				}
			} else if(obj instanceof String && isStartHttpUrl(obj.toString())){
				url.add(obj.toString());
			}
		}
		return url;
	}

	/**
	 * 获取某个元素包含的所有url<br>
	 * 注意：有的链接是以data开头的小资源文件不支持抓取（即：二进制数据转换成为Base64的资源文件，直接在页面上引用）
	 * 例如文字格式：data:text/plain;charset=UTF-8;base64,5L2g5aW977yM5Lit5paH77yB 
	 * 图片格式：data:image/gif;base64,R0lGODlhAQAcALMAAMXh96HR97XZ98
	 * 
	 * @param element
	 * @return
	 */
	public final HashSet<String> getAllUrlByElement(Elements element) {
		HashSet<String> urls = new HashSet<String>();
		for (Element link : element) {
			//链接只考虑href 与 src 两种
			String source = link.toString().contains("href") ? "href" : "src";
			
			// 绝对路径：之前由于Jsoup调用的是parse(content, baseUri)方法，所以这个值绝对不为空
			String absLink = link.absUrl(source);
			String url = page.getUrl()+"#";
			if(StringUtil.isNullOrBlank(absLink) || url.equals(absLink) || url.startsWith("#") || url.equalsIgnoreCase("null") 
					|| url.contains("javascript:") || url.contains("mailto:") || url.contains("about:blank")) {
				continue;
			}

			urls.add(absLink);
		}
		return urls;
	}
	

	/**
	 * 相对路径转换为绝对路径 <br/>
	 * 
	 * @param baseUrl ：http://baoliao.cq.qq.com/pc/detail.html?id=294064 <br/>
	 * @param relativeUrl : index.html 或者是 /pc/index.html <br/>
	 * @return : http://baoliao.cq.qq.com/pc/index.html
	 */
	public final String getAbsoluteURL(String baseUrl, String relativeUrl) {
		String path = null;
		try {
			relativeUrl = URLDecoder.decode(relativeUrl, "UTF-8").split("\r\n")[0];
			URI base = new URI(baseUrl.trim());// 基本网页URI
			URI abs = base.resolve(relativeUrl.replace(" ", ""));
			URL absURL = abs.toURL();// 转成URL
			path = absURL.toString();
		} catch (Exception ex) {
			logger.warn("转换相对路径时，发现了非法的url格式：[" + relativeUrl + "]。");
			UrlQueue.newFailVisitedUrl(page.getSeedName(), relativeUrl);
		}
		return path;
	}

	/**
	 * url格式的简单判断，不太严谨<br/>
	 * 字符串是否为http或者https协议格式开头的Url格式 <br/>
	 * 可以过滤掉ftp/telnet/mailto/
	 * 
	 * @param url
	 * @return
	 */
	public final static boolean isStartHttpUrl(String url) {
		if(url.startsWith("http")){
			return true;
		}
		return false;
	}

	public static void main(String... args) throws Exception {
		
		String str = UrlAnalyzer.custom(new Page("","")).toString();
				

		System.out.println(str+" --- ");
	}

}
