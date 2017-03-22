package com.bytegriffin.fetch;

/**
 * 抓取模式 ：list_detail页面格式、single单个页面、cascade级联、site整站
 */
public enum FetchMode {

	list_detail("list_detail"), single("single"), cascade("cascade"), site("site");

	private final String value;

	private FetchMode(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}