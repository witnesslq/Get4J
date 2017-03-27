package com.bytegriffin.get4j.net.http;

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.util.UrlQueue;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.ImmediateRefreshHandler;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * 专门处理Javascript效果的html网页 <br>
 * 但是它每访问一个页面的速度大概是httpclient的4倍左右 <br>
 */
public class HttpUnitEngine implements HttpEngine {

	private static final Logger logger = LogManager.getLogger(HttpUnitEngine.class);

	public HttpUnitEngine() {
	}

	@Override
	public void init(Seed seed) {
		// 1.初始化WebClient
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		webClient.getOptions().setUseInsecureSSL(true);// 支持https
		webClient.getOptions().setJavaScriptEnabled(true); // 启用JS解释器，默认为true
		webClient.getOptions().setCssEnabled(false); // 禁用css支持
		webClient.getOptions().setThrowExceptionOnScriptError(false); // js运行错误时，是否抛出异常
		webClient.getOptions().setTimeout(10000); // 设置连接超时时间，这里是10S。如果为0，则无限期等待
		webClient.getOptions().setDoNotTrackEnabled(false);
		webClient.setJavaScriptTimeout(8000);// 设置js运行超时时间
		webClient.waitForBackgroundJavaScript(500);// 设置页面等待js响应时间
		webClient.getOptions().setRedirectEnabled(true);
		webClient.setRefreshHandler(new ImmediateRefreshHandler());
		webClient.getOptions().setThrowExceptionOnScriptError(false);   
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());  
//		webClient.getOptions().setPopupBlockerEnabled(true);
		Constants.WEBCLIENT_CACHE.put(seed.getSeedName(), webClient);

		// 2.初始化Http Proxy
		LinkedList<HttpProxy> httpProxys = seed.getFetchHttpProxy();
		if (httpProxys != null && httpProxys.size() > 0) {
			HttpProxyLooper hplooper = new HttpProxyLooper();
			hplooper.setQueue(httpProxys);
			Constants.HTTP_PROXY_LOOPER_CACHE.put(seed.getSeedName(), hplooper);
		}

		// 3.初始化Http UserAgent
		LinkedList<String> userAgents = seed.getFetchUserAgent();
		if (userAgents != null && userAgents.size() > 0) {
			UserAgentLooper ualooper = new UserAgentLooper();
			ualooper.setQueue(userAgents);
			Constants.USER_AGENT_LOOPER_CACHE.put(seed.getSeedName(), ualooper);
		}

		// 4.设置HttpClient请求的间隔时间
		if (seed.getFetchSleepTimeout() != null) {
			Constants.FETCH_SLEEP_TIMEOUT_CACHE.put(seed.getSeedName(), seed.getFetchSleepTimeout());
		}
		
		logger.info("Seed[" + seed.getSeedName() + "]的Http引擎HttpUnitEngine的初始化完成。");
	}

	@Override
	public boolean testHttpProxy(String url, HttpProxy httpProxy) {
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		webClient.getOptions().setUseInsecureSSL(true);// 支持https
		webClient.getOptions().setTimeout(10000); // 设置连接超时时间，这里是10S。如果为0，则无限期等

		ProxyConfig proxyConfig = webClient.getOptions().getProxyConfig();  
        proxyConfig.setProxyHost(httpProxy.getIp());  
        proxyConfig.setProxyPort(Integer.valueOf(httpProxy.getPort()));  
        DefaultCredentialsProvider credentialsProvider = (DefaultCredentialsProvider) webClient.getCredentialsProvider();  
        credentialsProvider.addCredentials(httpProxy.getUsername(), httpProxy.getPassword()); 
        webClient.setCredentialsProvider(credentialsProvider);

        try {
			HtmlPage htmlPage = webClient.getPage(url);
			int status = htmlPage.getWebResponse().getStatusCode();
			if(HttpStatus.SC_OK == status){
			    logger.info("Http代理[" + httpProxy.toString() + "]测试成功。");
			    return true;
			} else {
				logger.info("Http代理[" + httpProxy.toString() + "]测试失败。");
				return false;
			}
		} catch (Exception e) {
			logger.error("Http代理[" + httpProxy.toString() + "]测试出错，请重新检查。");
			return false;
		} finally {
			webClient.close();
		}
	}

	/**
	 * 设置请求中的Http代理
	 * 
	 * @param site
	 */
	private static void setHttpProxy(String siteName, WebClient webClient, WebRequest request) {
		HttpProxyLooper hpl = Constants.HTTP_PROXY_LOOPER_CACHE.get(siteName);
		if (hpl == null) {
			return;
		}
		HttpProxy proxy = hpl.next();
		if (proxy.getHttpHost() != null) {
			request.setProxyHost(proxy.getIp());
			request.setProxyPort(Integer.valueOf(proxy.getPort()));
		}
		if (proxy.getCredsProvider() != null) {
			 DefaultCredentialsProvider credentialsProvider = new DefaultCredentialsProvider();   
			 credentialsProvider.addCredentials(proxy.getUsername(), proxy.getPassword());  
			 webClient.setCredentialsProvider(credentialsProvider);
		}
	}

	/**
	 * 设置User_Agent
	 */
	private static void setUserAgent(String seedName,  WebRequest request) {
		UserAgentLooper ual = Constants.USER_AGENT_LOOPER_CACHE.get(seedName);
		if (ual == null) {
			return;
		}
		String userAgent = ual.next();
		if(!StringUtil.isNullOrBlank(userAgent)){
			request.setAdditionalHeader("User-Agent",userAgent);
		}
	}

	/**
	 * 获取url的内容，与HttpClientProbe的getAndSetContent方法实现完全一致，
	 * 只是调用了HtmlUnit的API而已。
	 * @param page
	 * @return
	 */
	public Page SetContentAndCookies(Page page) {
		WebClient webClient = Constants.WEBCLIENT_CACHE.get(page.getSeedName());
		String url = page.getUrl();
		HttpClientEngine.sleepTimeout(page.getSeedName());
		try {
			logger.info("线程["+Thread.currentThread().getName()+"]种子[" + page.getSeedName() + "]获取并设置page内容的请求url为：["+url+"]");
			WebRequest request = new WebRequest(new URL(url));
			setHttpProxy(page.getSeedName(), webClient, request);
			setUserAgent(page.getSeedName(), request);
			HtmlPage htmlpage = webClient.getPage(request);
			WebResponse response = htmlpage.getWebResponse();
			String content = response.getContentAsString();
			if (content.contains("刷新太频繁") || content.contains("刷新频繁") || content.contains("频繁访问")) {
				logger.warn("线程["+Thread.currentThread().getName()+"]种子[" + page.getSeedName() + "]访问[" + url + "]时太过频繁。");
			}
			String contentType = response.getContentType();
			if (HttpClientEngine.isDownloadJsonFile(contentType)) {
				page.setJsonContent(content);
			} else if (contentType.contains("text/html") || contentType.contains("text/plain")) {
				page.setHtmlContent(content);
				//设置Response Cookie
				page.setCookies(response.getResponseHeaderValue("Set-Cookie"));
			} else { //不是html也不是json，那么只能是resource的链接了
				HashSet<String> resources = page.getResources();
				resources.add(url);
			}
		} catch (Exception e) {
			UrlQueue.newUnVisitedLink(page.getSeedName(), url);
			logger.error("线程["+Thread.currentThread().getName()+"]种子[" + page.getSeedName() + "]获取链接[" + url + "]内容失败。", e);
		} 
		
		return page;
	}

}
