package com.bytegriffin.get4j.net.http;

import java.net.URL;
import java.util.HashSet;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.core.UrlQueue;
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
public class HtmlUnitEngine extends AbstractHttpEngine implements HttpEngine {

    private static final Logger logger = LogManager.getLogger(HtmlUnitEngine.class);

    public HtmlUnitEngine() {
    }

    @Override
    public void init(Seed seed) {
        // 1.初始化WebClient
    	DefaultConfig.closeHttpClientLog();
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
        Globals.WEBCLIENT_CACHE.put(seed.getSeedName(), webClient);

        // 2.初始化参数
        initParams(seed, logger);

        logger.info("Seed[" + seed.getSeedName() + "]的Http引擎HttpUnitEngine的初始化完成。");
    }

    /**
     * 检查Http Proxy代理是否可运行
     *
     * @return boolean
     */
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
            if (HttpStatus.SC_OK == status) {
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
     * @param seedName  seedName
     * @param webClient webClient
     * @param request   request
     */
    private static void setHttpProxy(String seedName, WebClient webClient, WebRequest request) {
        HttpProxySelector hpl = Globals.HTTP_PROXY_CACHE.get(seedName);
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
    private static void setUserAgent(String seedName, WebRequest request) {
        UserAgentSelector ual = Globals.USER_AGENT_CACHE.get(seedName);
        if (ual == null) {
            return;
        }
        String userAgent = ual.choice();
        if (!StringUtil.isNullOrBlank(userAgent)) {
            request.setAdditionalHeader("User-Agent", userAgent);
        }
    }

    /**
     * 获取url的内容，与HttpClientProbe的getAndSetContent方法实现完全一致，
     * 只是调用了HtmlUnit的API而已。
     *
     * @param page page
     * @return Page
     */
    public Page getPageContent(Page page) {
        WebClient webClient = Globals.WEBCLIENT_CACHE.get(page.getSeedName());
        String url = page.getUrl();
        sleep(page.getSeedName(), logger);
        try {
            // 生成site url
            setHost(page);
            WebRequest request = new WebRequest(new URL(url));
            setHttpProxy(page.getSeedName(), webClient, request);
            setUserAgent(page.getSeedName(), request);
            request.setAdditionalHeader("Host", page.getHost());
            request.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

            com.gargoylesoftware.htmlunit.Page htmlpage = webClient.getPage(request);
            WebResponse response = htmlpage.getWebResponse();

            int statusCode = response.getStatusCode();
            boolean isvisit = isVisit(statusCode, page, logger);
            if (!isvisit) {
                HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManagerShared(true);
                Globals.HTTP_CLIENT_BUILDER_CACHE.put(page.getSeedName(), httpClientBuilder);
                return page;
            }

            long contentlength = response.getContentLength();
            if (contentlength > big_file_max_size) {//大于10m
                HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManagerShared(true);
                Globals.HTTP_CLIENT_BUILDER_CACHE.put(page.getSeedName(), httpClientBuilder);
                boolean isdone = HttpClientEngine.downloadBigFile(page, contentlength);
                if (isdone) {
                    return page;
                }
            }


            String content = response.getContentAsString();
            String contentType = response.getContentType();
            if (StringUtil.isNullOrBlank(content)) {
                logger.warn("线程[" + Thread.currentThread().getName() + "]访问种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]内容为空。");
                return page;
            }

            // 如果是资源文件的话
            if (!isJsonPage(contentType) && !isHtmlPage(contentType) && !isXmlPage(contentType, content)) {
                HashSet<String> resources = page.getResources();
                resources.add(page.getUrl());
                HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManagerShared(true);
                Globals.HTTP_CLIENT_BUILDER_CACHE.put(page.getSeedName(), httpClientBuilder);
                return page;
            }

            // 设置页面编码
            page.setCharset(getCharset(contentType, content));

            // 重新设置content编码
            content = getContentAsString(response.getContentAsStream(), page.getCharset());

            // 重新设置url编码
            // page.setUrl(decodeUrl(page.getUrl(), page.getCharset()));

            // 记录站点防止频繁抓取的页面链接
            frequentAccesslog(page.getSeedName(), url, content, logger);

            // 设置page内容
            setContent(contentType, content, page);

            //设置Response Cookie
            page.setCookies(response.getResponseHeaderValue("Set-Cookie"));

        } catch (Exception e) {
            UrlQueue.newUnVisitedLink(page.getSeedName(), url);
            logger.error("线程[" + Thread.currentThread().getName() + "]种子[" + page.getSeedName() + "]获取链接[" + url + "]内容失败。", e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
        }

        return page;
    }

    @Override
    public String probePageContent(Page page) {
        WebClient webClient = Globals.WEBCLIENT_CACHE.get(page.getSeedName());
        try {
            WebRequest request = new WebRequest(new URL(page.getUrl()));
            setHttpProxy(page.getSeedName(), webClient, request);
            setUserAgent(page.getSeedName(), request);
            request.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

            com.gargoylesoftware.htmlunit.Page htmlpage = webClient.getPage(request);
            WebResponse response = htmlpage.getWebResponse();

            int statusCode = response.getStatusCode();
            boolean isvisit = isVisit(statusCode, page, logger);
            if (!isvisit) {
                return null;
            }

            long contentlength = response.getContentLength();
            if (contentlength > big_file_max_size) {//大于10m
                logger.warn("线程[" + Thread.currentThread().getName() + "]探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]页面内容太大。");
            }

            String content = response.getContentAsString();
            String contentType = response.getContentType();
            if (StringUtil.isNullOrBlank(content)) {
                logger.warn("线程[" + Thread.currentThread().getName() + "]探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]页面内容为空。");
            }

            // 重新设置content编码
            content = getContentAsString(response.getContentAsStream(), getCharset(contentType, content));
            return content;
        } catch (Exception e) {
            logger.error("线程[" + Thread.currentThread().getName() + "]探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]内容失败。", e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
        }
        return null;
    }

}
