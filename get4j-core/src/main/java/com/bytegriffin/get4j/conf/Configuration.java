package com.bytegriffin.get4j.conf;

/**
 * configuration.xml全局配置文件
 */
public class Configuration {

    /**
     *  非必填项。下载的文件名命名规则，有两种模式：url/default，url表示文件名带有具体网络地址，默认值default表示无url前缀，
     *  如果选择url的话，url中的特殊字符?|等会被下划线_替换，否则在操作系统中生成文件名会报错。default模式下如果抓取的是动态网站，
	 *  HttpClient无法获取到首页名称的话，那么它会按照index.html自动补全
     */
    private String downloadFileNameRule;

    /**
     * 非必填项。指定邮件接收人，当系统出现异常，邮件接收人会受到由get4j@sina.com发送的通知邮件，收件人可以为多个，
     * 多个收件人地址之间可用分号;隔开表示抄送。如果不配置此项或者默认值default为空表示不会发送。
     */
    private String emailRecipient;

    public String getDownloadFileNameRule() {
        return downloadFileNameRule;
    }

    public void setDownloadFileNameRule(String downloadFileNameRule) {
        this.downloadFileNameRule = downloadFileNameRule;
    }

	public String getEmailRecipient() {
		return emailRecipient;
	}

	public void setEmailRecipient(String emailRecipient) {
		this.emailRecipient = emailRecipient;
	}

}
