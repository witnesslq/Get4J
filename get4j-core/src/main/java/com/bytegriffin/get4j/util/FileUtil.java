package com.bytegriffin.get4j.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.fetch.FetchResourceSelector;
import com.bytegriffin.get4j.net.http.HttpProxy;

/**
 * 文件工具类
 */
public final class FileUtil {

	private static Logger logger = LogManager.getLogger(FileUtil.class);

	/**
	 * 读取User Agent文件到内存中
	 * 
	 * @return
	 */
	public static List<String> readUserAgentFile(String userAgentFile) {
		List<String> list = readFileLine(userAgentFile);
		return list;
	}

	/**
	 * 转换配置文件路径，将classpath:/conf/user_agent转换成/opt/work/xxx/conf/user_agent
	 * 
	 * @param classpath
	 * @return
	 */
	public static String getAbsolutePath(String classpath) {
		String newpath = "";
		if (classpath.contains("classpath:")) {
			newpath = System.getProperty("user.dir") + File.separator;
			classpath = classpath.replace("classpath:", "");
		} else if (classpath.contains("classpath：")) {
			newpath = System.getProperty("user.dir") + File.separator;
			classpath = classpath.replace("classpath：", "");
		} else {// 此时为绝对路径，不用转换
			return classpath;
		}
		if (classpath.substring(0, 1).equals("/")) {
			classpath = classpath.replaceFirst("/", "");
		}
		classpath = classpath.replace("/", File.separator);
		return newpath + classpath;
	}

	/**
	 * 读取http代理文件转换为HttpProxy对象到内存中 http_proxy文件的格式是ip:port@username:password
	 * 
	 * @return
	 */
	public static List<HttpProxy> readHttpProxyFile(String httpProxyFile) {
		List<String> list = readFileLine(httpProxyFile);
		List<HttpProxy> newList = new ArrayList<HttpProxy>();
		for (String str : list) {
			HttpProxy hp = null;
			if (StringUtil.isNullOrBlank(str)) {
				continue;
			} else if (str.contains("@")) {
				String[] array = str.split("@");
				String[] front = array[0].split(":");
				String[] end = array[1].split(":");
				hp = new HttpProxy(front[0], front[1], end[0], end[1]);
			} else if (str.contains(":")) {
				String[] front = str.split(":");
				hp = new HttpProxy(front[0], front[1]);
			} else {
				hp = new HttpProxy(str);
			}
			newList.add(hp);
		}
		return newList;
	}

	/**
	 * 将Proxy字符串解析成代理对象
	 * 
	 * @param proxyString
	 * @return
	 */
	public static HttpProxy formatProxy(String proxyString) {
		HttpProxy hp = null;
		if (StringUtil.isNullOrBlank(proxyString)) {
			return hp;
		} else if (proxyString.contains("@")) {
			String[] array = proxyString.split("@");
			String[] front = array[0].split(":");
			String[] end = array[1].split(":");
			hp = new HttpProxy(front[0], front[1], end[0], end[1]);
		} else if (proxyString.contains(":")) {
			String[] front = proxyString.split(":");
			hp = new HttpProxy(front[0], front[1]);
		} else {
			hp = new HttpProxy(proxyString);
		}
		return hp;
	}

	/**
	 * 读取文件的每行数据将其放回到一个ArrayList中
	 * 
	 * @param configFile
	 * @return
	 */
	public static List<String> readFileLine(String configFile) {
		configFile = getAbsolutePath(configFile);
		boolean flag = FileUtil.isExists(configFile);
		if (!flag) {
			return null;
		}
		boolean fc = FileUtil.isExistContont(configFile);
		if (!fc) {
			return null;
		}
		List<String> result = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(configFile));// 构造一个BufferedReader类来读取文件
			String s = null;
			while ((s = br.readLine()) != null) {// 使用readLine方法，一次读一行
				if (StringUtil.isNullOrBlank(s.trim())) {
					continue;
				}
				if (s.contains("#")) {// #代表单行注释，所以要去掉
					continue;
				}
				result.add(s);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 判断文件是否为空，是否有内容
	 * 
	 * @param filename
	 * @return
	 */
	public static boolean isExistContont(String filename) {
		File file = new File(filename);
		if (file != null && file.length() == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 判断文件是否存在
	 * 
	 * @param filename
	 * @return
	 */
	public static boolean isExists(String filename) {
		File file = new File(filename);
		return file.exists();
	}

	/**
	 * 生成dump文件夹以及dump下的文件
	 * 
	 * @param folder
	 * @param filename
	 */
	public static File makeDumpDir(String folder, String filename) {
		File file = new File(folder);
		if (!file.exists()) {
			file.mkdirs();
		}
		File dumpfile = new File(folder + filename);
		if (!dumpfile.exists()) {
			try {
				dumpfile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return dumpfile;
	}

	/**
	 * 追加内容
	 * 
	 * @param file
	 * @param content
	 */
	public static void append(File file, Collection<String> contents) {
		if (contents != null && !contents.isEmpty()) {
			FileWriter fw = null;
			try {
				fw = new FileWriter(file, true);
				Iterator<String> iter = contents.iterator();
				while (iter.hasNext()) {
					String str = (String) iter.next();
					if (StringUtil.isNullOrBlank(str)) {
						break;
					}
					fw.write(str + System.getProperty("line.separator"));
				}
				fw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (fw != null) {
						fw.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 删除文件中的某行内容
	 * 
	 * @param file
	 * @param proxy
	 */
	public static void removeLine(String file, String proxy) {
		try {
			file = FileUtil.getAbsolutePath(file);
			File inFile = new File(file);
			File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

			BufferedReader br = new BufferedReader(new FileReader(file));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
			String line = null;

			while ((line = br.readLine()) != null) {
				if (line.trim().equals("")) {
					continue;
				} else if (!line.trim().equals(proxy)) {
					pw.println(line);
					pw.flush();
				}
			}
			pw.close();
			br.close();
			if (!inFile.delete()) {
				System.out.println("不能删除原文件。");
				return;
			}
			if (!tempFile.renameTo(inFile)) {
				System.out.println("不能重命名新文件。");
			}
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 在磁盘上创建下载文件夹
	 * 
	 * @param dir
	 */
	public static String makeDownloadDir(String diskDir) {
		diskDir = getAbsolutePath(diskDir);
		String laststr = diskDir.substring(diskDir.length() - 1, diskDir.length());
		if (laststr.equals(File.separator)) {
			diskDir = diskDir.substring(0, diskDir.length() - 1);
		}
		// diskDir += File.separator + seed.getSeedName();
		File homedir = new File(diskDir);
		if (!homedir.exists()) {
			try {
				homedir.mkdirs();
			} catch (Exception ex) {
				logger.warn("不能创建文件夹[" + diskDir + "]，它可能包含一些特殊字符串。", ex);
			}
		}
		return diskDir;
	}

	/**
	 * 在本地磁盘生成页面
	 * 
	 * @param page
	 */
	public static void downloadPagesToDisk(Page page) {
		String folderName = Constants.DOWNLOAD_DIR_CACHE.get(page.getSeedName());
		String fileName = folderName + File.separator;
		if (page.isJsonContent()) {
			fileName += generatePageName(page.getSeedName(), page.getUrl(), Constants.JSON_PAGE_SUFFIX);
		} else if (page.isHtmlContent()) {
			fileName += generatePageName(page.getSeedName(), page.getUrl(), Constants.DEFAULT_PAGE_SUFFIX);
		} else if (page.isXmlContent()) {
			fileName += generatePageName(page.getSeedName(), page.getUrl(), Constants.XML_PAGE_SUFFIX);
		} else {// 这种情况为资源文件，直接返回
			return;
		}
		byte[] content = null;
		try {
			if (page.isHtmlContent()) {
				content = page.getHtmlContent().getBytes(page.getCharset());
			} else if (page.isJsonContent()) {
				content = page.getJsonContent().getBytes(page.getCharset());
			} else if (page.isXmlContent()) {
				content = page.getXmlContent().getBytes(page.getCharset());
			}
		} catch (UnsupportedEncodingException e) {
			logger.error("Seed[" + page.getSeedName() + "]通过线程[" + Thread.currentThread().getName() + "]往硬盘上写入名为["
					+ fileName + "]时出错。", e);
		}
		FileUtil.writeFileToDisk(fileName, content);
	}

	/**
	 * 往硬盘上写文件
	 * 
	 * @param fileName
	 * @param content
	 */
	public static void writeFileToDisk(String fileName, byte[] content) {
		if (StringUtil.isNullOrBlank(fileName) || content == null) {
			return;
		}
		File file = new File(fileName);
		if (file.exists() && content.length == file.length()) {
			// logger.warn("线程[" + Thread.currentThread().getName() +
			// "]在往硬盘上写入名为["+fileName+"]时发现硬盘上已存在此文件。");
			return;
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileName);
			fos.write(content);
			fos.flush();
		} catch (Exception e) {
			logger.error("线程[" + Thread.currentThread().getName() + "]往硬盘上写入页面时出错。", e);
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				logger.warn("硬盘上写入页面时出错。", e);
			}
		}
	}

	/**
	 * 删除url的protocal http://www.website.com ===> www.website.com
	 */
	public static String deleteUrlSchema(String url) {
		if (StringUtil.isNullOrBlank(url)) {
			return url;
		}
		return url.replaceAll("http://", "").replaceAll("https://", "");
	}

	/**
	 * 下载的网页文件名规则 <br />
	 * 此规则是在生成文件名时将url进行编码，然后去除其中的一些操作系统 <br>
	 * 不支持的特殊字符：<>,/,\,|,:,"",*,? 否则生成的文件名会出错 <br>
	 * 默认生成的文件名是带url的，如果不想带可以将其过滤掉会更简洁，但是<br>
	 * 人为发现不了某个资源（js或css等）文件是属于哪个（php或jsp）页面的
	 * 
	 * @param seedName
	 * @param url
	 * @param suffix
	 *            文件后缀名
	 * @return
	 */
	public static String generatePageName(String seedName, String url, String suffix) {
		String newUrl = deleteUrlSchema(url);
		String laststr = newUrl.substring(newUrl.length() - 1, newUrl.length());
		if (laststr.equals("/")) {
			newUrl = newUrl.substring(0, newUrl.length() - 1);
		}

		// 此时newUrl的格式为 www.aaa.com/path 或者 www.aaa.com
		if (!Constants.IS_KEEP_FILE_URL) {
			newUrl = newUrl.substring(newUrl.lastIndexOf("/") + 1, newUrl.length());
		}

		// 判断动态页面url中没有后缀名的自动加上相应的后缀名
		if (!isFindPage(newUrl) && !FetchResourceSelector.isFindResources(newUrl)) {
			newUrl += "." + suffix;
		} else {
			newUrl += "." + Constants.DEFAULT_PAGE_SUFFIX;
		}

		if (StringUtil.isNullOrBlank(newUrl)) {
			newUrl = Constants.DEFAULT_HOME_PAGE_NAME;
		} else {

			// 默认用下划线取代url中的特殊字符
			newUrl = newUrl.replace("*", "_").replace("<", "_").replace(">", "_").replace("/", "_").replace("\\", "_")
					.replace("|", "_").replace(":", "_").replace("\"", "_").replace("?", "_");
			// 当然也可以对url中的特殊字符进行编码
			// try {
			// newUrl = URLEncoder.encode(newUrl,"UTF-8");
			// newUrl = newUrl.replace("*", "");
			// } catch (UnsupportedEncodingException e) {
			// e.printStackTrace();
			// }
		}
		return newUrl;
	}

	/**
	 * 下载的资源命名规则：与页面命名类似，不同的是url后跟的参数全部删除掉，
	 * 因为大多数页面都是动态，后面加上参数会代表不同的页面，而资源文件则不同，根本不需要
	 * 
	 * @param seedName
	 * @param url
	 * @param suffix
	 * @return
	 */
	public static String generateResourceName(String seedName, String url, String suffix) {
		String newUrl = deleteUrlSchema(url);
		String laststr = newUrl.substring(newUrl.length() - 1, newUrl.length());
		if (laststr.equals("/")) {
			newUrl = newUrl.substring(0, newUrl.length() - 1);
		}

		// 此时newUrl的格式为 www.aaa.com/path 或者 www.aaa.com
		if (!Constants.IS_KEEP_FILE_URL) {
			newUrl = newUrl.substring(newUrl.lastIndexOf("/") + 1, newUrl.length());
		}

		// 判断动态url中没有后缀名的自动加上相应的后缀名，有的资源文件没有后缀名，比如css不用写后缀照样也能引用
		// 注意：资源文件link[href]有时候是xml文件，例如：<link type="application/rss+xml"
		// href="rss"/>
		// <link type="application/wlwmanifest+xml"
		// href="wlwmanifest.xml"/>，所以要判断xml后缀
		if (!FetchResourceSelector.isFindResources(newUrl)) {
			newUrl += "." + suffix;
		}

		if (newUrl.indexOf("?") != -1) {
			newUrl = newUrl.substring(0, newUrl.indexOf("?"));
		}

		// 默认用下划线取代url中的特殊字符
		newUrl = newUrl.replace("*", "_").replace("<", "_").replace(">", "_").replace("/", "_").replace("\\", "_")
				.replace("|", "_").replace(":", "_").replace("\"", "_").replace("?", "_");
		// 当然也可以对url中的特殊字符进行编码
		// try {
		// newUrl = URLEncoder.encode(newUrl,"UTF-8");
		// newUrl = newUrl.replace("*", "");
		// } catch (UnsupportedEncodingException e) {
		// e.printStackTrace();
		// }
		return newUrl;
	}

	/**
	 * 判断url是否包含.htm/.jsp/.asp等后缀名称
	 * 
	 * @param url
	 * @return
	 */
	private static boolean isFindPage(String url) {
		boolean flag = false;
		if (url.contains(".htm") || url.contains(".jsp") || url.contains(".asp") || url.contains(".action")
				|| url.contains(".php") || url.contains(".xhtml") || url.contains(".shtm") || url.contains(".do")
				|| url.contains(".cgi") || url.contains(".xml") || url.contains(".perl")) {
			flag = true;
		}
		return flag;
	}

}
