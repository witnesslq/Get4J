package com.bytegriffin.get4j.net.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.CodingErrorAction;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
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
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.FileUtil;

import com.google.common.base.Strings;
import com.bytegriffin.get4j.core.UrlQueue;

public class HttpClientEngine extends AbstractHttpEngine implements HttpEngine {

    private static final Logger logger = LogManager.getLogger(HttpClientEngine.class);
    /**
     * 连接超时时间，单位毫秒
     **/
    private final static int conn_timeout = 30000;
    /**
     * 获取数据的超时时间，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
     **/
    private final static int soket_timeout = 30000;
    /**
     * 连接池中socket最大连接数上限
     **/
    private final static int pool_total_conn = 400;
    /**
     * 连接池中每个线程最大连接数
     **/
    private final static int per_route_conn = 20;
    /**
     * 最大重试次数
     **/
    private final static int max_retry_count = 5;
    /**
     * 链接管理器，提取成属性是主要是用于关闭闲置链接
     **/
    private static PoolingHttpClientConnectionManager connManager;

    @Override
    public void init(Seed seed) {
        // 1.初始化HttpClientBuilder
        initHttpClientBuilder(seed.getSeedName());

        // 2.初始化配置参数
        initParams(seed, logger);

        logger.info("Seed[" + seed.getSeedName() + "]的Http引擎HttpClientEngine的初始化完成。");
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
            if (exception instanceof SSLException) {
                // SSL handshake exception
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            return !(request instanceof HttpEntityEnclosingRequest);
        }

    };

    /**
     * 绕过验证
     *
     * @return SSLContext
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
            ctx.init(null, new TrustManager[]{trustManager}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ctx;
    }

    /**
     * 检查Http Proxy代理是否可运行
     *
     * @return boolean
     */
    public boolean testHttpProxy(String url, HttpProxy httpProxy) {
        CloseableHttpClient httpclient;
        HttpClientBuilder builder = HttpClients.custom();
        if (httpProxy.getCredsProvider() != null) {
            builder.setDefaultCredentialsProvider(httpProxy.getCredsProvider());
        }
        httpclient = builder.build();
        HttpGet httpget = null;
        try {
            RequestConfig config = RequestConfig.custom().setProxy(httpProxy.getHttpHost()).setConnectTimeout(3000)
                    .build();
            httpget = new HttpGet(url);
            httpget.setConfig(config);
            HttpResponse response = httpclient.execute(httpget);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
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
            if (httpget != null) {
                httpget.releaseConnection();
            }
        }
    }

    /**
     * 负责使用连接管理器清空失效连接和过长连接
     */
    public static void closeIdleConnection() {
        if (connManager != null) {
            // 关闭失效连接
            connManager.closeExpiredConnections();
            // 关闭空闲超过30秒的连接
            connManager.closeIdleConnections(30, TimeUnit.SECONDS);
        }
    }

    /**
     * 初始化HttpAsyncClientBuilder
     */
    private static void initHttpClientBuilder(String seedName) {
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
        connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory, dnsResolver);

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
                .setRelativeRedirectsAllowed(true).setSocketTimeout(soket_timeout).setConnectTimeout(conn_timeout)
                .setCircularRedirectsAllowed(true).setConnectionRequestTimeout(conn_timeout)
                .setExpectContinueEnabled(true)
                //HttpClient是默认开启跳转功能的，但是遇到一些链接会报错：http://tousu.baidu.com/news/add
                .setRedirectsEnabled(false)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).build();

        // 处理301：永久重定向 302、303、307
        LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy();

        ConnectionKeepAliveStrategy keepAliveStrategy = new DefaultConnectionKeepAliveStrategy() {
            /**
             * 服务器端配置（以tomcat为例）：keepAliveTimeout=60000，表示在60s内内，服务器会一直保持连接状态。
             * 也就是说，如果客户端一直请求服务器，且间隔未超过60s，则该连接将一直保持，如果60s内未请求，则超时。
             *
             * getKeepAliveDuration返回超时时间；
             */
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {

                // 如果服务器指定了超时时间，则以服务器的超时时间为准
                HeaderElementIterator it = new BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        try {// 服务器指定时间
                            return Long.parseLong(value) * 1000;
                        } catch (NumberFormatException ignore) {
                            ignore.printStackTrace();
                        }
                    }
                }

                long keepAlive = super.getKeepAliveDuration(response, context);

                // 如果服务器未指定超时时间，则客户端默认30s超时
                if (keepAlive == -1) {
                    keepAlive = 30 * 1000;
                }
                return keepAlive;
            }
        };

        // gzip请求
        HttpRequestInterceptor gzipRequestInterceptor = new HttpRequestInterceptor() {
            public void process(final HttpRequest request, final HttpContext context)
                    throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip");
                }
            }
        };

        // gzip解析
        HttpResponseInterceptor gzipResponseInterceptor = new HttpResponseInterceptor() {
            public void process(final HttpResponse response, final HttpContext context)
                    throws HttpException, IOException {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return;
                }
                Header ceheader = entity.getContentEncoding();
                if (ceheader == null) {
                    return;
                }
                HeaderElement[] codecs = ceheader.getElements();
                for (HeaderElement he : codecs) {
                    if (he.getName().equalsIgnoreCase("gzip")) {
                        GzipDecompressingEntity gentity = new GzipDecompressingEntity(response.getEntity());
                        response.setEntity(gentity);
                        break;
                    }
                }
            }
        };

        // Create an HttpClient with the given custom dependencies and
        // configuration.
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(connManager)
                .addInterceptorFirst(gzipRequestInterceptor).addInterceptorLast(gzipResponseInterceptor)
                .setConnectionTimeToLive(1, TimeUnit.DAYS).setRedirectStrategy(redirectStrategy)
                .setConnectionManagerShared(true).setRetryHandler(retryHandler).setKeepAliveStrategy(keepAliveStrategy)
                .setDefaultRequestConfig(defaultRequestConfig);

        Globals.HTTP_CLIENT_BUILDER_CACHE.put(seedName, httpClientBuilder);
    }

    /**
     * 设置请求中的Http代理
     *
     * @param seedName seedName
     */
    private static void setHttpProxy(String seedName) {
        HttpProxySelector hpl = Globals.HTTP_PROXY_CACHE.get(seedName);
        if (hpl == null) {
            return;
        }
        HttpProxy proxy = hpl.choice();
        if (proxy.getHttpHost() != null) {
            Globals.HTTP_CLIENT_BUILDER_CACHE.get(seedName).setProxy(proxy.getHttpHost());
        }
        if (proxy.getCredsProvider() != null) {
            Globals.HTTP_CLIENT_BUILDER_CACHE.get(seedName).setDefaultCredentialsProvider(proxy.getCredsProvider());
        }
    }

    /**
     * 设置User_Agent
     */
    private static void setUserAgent(String seedName) {
        UserAgentSelector ual = Globals.USER_AGENT_CACHE.get(seedName);
        if (ual == null) {
            return;
        }
        String userAgent = ual.choice();
        if (!Strings.isNullOrEmpty(userAgent)) {
            Globals.HTTP_CLIENT_BUILDER_CACHE.get(seedName).setUserAgent(userAgent);
        }
    }

    /**
     * 获取并设置page的页面内容（包含Html、Json）
     * 注意：
     * 1.有的站点链接是Post操作，只需在浏览器中找到真实link，保证参数完整，Get也可以获取。
     * 2.有些网站会检查header中的Referer是否合法
     *
     * @param page page
     * @return Page
     */
    public Page getPageContent(Page page) {
        CloseableHttpClient httpClient;
        String url = page.getUrl();
        HttpGet request = null;
        try {
            sleep(page.getSeedName(), logger);
            setHttpProxy(page.getSeedName());
            setUserAgent(page.getSeedName());
            // 生成site url
            setHost(page, logger);
            httpClient = Globals.HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
            request = new HttpGet(url);
            request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            request.addHeader("Host", page.getHost());
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            boolean isvisit = isVisit(statusCode, page, logger);
            if (!isvisit) {
                return page;
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                logger.warn("线程[" + Thread.currentThread().getName() + "]访问种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]内容为空。");
            }
            // 设置Response Cookie
            Header header = response.getLastHeader("Set-Cookie");
            if (header != null) {
                page.setCookies(header.getValue());
            }
            Header ctHeader = null;
            try {
                ctHeader = entity.getContentType();
            } catch (NullPointerException e) {
                UrlQueue.newFailVisitedUrl(page.getSeedName(), url);
                logger.error("线程[" + Thread.currentThread().getName() + "]获取种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]页面内容为空。", e);
                return page;
            }
            if (ctHeader != null) {
                long contentlength = entity.getContentLength();
                boolean isdone = downloadBigFile(page, contentlength);
                if (isdone) {
                    return page;
                }
                String contentType = ctHeader.getValue();

                // 转换内容字节
                byte[] bytes = EntityUtils.toByteArray(entity);
                String content = new String(bytes);

                if (Strings.isNullOrEmpty(content)) {
                    UrlQueue.newFailVisitedUrl(page.getSeedName(), url);
                    logger.warn("线程[" + Thread.currentThread().getName() + "]访问种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]内容为空。");
                    return page;
                }

                // 如果是资源文件的话
                if (!isJsonPage(contentType) && !isHtmlPage(contentType) && !isXmlPage(contentType, content)) {
                    HashSet<String> resources = page.getResources();
                    resources.add(page.getUrl());
                    return page;
                }

                // 设置页面编码
                page.setCharset(getCharset(contentType, content));

                // 重新设置content编码
                content = new String(bytes, page.getCharset());

                // 重新设置url编码
                //	page.setUrl(decodeUrl(page.getUrl(), page.getCharset()));

                // 记录站点防止频繁抓取的页面链接
                frequentAccesslog(page.getSeedName(), url, content, logger);

                // 设置page内容
                setContent(contentType, content, page);
            }

        } catch (Exception e) {
            UrlQueue.newUnVisitedLink(page.getSeedName(), url);
            logger.error("线程[" + Thread.currentThread().getName() + "]抓取种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]内容失败。", e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
            if (request != null) {
                request.abort();
            }
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
        return page;
    }

    /**
     * 下载大文件，默认设置超过10M大小的文件算是大文件
     * 文件太大会抛异常，所以特此添加一个下载打文件的方法
     *
     * @param page          page
     * @param contentlength contentlength
     * @return boolean
     */
    static boolean downloadBigFile(Page page, long contentlength) {
        if (contentlength <= big_file_max_size) {//10M
            return false;
        }
        String start = DateUtil.getCurrentDate();
        String fileName = FileUtil.generateResourceName(page.getUrl(), "");
        fileName = Globals.DOWNLOAD_DIR_CACHE.get(page.getSeedName()) + fileName;
        FileUtil.makeDiskFile(fileName, contentlength);
        CloseableHttpClient httpClient = null;
        String url = page.getUrl();
        HttpGet request = null;
        try {
            setHttpProxy(page.getSeedName());
            setUserAgent(page.getSeedName());
            httpClient = Globals.HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
            request = new HttpGet(url);
            //request.addHeader("Range", "bytes=" + offset + "-" + (this.offset + this.length - 1));//断点续传的话需要设置Range属性，当然前提是服务器支持
            HttpResponse response = httpClient.execute(request);
            FileUtil.writeFile(fileName, contentlength, response.getEntity().getContent());
            String log = DateUtil.getCostDate(start);
            logger.info("线程[" + Thread.currentThread().getName() + "]下载大小为[" + contentlength / (1024 * 1024) + "]MB的文件[" + fileName + "]总共花费时间为[" + log + "]。");
        } catch (Exception e) {
            logger.error("线程[" + Thread.currentThread().getName() + "]下载种子[" + page.getSeedName() + "]的大文件[" + page.getUrl() + "]时失败。", e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
            if (request != null) {
                request.abort();
            }
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
        return true;
    }

    /**
     * 禁用redirect，为了能获取301、302跳转后的真实地址
     * 是下载资源文件时使用。
     *
     * @param seedName seedName
     */
    private static void setRedirectFalse(String seedName) {
        RequestConfig config = RequestConfig.custom().setRedirectsEnabled(false).build();//不允许重定向
        Globals.HTTP_CLIENT_BUILDER_CACHE.get(seedName).setDefaultRequestConfig(config);
    }

    /**
     * 下载网页中的资源文件（JS/CSS/JPG等）<br>
     * 不管HttpEngine是不是HttpClient，都默认使用它下载资源文件，因为当HttpEngine是HtmlUnit时下载速度极其慢。<br>
     *
     * @param page
     */
    public static void downloadResources(Page page) {
        // 判断是否有资源文件，
        if (page.getResources() == null || page.getResources().size() == 0) {
            return;
        }
        HashSet<String> resources = page.getResources();
        sleep(page.getSeedName(), logger);
        setHttpProxy(page.getSeedName());
        setUserAgent(page.getSeedName());
        setRedirectFalse(page.getSeedName());
        CloseableHttpClient httpClient = Globals.HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
        for (String url : resources) {
            HttpGet request = null;

            try {
                request = new HttpGet(url);
                //request.setHeader("http.protocol.handle-redirects","false");
                HttpResponse response = httpClient.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();

                boolean isvisit = isVisit(statusCode, page, logger);
                if (!isvisit) {
                    continue;
                }
                // 301/302/303/307 获取真实地址
                if (HttpStatus.SC_MOVED_PERMANENTLY == statusCode || HttpStatus.SC_MOVED_TEMPORARILY == statusCode
                        || HttpStatus.SC_SEE_OTHER == statusCode || HttpStatus.SC_TEMPORARY_REDIRECT == statusCode) {
                    Header responseHeader = response.getFirstHeader("Location");
                    if (responseHeader != null && !Strings.isNullOrEmpty(responseHeader.getValue())) {
                        request.releaseConnection();
                        url = responseHeader.getValue();
                        response = httpClient.execute(new HttpGet(url));
                        logger.warn("线程[" + Thread.currentThread().getName() + "]访问种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]时跳转到新的url[" + url + "]上。");
                    }
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    EntityUtils.consume(entity);
                    logger.warn("线程[" + Thread.currentThread().getName() + "]下载种子[" + page.getSeedName() + "]的url["
                            + page.getUrl() + "]资源内容为空。");
                    return;
                }

                long contentlength = entity.getContentLength();
                boolean isdone = downloadBigFile(page, contentlength);
                if (isdone) {
                    return;
                }

                byte[] content = EntityUtils.toByteArray(entity);
                String resourceName = Globals.DOWNLOAD_DIR_CACHE.get(page.getSeedName());
                Header header = entity.getContentType();
                if (header == null) {
                    continue;
                }
                String contentType = header.getValue();
                String suffix = "";
                if (Strings.isNullOrEmpty(contentType)) {
                    resourceName += FileUtil.generateResourceName(url, suffix);
                    FileUtil.writeFileToDisk(resourceName, content);
                    continue;
                } else {
                    if (isJsonPage(contentType) || isHtmlPage(contentType) || isXmlPage(contentType, new String(content))) {
                        continue;// 如果是页面就直接过滤掉
                    }
                    if (contentType.contains("svg")) {
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
                    } else { // 此种情况为后缀名与ContentType保持一致的
                        if (contentType.contains("/")) {
                            String[] array = contentType.split("/");
                            if (array != null && array.length > 1 && array[1].contains(";")) {
                                suffix = array[1].substring(0, array[1].indexOf(";"));
                            } else {
                                suffix = array[0];
                            }
                        }
                    }
                }
                resourceName += FileUtil.generateResourceName(url, suffix);
                FileUtil.writeFileToDisk(resourceName, content);
            } catch (Exception e) {
                UrlQueue.newUnVisitedResource(page.getSeedName(), url);
                logger.error("线程[" + Thread.currentThread().getName() + "]下载种子[" + page.getSeedName() + "]的url[" + url
                        + "]资源失败。", e);
                EmailSender.sendMail(e);
                ExceptionCatcher.addException(page.getSeedName(), e);
                if (request != null) {
                    request.abort();
                }
            } finally {
                if (request != null) {
                    request.releaseConnection();
                }
            }
        }
    }

    /**
     * 下载avatar资源文件<br>
     * 不管HttpEngine是不是HttpClient，都默认使用它下载资源文件，因为当HttpEngine是HtmlUnit时下载速度极其慢。<br>
     *
     * @param page
     */
    public static void downloadAvatar(Page page) {
        String url = page.getAvatar();
        sleep(page.getSeedName(), logger);
        setHttpProxy(page.getSeedName());
        setUserAgent(page.getSeedName());
        setRedirectFalse(page.getSeedName());
        CloseableHttpClient httpClient = Globals.HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
        HttpGet request = null;
        try {
            request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            boolean isvisit = isVisit(statusCode, page, logger);
            if (!isvisit) {
                return;
            }
            // 301/302/303/307 获取真实地址
            if (HttpStatus.SC_MOVED_PERMANENTLY == statusCode || HttpStatus.SC_MOVED_TEMPORARILY == statusCode
                    || HttpStatus.SC_SEE_OTHER == statusCode || HttpStatus.SC_TEMPORARY_REDIRECT == statusCode) {
                Header responseHeader = response.getFirstHeader("Location");
                if (responseHeader != null) {
                    if (!Strings.isNullOrEmpty(responseHeader.getValue())) {
                        request.releaseConnection();
                        url = responseHeader.getValue();
                        request = new HttpGet(url);
                        response = httpClient.execute(request);
                        logger.warn("线程[" + Thread.currentThread().getName() + "]访问种子[" + page.getSeedName() + "]的url["
                                + page.getUrl() + "]时跳转到新的url[" + url + "]上。");
                    }
                }
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                EntityUtils.consume(entity);
                logger.warn("线程[" + Thread.currentThread().getName() + "]下载种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]资源内容为空。");
                return;
            }
            long contentlength = entity.getContentLength();
            boolean isdone = downloadBigFile(page, contentlength);
            if (isdone) {
                return;
            }
            byte[] content = EntityUtils.toByteArray(entity);
            Header header = entity.getContentType();
            if (header == null) {
                return;
            }
            String contentType = header.getValue();
            String suffix = "";
            String resourceName = Globals.DOWNLOAD_DIR_CACHE.get(page.getSeedName());
            if (Strings.isNullOrEmpty(contentType)) {
                resourceName += FileUtil.generateResourceName(url, suffix);
                FileUtil.writeFileToDisk(resourceName, content);
                return;
            } else {
                if (isJsonPage(contentType) || isHtmlPage(contentType) || isXmlPage(contentType, new String(content))) {
                    return;// 如果是页面就直接过滤掉
                }
                if (contentType.contains("svg")) {
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
                    if (contentType.contains("/")) {
                        String[] array = contentType.split("/");
                        if (array.length > 1 && array[1].contains(";")) {
                            suffix = array[1].substring(0, array[1].indexOf(";"));
                        } else {
                            suffix = array[0];
                        }
                    }
                }
            }

            resourceName += FileUtil.generateResourceName(url, suffix);
            FileUtil.writeFileToDisk(resourceName, content);
            page.setAvatar(resourceName);
        } catch (Exception e) {
            UrlQueue.newUnVisitedResource(page.getSeedName(), url);
            logger.error("线程[" + Thread.currentThread().getName() + "]下载种子[" + page.getSeedName() + "]的url[" + url
                    + "]资源失败。", e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
            if (request != null) {
                request.abort();
            }
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public String probePageContent(Page page) {
        CloseableHttpClient httpClient;
        HttpGet request = null;
        try {
            setHttpProxy(page.getSeedName());
            setUserAgent(page.getSeedName());
            httpClient = Globals.HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
            request = new HttpGet(page.getUrl());
            request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            boolean isvisit = isVisit(statusCode, page, logger);
            if (!isvisit) {
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                logger.warn("线程[" + Thread.currentThread().getName() + "]探测种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]内容为空。");
            }
            Header ctHeader = null;
            try {
                ctHeader = entity.getContentType();
            } catch (NullPointerException e) {
                logger.error("线程[" + Thread.currentThread().getName() + "]探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]页面内容为空。", e);
                return null;
            }
            if (ctHeader != null) {
                long contentlength = entity.getContentLength();
                if (contentlength >= big_file_max_size) {
                    logger.warn("线程[" + Thread.currentThread().getName() + "]探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]页面内容太大。");
                }
                String contentType = ctHeader.getValue();

                // 转换内容字节
                byte[] bytes = EntityUtils.toByteArray(entity);
                String content = new String(bytes);

                if (Strings.isNullOrEmpty(content)) {
                    logger.warn("线程[" + Thread.currentThread().getName() + "]探测种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]内容为空。");
                    return null;
                }

                // 重新设置content编码
                content = new String(bytes, getCharset(contentType, content));

                // 设置page内容
                setContent(contentType, content, page);

                // 设置page内容
                return content;
            }

        } catch (Exception e) {
            logger.error("线程[" + Thread.currentThread().getName() + "]探测种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]内容失败。", e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
            if (request != null) {
                request.abort();
            }
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }

        return null;
    }

}
