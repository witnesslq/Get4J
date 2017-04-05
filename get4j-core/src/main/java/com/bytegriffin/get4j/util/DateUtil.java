package com.bytegriffin.get4j.util;

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

    /**
     * 时间开销
     *
     * @param startTime String
     * @return String
     */
    public static String getCostDate(String startTime) {
        String endTime = getCurrentDate();
        SimpleDateFormat dfs = new SimpleDateFormat(yyyyMMddHHmmss);
        long between = 0;
        try {
            Date begin = dfs.parse(startTime);
            Date end = dfs.parse(endTime);
            between = (end.getTime() - begin.getTime());// 得到两者的毫秒数
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        long day = between / (24 * 60 * 60 * 1000);
        long hour = (between / (60 * 60 * 1000) - day * 24);
        long min = ((between / (60 * 1000)) - day * 24 * 60 - hour * 60);
        long s = (between / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
        long ms = (between - day * 24 * 60 * 60 * 1000 - hour * 60 * 60 * 1000 - min * 60 * 1000 - s * 1000);
        StringBuilder sb = new StringBuilder();
        if (day > 0) {
            sb.append(day);
            sb.append("天");
        }
        if (hour > 0) {
            sb.append(hour);
            sb.append("小时");
        }
        if (min > 0) {
            sb.append(min);
            sb.append("分钟");
        }
        if (s > 0) {
            sb.append(s);
            sb.append("秒");
        }
        if (ms > 0) {
            sb.append(ms);
            sb.append("毫秒");
        }
        return sb.toString();
    }

}
