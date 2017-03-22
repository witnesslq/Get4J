package com.bytegriffin.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class DateUtil {

	public static final String yyyyMMdd = "yyyy-MM-dd";
	public static final String yyyyMMddHHmmss = "yyyy-MM-dd HH:mm:ss";

	public static Date strToDate(String str) {
		return strToDate(str, yyyyMMddHHmmss);
	}

	public static Date strToDate(String str, String pattern) {
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		try {
			return format.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * 当前时间
	 */
	public static String getCurrentDate() {
		Date date = Calendar.getInstance().getTime();
		SimpleDateFormat format = new SimpleDateFormat(yyyyMMddHHmmss);
		return format.format(date);
	}

	public static String dateToStr(Date date) {
		SimpleDateFormat format = new SimpleDateFormat(yyyyMMddHHmmss);
		return format.format(date);
	}

}
