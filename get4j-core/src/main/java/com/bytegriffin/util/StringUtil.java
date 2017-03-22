package com.bytegriffin.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

	
	/**
	 * 判断字符串是否为空 
	 * 是:true 否:false
	 * @param str
	 * @return
	 */
	public static boolean isNullOrBlank(String str) {
		if (null == str || "".equals(str.trim())) {
			return true;
		}
		return false;
	}
	
	/**
	 * 判断字符串是否包含中文
	 * @param strname
	 * @return
	 */
	public static boolean isContainChinese(String strname) {
		Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		Matcher m = p.matcher(strname);
		if (m.find()) {
			return true;
		}
		return false;
	}


}
