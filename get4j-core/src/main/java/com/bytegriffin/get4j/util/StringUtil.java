package com.bytegriffin.get4j.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtil {

    /**
     * 是否是数字
     *
     * @param str String
     * @return boolean
     */
    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }


    /**
     * 判断字符串是否为空
     * 是:true 否:false
     *
     * @param str String
     * @return boolean
     */
    public static boolean isNullOrBlank(String str) {
        return (null == str || "".equals(str.trim()));
    }

    /**
     * 判断字符串是否包含中文
     *
     * @param strname String
     * @return boolean
     */
    public static boolean isContainChinese(String strname) {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(strname);
        return m.find();
    }


}
