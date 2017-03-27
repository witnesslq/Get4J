package com.bytegriffin.get4j;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.bytegriffin.get4j.util.StringUtil;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.ImmediateRefreshHandler;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class TestHttpEngine {


	private static String url = "http://huaban.com/favorite/home/";

	public static void testunit() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		long start = System.currentTimeMillis();
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		@SuppressWarnings("resource")
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		webClient.getOptions().setUseInsecureSSL(true);// 支持https
		webClient.getOptions().setJavaScriptEnabled(true); // 启用JS解释器，默认为true
		webClient.getOptions().setCssEnabled(true); // 禁用css支持
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
		HtmlPage page = webClient.getPage(url);
		long end = System.currentTimeMillis();
		long aaa = end - start;

		System.err.println(aaa+" "+page.asXml());
	}

	/**
	 * 注意：Jsoup在选择多个class时，中间的空格用点替代
	 * 
	 * @param cotent
	 */
	public static void parse(String cotent) {
		Document doc = Jsoup.parse(cotent);
		Elements eles = doc.select("div.inv-title.pt5>a[href]");
		for (Element e : eles) {
			String link = e.attr("href");
			if (StringUtil.isNullOrBlank(link)) {
				continue;
			}
			System.err.println(link);
		}
	}

	public static void httpclient() throws ClientProtocolException, IOException {
		long start = System.currentTimeMillis();
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();// 标准Cookie策略

		CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		// 发送get请求
		HttpGet request = new HttpGet("https://www.tuandai.com/pages/ajax/newinvest_list.ashx?pageindex=1&pagesize=5&Cmd=GetInvest_List&type=6&status=1");
//		List <NameValuePair> params = new ArrayList<NameValuePair>();  
//        params.add(new BasicNameValuePair("pageindex", "1"));
//        params.add(new BasicNameValuePair("pagesize", "5"));
//        params.add(new BasicNameValuePair("RepaymentTypeId", "0"));
//        params.add(new BasicNameValuePair("type", "6"));
//        params.add(new BasicNameValuePair("status", "1"));
//        params.add(new BasicNameValuePair("orderby", "0"));
//        params.add(new BasicNameValuePair("beginDeadLine","0"));
//        params.add(new BasicNameValuePair("endDeadLine","0"));
//        params.add(new BasicNameValuePair("rate","0"));
//        params.add(new BasicNameValuePair("beginRate","0"));
//        params.add(new BasicNameValuePair("endRate","0"));
//        params.add(new BasicNameValuePair("orderby","0"));
//        params.add(new BasicNameValuePair("Cmd","GetInvest_List"));
//
//		request.setEntity(new UrlEncodedFormEntity(params));
		HttpResponse response = client.execute(request);
		long end = System.currentTimeMillis();
		long aaa = end - start;
		String content = EntityUtils.toString(response.getEntity(), Consts.UTF_8);
		parse(content);
		System.err.println(aaa+" "+content);
	}

	public static final String getAbsoluteURL(String baseUrl, String relativeUrl) {
		String path = null;
		try {
			relativeUrl = URLDecoder.decode(relativeUrl, "UTF-8").split("\r\n")[0];
			URI base = new URI(baseUrl.trim());// 基本网页URI
			URI abs = base.resolve(relativeUrl.trim());
			URL absURL = abs.toURL();// 转成URL
			path = absURL.toString();
		} catch (MalformedURLException | IllegalArgumentException | UnsupportedEncodingException ex) {
		} catch (URISyntaxException e) {
		}
		return path;
	}
	
	public static void main(String... args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		// testunit();
		 httpclient();
	}
}
