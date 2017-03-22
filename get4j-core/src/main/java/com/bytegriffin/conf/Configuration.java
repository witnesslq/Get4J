package com.bytegriffin.conf;

/**
 * configuration.xml全局配置文件
 */
public class Configuration {

	public static final String Url_Download_File_Name = "url";
	public static final String Auto_Download_File_Name = "auto";

	/**
	 * 非必填项。下载的文件名前缀，有两种模式：url/auto，url表示某文件
	 * 的具体网络地址，default表示无前缀，默认值是auto，如果选择url的话，
	 * url中的特殊字符?|等会被下划线_替换，否则在操作系统中生成会报错。
	 * auto模式下如果抓取的是动态网站，HttpClient无法获取到首页名称的话，
	 * 那么它会按照index.html自动补全
	 */
	private String downloadFileNameRule = Auto_Download_File_Name;

	public String getDownloadFileNameRule() {
		return downloadFileNameRule;
	}

	public void setDownloadFileNameRule(String downloadFileNameRule) {
		this.downloadFileNameRule = downloadFileNameRule;
	}


}
