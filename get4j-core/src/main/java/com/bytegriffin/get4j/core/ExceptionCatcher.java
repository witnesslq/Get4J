package com.bytegriffin.get4j.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异常捕捉
 */
public final class ExceptionCatcher {

    /**
     * 与种子抓取相关的异常信息
     * 当 key：seed_name value：exception information
     */
    private static Map<String, List<String>> seed_exception_info = new HashMap<>();

    /**
     * 其他类型的异常信息
     */
    private static List<String> exception_infos = new ArrayList<>();

    public static List<String> getAllExceptions(){
    	return exception_infos;
    }

    public static List<String> getExceptions(String seedName){
    	return seed_exception_info.get(seedName);
    }

	/**
	 * 获取完整的堆栈信息
	 * @param t
	 * @return
	 */
	public static String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();  
        PrintWriter pw = new PrintWriter(sw, true);  
        t.printStackTrace(pw);  
        return sw.getBuffer().toString();  
    }

	/**
	 * 增加异常信息 调用入口一
	 * @param seedName
	 * @param t
	 */
    public static void addException(String seedName, Throwable t){
    	String exception = getStackTrace(t);
    	addException(seedName, exception);
    }

    /**
     * 增加异常信息 调用入口二
     * @param seedName
     * @param exception
     */
    public static void addException(String seedName, String exception){
    	List<String> list = seed_exception_info.get(seedName);
        if (list == null || list.size() == 0) {
        	list = new ArrayList<>();
        	seed_exception_info.put(seedName, list);
        }
        if(!list.contains(exception)){
        	list.add(exception);
        }
    }

    /**
     * 增加异常信息 调用入口三
     * @param t
     */
    public static void addException(Throwable t){
    	String exception = getStackTrace(t);
    	addException(exception);
    }

    /**
     * 增加异常信息 调用入口四
     * @param exception
     */
    public static void addException(String exception){
    	if(!exception_infos.contains(exception)){
    		exception_infos.add(exception);
    	}
    }
}
