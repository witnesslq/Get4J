package com.bytegriffin.get4j.net.http;

import java.net.URL;
import java.util.List;

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
public class HtmlUnitEngine extends AbstractHttpEngine implements HttpEngine{

	private static final Logger logger = LogManager.getLogger(HtmlUnitEngine.class);

	public HtmlUnitEngine() {
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
		List<HttpProxy> httpProxys = seed.getFetchHttpProxy();
		if (httpProxys != null && httpProxys.size() > 0) {
			HttpProxySelector hplooper = new HttpProxySelector();
			hplooper.setQueue(httpProxys);
			Constants.HTTP_PROXY_CACHE.put(seed.getSeedName(), hplooper);
		}

		// 3.初始化Http UserAgent
		List<String> userAgents = seed.getFetchUserAgent();
		if (userAgents != null && userAgents.size() > 0) {
			UserAgentSelector ualooper = new UserAgentSelector();
			ualooper.setQueue(userAgents);
			Constants.USER_AGENT_CACHE.put(seed.getSeedName(), ualooper);
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
		HttpProxySelector hpl = Constants.HTTP_PROXY_CACHE.get(siteName);
		if (hpl == null) {
			return;
		}
		HttpProxy proxy = hpl.choice();
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
		UserAgentSelector ual = Constants.USER_AGENT_CACHE.get(seedName);
		if (ual == null) {
			return;
		}
		String userAgent = ual.choice();
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
	public Page getPageContent(Page page) {
		WebClient webClient = Constants.WEBCLIENT_CACHE.get(page.getSeedName());
		String url = page.getUrl();
		HttpClientEngine.sleepTimeout(page.getSeedName());
		try {
			WebRequest request = new WebRequest(new URL(url));
			setHttpProxy(page.getSeedName(), webClient, request);
			setUserAgent(page.getSeedName(), request);
			HtmlPage htmlpage = webClient.getPage(request);
			WebResponse response = htmlpage.getWebResponse();

			int statusCode = response.getStatusCode();
			boolean isvisit = isVisit(statusCode, page, logger);
			if(!isvisit){
				return page;
			}
			
			String content = response.getContentAsString();
			String contentType = response.getContentType();
			if(StringUtil.isNullOrBlank(content)){
				logger.warn("线程[" + Thread.currentThread().getName() + "]访问种子[" + page.getSeedName() + "]的url["+page.getUrl()+"]内容为空。");
				return page;
			}

			// 设置页面编码
			page.setCharset(getCharset(contentType, content));
			
			// 重新设置content编码
			content = getContentAsString(response.getContentAsStream(), page.getCharset());
			
			// 记录站点防止频繁抓取的页面链接
			frequentAccesslog(page.getSeedName(), url, content, logger);

			// 设置page内容
			setContent(contentType, content, page);

			//设置Response Cookie
			page.setCookies(response.getResponseHeaderValue("Set-Cookie"));

		} catch (Exception e) {
			UrlQueue.newUnVisitedLink(page.getSeedName(), url);
			logger.error("线程["+Thread.currentThread().getName()+"]种子[" + page.getSeedName() + "]获取链接[" + url + "]内容失败。", e);
		}
		
		return page;
	}

}
