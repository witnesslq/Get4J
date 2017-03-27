package com.bytegriffin.get4j.net.http;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.CodingErrorAction;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.DefaultHttpResponseParser;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.util.UrlQueue;

public class HttpClientEngine implements HttpEngine {

	private static final Logger logger = LogManager.getLogger(HttpClientEngine.class);
	/** 连接超时时间，单位毫秒**/
	private final static int conn_timeout = 30000;
	/** 获取数据的超时时间，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。 **/
	private final static int soket_timeout = 30000;
	/** 连接池中最大连接数 **/
	private final static int pool_total_conn = 100;
	/** 连接池中每个线程最大连接数 **/
	private final static int per_route_conn = 10;
	/** 最大重试次数 **/
	private final static int max_retry_count = 3;

	// private static HttpAsyncClientBuilder httpAsyncClientBuilder = null;

	@Override
	public void init(Seed seed) {
		//1.初始化HttpClientBuilder
		initHttpAsyncClientBuilder(seed.getSeedName());

		//2.初始化Http Proxy
		LinkedList<HttpProxy> httpProxys = seed.getFetchHttpProxy();
		if (httpProxys != null && httpProxys.size() > 0) {
			HttpProxyLooper hplooper = new HttpProxyLooper();
			hplooper.setQueue(httpProxys);
			Constants.HTTP_PROXY_LOOPER_CACHE.put(seed.getSeedName(), hplooper);
		}

		//3.初始化Http UserAgent
		LinkedList<String> userAgents = seed.getFetchUserAgent();
		if (userAgents != null && userAgents.size() > 0) {
			UserAgentLooper ualooper = new UserAgentLooper();
			ualooper.setQueue(userAgents);
			Constants.USER_AGENT_LOOPER_CACHE.put(seed.getSeedName(), ualooper);
		}

		//4.设置HttpClient请求的间隔时间
		if(seed.getFetchSleepTimeout() != null){
			Constants.FETCH_SLEEP_TIMEOUT_CACHE.put(seed.getSeedName(), seed.getFetchSleepTimeout());
		}
		logger.info("Seed[" + seed.getSeedName() + "]的Http引擎HttpClientEngine的初始化完成。");
	}
	
	/**
	 * 设计Http请求间隔时间
	 * @param seedName
	 */
	public static void sleepTimeout(String seedName){
		Long millis = Constants.FETCH_SLEEP_TIMEOUT_CACHE.get(seedName);
		if(millis == null){
			return;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.error("HttpClient请求时间间隔时出错：",e);
		}
	}

	/**
	 * 重试机制
	 */
	private static HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
		public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
			if (executionCount >= max_retry_count) {
				// Do not retry if over max retry count
				return false;
			}
			if (exception instanceof InterruptedIOException) {
				// Timeout
				return false;
			}
			if (exception instanceof UnknownHostException) {
				// Unknown host
				return false;
			}
			if (exception instanceof ConnectTimeoutException) {
				// Connection refused
				return false;
			}
			if (exception instanceof SSLException) {
				// SSL handshake exception
				return false;
			}
			HttpClientContext clientContext = HttpClientContext.adapt(context);
			HttpRequest request = clientContext.getRequest();
			boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
			if (idempotent) {
				// Retry if the request is considered idempotent
				return true;
			}
			return false;
		}

	};

	/**
	 * 绕过验证
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	private static SSLContext createIgnoreVerifySSL() {
		SSLContext ctx = null;

		// 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
		X509TrustManager trustManager = new X509TrustManager() {
			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		try {
			ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] { trustManager }, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ctx;
	}

	/**
	 * 检查Http Proxy代理是否可运行
	 * 
	 * @return
	 */
	public boolean testHttpProxy(String url, HttpProxy httpProxy) {
		CloseableHttpClient httpclient = null;
		HttpClientBuilder builder = HttpClients.custom();
		if (httpProxy.getCredsProvider() != null) {
			builder.setDefaultCredentialsProvider(httpProxy.getCredsProvider());
		}
		httpclient = builder.build();
		try {
			RequestConfig config = RequestConfig.custom().setProxy(httpProxy.getHttpHost()).setConnectTimeout(3000).build();
			HttpGet httpget = new HttpGet(url);
			httpget.setConfig(config);
			HttpResponse response = httpclient.execute(httpget);
			if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode()){
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
			close(httpclient);
		}
	}

	/**
	 * 初始化HttpAsyncClientBuilder
	 * 
	 * @return
	 */
	public static void initHttpAsyncClientBuilder(String siteName) {
		// Use custom message parser / writer to customize the way HTTP
		// messages are parsed from and written out to the data stream.
		HttpMessageParserFactory<HttpResponse> responseParserFactory = new DefaultHttpResponseParserFactory() {

			@Override
			public HttpMessageParser<HttpResponse> create(SessionInputBuffer buffer, MessageConstraints constraints) {
				LineParser lineParser = new BasicLineParser() {

					@Override
					public Header parseHeader(final CharArrayBuffer buffer) {
						try {
							return super.parseHeader(buffer);
						} catch (ParseException ex) {
							return new BasicHeader(buffer.toString(), null);
						}
					}

				};
				return new DefaultHttpResponseParser(buffer, lineParser, DefaultHttpResponseFactory.INSTANCE,
						constraints) {

					@Override
					protected boolean reject(final CharArrayBuffer line, int count) {
						// try to ignore all garbage preceding a status line
						// infinitely
						return false;
					}

				};
			}

		};

		HttpMessageWriterFactory<HttpRequest> requestWriterFactory = new DefaultHttpRequestWriterFactory();

		// Use a custom connection factory to customize the process of
		// initialization of outgoing HTTP connections. Beside standard
		// connection
		// configuration parameters HTTP connection factory can define message
		// parser / writer routines to be employed by individual connections.
		HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new ManagedHttpClientConnectionFactory(
				requestWriterFactory, responseParserFactory);

		// Client HTTP connection objects when fully initialized can be bound to
		// an arbitrary network socket. The process of network socket
		// initialization,
		// its connection to a remote address and binding to a local one is
		// controlled
		// by a connection socket factory.

		// SSL context for secure connections can be created either based on
		// system or application specific properties.
		// SSLContext sslcontext = SSLContexts.createSystemDefault();
		SSLContext sslcontext = createIgnoreVerifySSL();
		// Create a registry of custom connection socket factories for supported
		// protocol schemes.
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", new SSLConnectionSocketFactory(sslcontext)).build();

		// Use custom DNS resolver to override the system DNS resolution.
		// DnsResolver dnsResolver = new SystemDefaultDnsResolver() {
		//
		// @Override
		// public InetAddress[] resolve(final String host) {
		// try{
		// if (host.equalsIgnoreCase("myhost")) {
		// return new InetAddress[] { InetAddress.getByAddress(new byte[] {127,
		// 0, 0, 1}) };
		// }
		// return super.resolve(host);
		// }catch(UnknownHostException ex){
		// logger.info("未知的Host：["+host+"]");
		// }
		// return null;
		// }
		//
		// };

		DnsResolver dnsResolver = new SystemDefaultDnsResolver();
		// Create a connection manager with custom configuration.
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry,
				connFactory, dnsResolver);

		// Create socket configuration
		SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
		// Configure the connection manager to use socket configuration either
		// by default or for a specific host.
		connManager.setDefaultSocketConfig(socketConfig);
		connManager.setSocketConfig(new HttpHost("somehost", 80), socketConfig);
		// Validate connections after 1 sec of inactivity
		connManager.setValidateAfterInactivity(1000);

		// Create message constraints
		MessageConstraints messageConstraints = MessageConstraints.custom().setMaxHeaderCount(200)
				.setMaxLineLength(2000).build();
		// Create connection configuration
		ConnectionConfig connectionConfig = ConnectionConfig.custom().setMalformedInputAction(CodingErrorAction.IGNORE)
				.setUnmappableInputAction(CodingErrorAction.IGNORE).setCharset(Consts.UTF_8)
				.setMessageConstraints(messageConstraints).build();

		// Configure the connection manager to use connection configuration
		// either
		// by default or for a specific host.
		connManager.setDefaultConnectionConfig(connectionConfig);
		// connManager.setConnectionConfig(new HttpHost("somehost", 80),
		// ConnectionConfig.DEFAULT);

		// Configure total max or per route limits for persistent connections
		// that can be kept in the pool or leased by the connection manager.
		connManager.setMaxTotal(pool_total_conn);
		connManager.setDefaultMaxPerRoute(per_route_conn);
		// connManager.setMaxPerRoute(new HttpRoute(new HttpHost("somehost",
		// 80)), 20);

		// Use custom cookie store if necessary.
		// CookieStore cookieStore = new BasicCookieStore();
		// Use custom credentials provider if necessary.
		// CredentialsProvider credentialsProvider = new
		// BasicCredentialsProvider();
		// Create global request configuration
		RequestConfig defaultRequestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT)
				.setSocketTimeout(soket_timeout).setConnectTimeout(conn_timeout).setConnectionRequestTimeout(conn_timeout).setExpectContinueEnabled(true)
				.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
				.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).build();

		LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy();

		// Create an HttpClient with the given custom dependencies and
		// configuration.
		HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(connManager).setConnectionTimeToLive(1, TimeUnit.DAYS)
				.setRedirectStrategy(redirectStrategy).setConnectionManagerShared(true).setRetryHandler(retryHandler)
				.setDefaultRequestConfig(defaultRequestConfig);

		Constants.HTTP_CLIENT_BUILDER_CACHE.put(siteName, httpClientBuilder);
	}

	/**
	 * 关闭httpClient
	 */
	private static void close(CloseableHttpClient httpClient) {
		try {
			if (httpClient != null) {
				httpClient.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 格式化Url: www.website.com ===> http://www.website.com
	 * Site设置的Url必须带schema，否则报错
	 */
	public static String addUrlSchema(String url) {
		if (StringUtil.isNullOrBlank(url)) {
			return url;
		}
		if (!url.contains("http://") && !url.contains("https://")) {
			url = "http://" + url.trim();
		}
		return url;
	}

	/**
	 * 删除url的protocal http://www.website.com ===> www.website.com
	 */
	public static String deleteUrlSchema(String url) {
		if (StringUtil.isNullOrBlank(url)) {
			return url;
		}
		return url.replaceAll("http://", "").replaceAll("https://", "");
	}

	/**
	 * 设置请求中的Http代理
	 * 
	 * @param site
	 */
	private static void setHttpProxy(String seedName) {
		HttpProxyLooper hpl = Constants.HTTP_PROXY_LOOPER_CACHE.get(seedName);
		if (hpl == null) {
			return;
		}
		HttpProxy proxy = hpl.next();
		if (proxy.getHttpHost() != null) {
			Constants.HTTP_CLIENT_BUILDER_CACHE.get(seedName).setProxy(proxy.getHttpHost());
		}
		if (proxy.getCredsProvider() != null) {
			Constants.HTTP_CLIENT_BUILDER_CACHE.get(seedName).setDefaultCredentialsProvider(proxy.getCredsProvider());
		}
	}

	/**
	 * 设置User_Agent
	 */
	private static void setUserAgent(String seedName) {
		UserAgentLooper ual = Constants.USER_AGENT_LOOPER_CACHE.get(seedName);
		if (ual == null) {
			return;
		}
		String userAgent = ual.next();
		if(!StringUtil.isNullOrBlank(userAgent)){
			Constants.HTTP_CLIENT_BUILDER_CACHE.get(seedName).setUserAgent(ual.next());
		}
	}

	/**
	 * 判断HttpClient下载是否为Json文件
	 * 
	 * @param contentType
	 * @return
	 */
	public static boolean isDownloadJsonFile(String contentType) {
		if (contentType.contains("json") || contentType.contains("JSON") || contentType.contains("Json")) {
			return true;
		}
		return false;
	}

	/**
	 * 获取并设置page的页面内容（包含Html、Json）
	 * 注意：有的站点链接是Post操作，只需在浏览器中找到真实link，保证参数完整，Get也可以获取。
	 * @param page
	 * @return
	 */
	public Page SetContentAndCookies(Page page) {
		CloseableHttpClient httpClient = null;
		String url = page.getUrl();
		try {
			logger.info("线程["+Thread.currentThread().getName()+"]种子[" + page.getSeedName() + "]获取并设置page内容的请求url为：["+url+"]");
			sleepTimeout(page.getSeedName());
			setHttpProxy(page.getSeedName());
			setUserAgent(page.getSeedName());
			httpClient = Constants.HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
			HttpGet request = new HttpGet(url);
			HttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();			
			String content = EntityUtils.toString(entity, Consts.UTF_8);
			if (content.contains("刷新太频繁") || content.contains("刷新频繁") || content.contains("频繁访问") || content.contains("访问频繁")) {
				logger.warn("线程["+Thread.currentThread().getName()+"]种子[" + page.getSeedName() + "]访问[" + url + "]时太过频繁。");
			}
			String contentType = entity.getContentType().getValue();
			if (isDownloadJsonFile(contentType)) {
				page.setJsonContent(content);
			} else if (contentType.contains("text/html") || contentType.contains("text/plain")) {
				page.setHtmlContent(content);//注意：有时text/plain这种文本格式里面放的是json字符串，但是有种特殊情况是这个json字符串里也包含html
			} else { //不是html也不是json，那么只能是resource的链接了
				HashSet<String> resources = page.getResources();
				resources.add(url);
			}
			// 设置Response Cookie
			Header header = response.getLastHeader("Set-Cookie");
			if(header != null){ 
				page.setCookies(header.getValue());
			}
		} catch (Exception e) {
			UrlQueue.newUnVisitedLink(page.getSeedName(), url);
			logger.error("线程["+Thread.currentThread().getName()+"]种子[" + page.getSeedName() + "]获取链接[" + url + "]内容失败。", e);
		} finally {
			close(httpClient);
		}
		return page;
	}

	/**
	 * 下载网页中的资源文件（JS/CSS/JPG等）<br>
	 * 无需调用HttpUnit引擎，因为它已是被解析出来的资源<br>
	 * @param site
	 * @return
	 */
	public static void downloadResources(Page page) {
		// 判断是否有资源文件，
		if (page.getResources() == null || page.getResources().size() == 0) {
			return;
		}
		String folderName = Constants.DOWNLOAD_DIR_CACHE.get(page.getSeedName());
		HashSet<String> resources = page.getResources();
		sleepTimeout(page.getSeedName());
		setHttpProxy(page.getSeedName());
		setUserAgent(page.getSeedName());
		CloseableHttpClient httpClient = Constants.HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
		for (String url : resources) {
			try {
				HttpGet request = new HttpGet(url);
				HttpResponse response = httpClient.execute(request);
				HttpEntity entity = response.getEntity();
				byte[] content = EntityUtils.toByteArray(entity);
				String resourceName = folderName + File.separator;
				Header header = entity.getContentType();
				if(header == null){
					continue;
				}
				String contentType = header.getValue();
				String suffix = "";
				if (isDownloadJsonFile(contentType) || contentType.contains("text/html") || contentType.contains("text/plain")) {
					continue;//如果是页面就直接过滤掉
				} else if (contentType.contains("svg")) {
					suffix = "svg";
				} else if (contentType.contains("icon")) {
					suffix = "ico";
				} else if (contentType.contains("javascript")) {
					suffix = "js";
				} else if (contentType.contains("excel")) {
					suffix = "xls";
				} else if (contentType.contains("powerpoint")) {
					suffix = "ppt";
				} else if (contentType.contains("word")) {
					suffix = "doc";
				} else if (contentType.contains("flash")) {
					suffix = "swf";
				} else {// 此种情况为后缀名与ContentType保持一致的
					String[] array = contentType.split("/");
					if (array != null) {
						suffix = array[1];
					}
					if(suffix.contains(";")){
						suffix = suffix.substring(0, suffix.indexOf(";"));
					}
				}
				resourceName += FileUtil.generateResourceName(page.getSeedName(), url, suffix);
				FileUtil.writeFileToDisk(resourceName, content);
			} catch (Exception e) {
				logger.error("Seed[" + page.getSeedName() + "]连接[" + url + "]失败。", e);
			} finally {
				close(httpClient);
			}
		}
	}

	/**
	 * 下载avatar资源文件<br>
	 * 无需调用HttpUnit引擎，因为它已是被解析出来的资源<br>
	 * @param page
	 */
	public static void downloadAvatar(Page page) {
		String folderName = Constants.DOWNLOAD_DIR_CACHE.get(page.getSeedName());
		String url = page.getAvatar();
		sleepTimeout(page.getSeedName());
		setHttpProxy(page.getSeedName());
		setUserAgent(page.getSeedName());
		CloseableHttpClient httpClient = Constants.HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
		try {
			HttpGet request = new HttpGet(url);
			HttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			byte[] content = EntityUtils.toByteArray(entity);
			String resourceName = folderName + File.separator;
			Header header = entity.getContentType();
			if(header == null){
				return;
			}
			String contentType = header.getValue();
			String suffix = "";
			if (isDownloadJsonFile(contentType) || contentType.contains("text/html") || contentType.contains("text/plain")) {
				return;//如果是页面就直接过滤掉
			} else if (contentType.contains("svg")) {
				suffix = "svg";
			} else if (contentType.contains("icon")) {
				suffix = "ico";
			} else if (contentType.contains("javascript")) {
				suffix = "js";
			} else if (contentType.contains("excel")) {
				suffix = "xls";
			} else if (contentType.contains("powerpoint")) {
				suffix = "ppt";
			} else if (contentType.contains("word")) {
				suffix = "doc";
			} else if (contentType.contains("flash")) {
				suffix = "swf";
			} else {// 此种情况为后缀名与ContentType保持一致的
				String[] array = contentType.split("/");
				if (array != null) {
					suffix = array[1];
				}
				if(suffix.contains(";")){
					suffix = suffix.substring(0, suffix.indexOf(";"));
				}
			}
			resourceName += FileUtil.generateResourceName(page.getSeedName(), url, suffix);
			FileUtil.writeFileToDisk(resourceName, content);
			page.setAvatar(resourceName);
		} catch (Exception e) {
			logger.error("Seed[" + page.getSeedName() + "]连接[" + url + "]失败。", e);
		} finally {
			close(httpClient);
		}

	}
}
