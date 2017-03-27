package com.bytegriffin.get4j.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ShellUtil {

	private static final String shell_path = System.getProperty("user.dir") + File.separator + "bin" + File.separator + ".sh";

	private static final Logger logger = LogManager.getLogger(ShellUtil.class);

	public static int executeShell(String shellCommand) throws IOException {
		int success = 0;
		BufferedReader bufferedReader = null;
		StringBuffer stringBuffer = new StringBuffer();
		try {
			logger.info("准备执行Shell命令 ");
			Process pid = null;
			String[] cmd = { "/bin/sh", "-c", shellCommand };
			// 执行Shell命令
			pid = Runtime.getRuntime().exec(cmd);
			if (pid != null) {
				logger.info("进程号：" + pid.toString());
				// bufferedReader用于读取Shell的输出内容
				bufferedReader = new BufferedReader(new InputStreamReader(pid.getInputStream()), 1024);
				pid.waitFor();
			} else {
				logger.info("没有pid...");
			}
			logger.info("Shell命令执行完毕，");
			String line = null;
			// 读取Shell的输出内容，并添加到stringBuffer中
			while (bufferedReader != null && (line = bufferedReader.readLine()) != null) {
				stringBuffer.append(line).append("\r\n");
			}
			String result = stringBuffer.toString();
		} catch (Exception ioe) {
			logger.info("执行Shell命令时发生异常：", ioe);
		} finally {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
			success = 1;
		}
		return success;
	}
	
	public static void main(String[] args) throws IOException{
		int status = executeShell("E:/work/workspace/Anspider/bin/anspider-env.sh");
		System.out.println(status);
	}
	

}