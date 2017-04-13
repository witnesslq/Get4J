package com.bytegriffin.get4j.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
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
	 * @return List<String>
	 */
	public static List<String> readUserAgentFile(String userAgentFile) {
		return readFileLine(userAgentFile);
	}

	/**
	 * 获取系统绝对路径 <br />
	 * 将配置文件转换为爬虫系统的绝对路径，将classpath:/conf/user_agent转换成/opt/work/xxx/conf/user_agent
	 *
	 * @param classpath
	 *            String
	 * @return String
	 */
	public static String getSystemAbsolutePath(String classpath) {
		String newpath;
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
	 * @param httpProxyFile
	 *            String
	 * @return List<HttpProxy>
	 */
	public static List<HttpProxy> readHttpProxyFile(String httpProxyFile) {
		List<String> list = readFileLine(httpProxyFile);
		if (list == null || list.size() == 0) {
			return null;
		}
		List<HttpProxy> newList = new ArrayList<>();
		for (String str : list) {
			if (StringUtil.isNullOrBlank(str)) {
				continue;
			}
			HttpProxy hp;
			if (str.contains("@")) {
				String[] array = str.split("@");
				if (array.length > 0) {
					String[] front = array[0].split(":");
					String[] end = array[1].split(":");
					hp = new HttpProxy(front[0], front[1], end[0], end[1]);
				} else {
					if (str.contains(":")) {
						String[] front = str.split(":");
						hp = new HttpProxy(front[0], front[1]);
					} else {
						hp = new HttpProxy(str);
					}
				}
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
	 *            代理字符串
	 * @return HttpProxy
	 */
	public static HttpProxy formatProxy(String proxyString) {
		if (StringUtil.isNullOrBlank(proxyString)) {
			return null;
		}
		HttpProxy hp;
		if (proxyString.contains("@")) {
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
	 *            String
	 * @return List<String>
	 */
	private static List<String> readFileLine(String configFile) {
		configFile = getSystemAbsolutePath(configFile);
		File file = new File(configFile);
		if (!file.exists()) {
			return null;
		}
		boolean fc = isExistContont(configFile);
		if (!fc) {
			return null;
		}
		List<String> result = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(configFile));// 构造一个BufferedReader类来读取文件
			String s;
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
	 *            String
	 * @return boolean
	 */
	private static boolean isExistContont(String filename) {
		File file = new File(filename);
		return !(file.length() == 0);
	}

	/**
	 * 生成dump文件夹以及dump下的文件
	 *
	 * @param folder
	 *            String
	 * @param filename
	 *            String
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
	 *            File
	 * @param contents
	 *            Collection<String>
	 */
	public static synchronized void append(File file, Collection<String> contents) {
		if (contents != null && !contents.isEmpty()) {
			FileWriter fw = null;
			try {
				fw = new FileWriter(file, true);
				for (String str : contents) {
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
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 删除文件中的某行内容
	 *
	 * @param file
	 *            String
	 * @param proxy
	 *            String
	 */
	public static void removeLine(String file, String proxy) {
		try {
			file = FileUtil.getSystemAbsolutePath(file);
			File inFile = new File(file);
			File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

			BufferedReader br = new BufferedReader(new FileReader(file));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
			String line;

			while ((line = br.readLine()) != null) {
				if (!line.trim().equals("") && !line.trim().equals(proxy)) {
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
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 在磁盘上创建下载文件夹
	 *
	 * @param diskDir String
	 */
	public static String makeDownloadDir(String diskDir) {
		diskDir = getSystemAbsolutePath(diskDir);
		String laststr = diskDir.substring(diskDir.length() - 1, diskDir.length());
		if (laststr.equals(File.separator)) {
			diskDir = diskDir.substring(0, diskDir.length() - 1);
		}
		File homedir = new File(diskDir);
		boolean iscreate = false;
		if (!homedir.exists()) {
			try {
				iscreate = homedir.mkdirs();
			} catch (Exception ex) {
				logger.warn("不能创建文件夹[" + diskDir + "]，它可能包含一些特殊字符串。", ex);
			}
		}
		if (iscreate) {
			diskDir = diskDir.endsWith(File.separator) ? diskDir : diskDir + File.separator;
		}
		return diskDir;
	}

	/**
	 * 在本地磁盘生成页面
	 *
	 * @param page
	 *            Page
	 */
	public static void downloadPagesToDisk(Page page) {
		String folderName = Constants.DOWNLOAD_DIR_CACHE.get(page.getSeedName());
		String fileName = folderName + File.separator;
		if (page.isJsonContent()) {
			fileName += generatePageName(page.getUrl(), Constants.JSON_PAGE_SUFFIX);
		} else if (page.isHtmlContent()) {
			fileName += generatePageName(page.getUrl(), Constants.DEFAULT_PAGE_SUFFIX);
		} else if (page.isXmlContent()) {
			fileName += generatePageName(page.getUrl(), Constants.XML_PAGE_SUFFIX);
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
	 *            String
	 * @param content
	 *            byte[]
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
	private static String deleteUrlSchema(String url) {
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
	 * @param url
	 *            String
	 * @param suffix
	 *            文件后缀名
	 * @return String
	 */
	private static String generatePageName(String url, String suffix) {
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
		if (!isFindPage(newUrl)) {
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
	 * 注意：有些资源文件是一个页面经过跳转后的资源文件，比如www.aa.com/cc.php===>www.aa.com/img.jpg
	 *
	 * @param url
	 *            String
	 * @param suffix
	 *            String
	 * @return String
	 */
	public static String generateResourceName(String url, String suffix) {
		// String newUrl = deleteUrlSchema(url);
		String laststr = url.substring(url.length() - 1, url.length());
		if (laststr.equals("/")) {
			url = url.substring(0, url.length() - 1);
		}
		// 此时newUrl的格式为 www.aaa.com/path 或者 www.aaa.com
		if (!Constants.IS_KEEP_FILE_URL) {
			url = url.substring(url.lastIndexOf("/") + 1, url.length());
		}
		// 去除url中的参数
		if (url.contains("?")) {
			url = url.substring(0, url.indexOf("?"));
		}

		// 判断动态url中没有后缀名的自动加上相应的后缀名，有的资源文件没有后缀名，比如css不用写后缀照样也能引用
		// 注意：资源文件link[href]有时候是xml文件，例如：<link type="application/rss+xml"
		// href="rss"/>
		// <link type="application/wlwmanifest+xml"
		// href="wlwmanifest.xml"/>，所以要判断xml后缀
		if (!FetchResourceSelector.isFindResources(url)) {
			url += "." + suffix;
		}
		// 默认用下划线取代url中的特殊字符
		url = url.replace("*", "_").replace("<", "_").replace(">", "_").replace("/", "_").replace("\\", "_")
				.replace("|", "_").replace(":", "_").replace("\"", "_").replace("?", "_");
		// 当然也可以对url中的特殊字符进行编码
		// try {
		// newUrl = URLEncoder.encode(newUrl,"UTF-8");
		// newUrl = newUrl.replace("*", "");
		// } catch (UnsupportedEncodingException e) {
		// e.printStackTrace();
		// }
		return url;
	}

	/**
	 * 根据url的后缀名（.htm/.jsp/.asp等）来判断是否为一个页面
	 *
	 * @param url
	 *            String
	 * @return boolean
	 */
	private static boolean isFindPage(String url) {
		boolean flag = false;
		if (url.contains(".htm") || url.contains(".jsp") || url.contains(".asp") || url.contains(".action")
				|| url.contains(".php") || url.contains(".xhtml") || url.contains(".shtm") || url.contains(".do")
				|| url.contains(".cgi") || url.contains(".xml") || url.contains(".perl") || url.contains(".ftm")
				|| url.contains(".vm") || url.contains(".thymes") || url.contains(".tml") || url.contains(".xjsp")
				|| url.contains(".jsf") || url.contains(".ftl")) {
			flag = true;
		}
		return flag;
	}

	/**
	 * 事先创建指定大小的空内容的文件
	 *
	 * @param fileName
	 *            String
	 * @param fileSize
	 *            long
	 */
	public static void createNullFile(String fileName, long fileSize) {
		File newFile = new File(fileName);
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(newFile, "rw");
			raf.setLength(fileSize);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 下载文件到磁盘上
	 *
	 * @param fileName
	 *            String
	 * @param contentLength
	 *            Long
	 * @param content
	 *            InputStream
	 */
	public static void writeFile(String fileName, Long contentLength, InputStream content) {
		long offset = 0;
		BufferedInputStream bis = new BufferedInputStream(content);
		byte[] buff = new byte[1024];
		int bytesRead;
		File newFile = new File(fileName);
		RandomAccessFile raf = null;
		try {

			int unit = (int) (contentLength / 100);// 将文件大小分成100分
			int unitProgress = 0; // 用于保存当前进度(1~100%)
			raf = new RandomAccessFile(newFile, "rw");
			while ((bytesRead = bis.read(buff, 0, buff.length)) != -1) {
				raf.seek(offset);
				raf.write(buff, 0, bytesRead);
				offset = offset + bytesRead;
				int temp = (int) (offset / unit); // 计算当前百分比进度
				if (temp >= 1 && temp > unitProgress) {// 如果下载过程出现百分比变化
					unitProgress = temp;
					if (unitProgress % 20 == 0) {
						logger.info("线程[" + Thread.currentThread().getName() + "]下载文件[" + fileName + "]的进度为["
								+ unitProgress + "%]。");
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
				bis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * 递归删除文件夹以及下面所有的文件
	 * @param path
	 */
	public static void deleteFile(String path) {
		File file = new File(path);
		if (!file.isDirectory()) {
			file.delete();
		} else if (file.isDirectory()) {
			File[] fileList = file.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				File delfile = fileList[i];
				if (!delfile.isDirectory()) {
					delfile.delete();
				} else if (delfile.isDirectory()) {
					deleteFile(fileList[i].getPath());
				}
			}
			file.delete();
		}
	}

}
