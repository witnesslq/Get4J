package com.bytegriffin.get4j.fetch;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.jayway.jsonpath.JsonPath;

/**
 *  资源精确选择器 比如过滤图片、js文件等等。。。
 */
public class FetchResourceSelector{

	/**
	 * 默认值是all，当内容html时系统会抓取所有的资源文件
	 */
	public static final String ALL_RESOURCE_FILTER = "all";
	
	/**
	 * 系统不会抓取任何资源文件
	 */
	private static final String NONE_RESOURCE_FILTER = "none";

	/**
	 * 资源文件后缀名
	 * 注意某些页面上的xml文件也看作是资源文件
	 * 例如：<link type="application/rss+xml" href="rss"/> <link type="application/wlwmanifest+xml" href="wlwmanifest.xml"/>
	 */
	public static final Pattern BINARY_FILTERS = Pattern
            .compile(".*(\\.(css|js|bmp|gif|jpe?g|png|ico|tiff?|svg|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf"
                    + "|rm|rmvb|smil|wmv|swf|wma|7z|tar|zip|rar|gz|tgz|exe|asf|iso|3gp|mkv|flac|ape|dll|OCX|rtf|jar"
                    + "|psd|lzh|pdf|dat|apk|ipa|epub|mobi|deb|sisx|cab|pxl|csv|doc|xls|ppt|pptx|msi|chm|torrent|docx"
                    + "|pptx|xlsx|xv|xvx|tp|tl|hqx|mdf|arj|txt|xml))$");
	
	private static final FetchResourceSelector fetchResourceSelector =  new FetchResourceSelector();;

	public static FetchResourceSelector me(){
		return fetchResourceSelector;
	}

	/**
	 * 配置的resource选择字符串集合
	 */
	private List<String> selectors;
	
	public List<String> getSelectors() {
		return selectors;
	}

	public void setSelectors(List<String> selectors) {
		this.selectors = selectors;
	}

	/**
	 * 初始化资源选择器缓存
	 * @param seed
	 */
	public static void init(Seed seed){
		List<String> otherSelectors = seed.getFetchResourceSelectors();
		if(otherSelectors != null && otherSelectors.size() > 0){
			FetchResourceSelector ff = new FetchResourceSelector();
			ff.setSelectors(otherSelectors);
			Constants.FETCH_RESOURCE_SELECTOR_CACHE.put(seed.getSeedName(), ff);
		}
	}

	/**
	 * 是否配置了抓取所有的资源文件，默认值不填也是all
	 * @return
	 */
	public boolean isConfigAll(){
		if(this.selectors == null || (this.selectors.size() == 1 
				&& this.selectors.get(0).equalsIgnoreCase(ALL_RESOURCE_FILTER)) ){
			return true;
		}
		return false;
	}

	/**
	 * 是否屏蔽（过滤）了所有的资源文件
	 * @return
	 */
	public boolean isConfigNone(){
		if(this.selectors != null && this.selectors.size() == 1 
				&& this.selectors.get(0).equalsIgnoreCase(NONE_RESOURCE_FILTER)){
			return true;
		}
		return false;
	}

	/**
	 * 过滤某url是否是资源文件
	 * 如果发现就是true，否则是false
	 * @param url
	 * @return
	 */
	public static boolean isFindResources(String url){
		Matcher m = BINARY_FILTERS.matcher(url);
		boolean isfind = m.find();
		return isfind;
	}

	/**
	 * 暂时没用
	 * 过滤fetcher抓取的过滤器，如果发现了过滤的后缀名就返回true
	 * @param url  相对url或者绝对url都行
	 * @return
	 */
	@Deprecated
    public boolean isFindSuffix(String url, String fetchFilter){
    	int xiegang = url.lastIndexOf("/");
		int wenhao = url.indexOf("?");
		if(wenhao == -1){
			url = url.substring(xiegang + 1, url.length());
		} else {
			url = url.substring(xiegang + 1, wenhao);
		}
    	Pattern pattern = Pattern.compile(fetchFilter);
    	Matcher m = pattern.matcher(url);
		boolean isfind = m.find();
		return isfind;
    }

    /**
     * 暂时没用(JSoup支持正则查找)
     * 使用正则表达式过滤url
     * @param urls 原始输入的一堆url
     * @param regex  正则表达式（过滤规则）
     * @return  返回过滤后剩下的url
     */
	@Deprecated
    public static HashSet<String> regex(HashSet<String> urls,String regex){
    	Pattern filter = Pattern.compile(regex);
    	Matcher m = null;
    	HashSet<String> newurls = new HashSet<String>();
    	newurls.addAll(urls);
    	for(String url : urls){
    		m = filter.matcher(url);
    		boolean isfind = m.find();
    		if(isfind){
    			newurls.remove(url);
    		}
    	}
		return newurls;
    }

    /**
     * 使用Jsoup自带的css选择器找出html页面中的url资源，支持模糊匹配、正则匹配 <br>
     * [attr^=value]开头匹配value, [attr$=value]结尾匹配value, [attr*=value]包含属性值value，例如：[href*=/path/]
     * [attr~=regex]正则匹配，例如：img[src~=(?i)\.(png|jpe?g)]
     * @param html
     * @param cssQuery
     * @return
     */
    public HashSet<String> cssSelect(Page page,String cssQuery){
    	String eleAt = cssQuery.contains("href") ? "href" : "src";
    	Document doc = Jsoup.parse(page.getHtmlContent());
    	Elements eles = doc.select(cssQuery);
    	HashSet<String> newurls = UrlAnalyzer.custom(page).getAllUrlByElement(eleAt, page.getUrl(), eles);
    	return newurls;
    }

    /**
     * 使用Jsonpath找出json文件中的url资源 <br>
     * @param jsonContent json内容
     * @param jsonPath 过滤符，例如：$.data[*].avatar
     * @param urlPrefix url前缀字符串
     * @return
     */
    @SuppressWarnings("unchecked")
    public static HashSet<String> jsonPath(String jsonContent,String jsonPath, String urlPrefix){
    	if(urlPrefix == null){
    		urlPrefix = "";
    	}
    	HashSet<String> links = new HashSet<String>();
    	Object obj = JsonPath.read(jsonContent, jsonPath);
    	if(obj instanceof List){
    		List<String> list = (List<String>)obj;
			for(String link : list){
				links.add(urlPrefix + link);
			}
		} else if (obj instanceof String){
			links.add(urlPrefix + String.valueOf(obj));
		}
    	return links;
    }
    
    /**
     * 使用Jsonpath找出json文件中的url资源 <br>
     * @param jsonContent json内容
     * @param jsonPath 过滤符，例如：$.data[*].avatar
     * @param urlPrefix url前缀字符串
     * @return
     */
    public static List<String> jsonPath2List(String jsonContent,String jsonPath, String urlPrefix){
    	if(urlPrefix == null){
    		urlPrefix = "";
    	}
    	List<String> links = new ArrayList<String>();
    	Object obj = JsonPath.read(jsonContent, jsonPath);
    	if(obj instanceof List){
    		@SuppressWarnings("unchecked")
			List<String> list = (List<String>)obj;
			for(String link : list){
				links.add(urlPrefix + link);
			}
		} else if (obj instanceof String){
			links.add(urlPrefix + String.valueOf(obj));
		}
    	return links;
    }

    public static void main(String... args) throws IOException {	

	}

}
