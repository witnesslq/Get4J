package com.bytegriffin.get4j.net.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ftp同步器：用于将下载的资源文件资源文件，比如：avatar文件同步到图片服务器
 * 注意：远程的dir目录就是SeedName
 */
public class FtpSyncer implements Syncer{

	private static final Logger logger = LogManager.getLogger(FtpSyncer.class);

	private String host;
	private String port;
	private String username;
	private String password;
	private List<String> batch;

	public FtpSyncer(String host, String port, String username, String password){
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public void setBatch(List<String> batch) {
		this.batch = batch;
	}

	public void sync() {
		FTPClient ftpClient = new FTPClient();
		ftpClient.setControlEncoding("UTF-8");
		try {
			ftpClient.connect(host, Integer.valueOf(port));
			ftpClient.login(username, password);
			int replyCode = ftpClient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(replyCode)) {
				return;
			}
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			for(String resource : batch){
				String dir = resource.substring(0, resource.indexOf(BatchScheduler.split));
				resource = resource.substring(resource.indexOf(BatchScheduler.split) + 1);
				String fileName = resource.contains(File.separator)? resource.substring(resource.lastIndexOf(File.separator) + 1) : resource;
				FileInputStream inputStream = null;
				try {
					inputStream = new FileInputStream(new File(resource));

					ftpClient.makeDirectory(dir);
					ftpClient.changeWorkingDirectory(dir);
					ftpClient.storeFile(fileName, inputStream);
					ftpClient.changeToParentDirectory();
				} catch (FileNotFoundException e) {
				} finally{
					if(inputStream != null){
						inputStream.close();
					}
				}
			}
		} catch (Exception e) {
			logger.error("ftp同步资源文件时出错。", e);
		} finally {
			try {
				
				ftpClient.logout();
				if (ftpClient.isConnected()) {
					ftpClient.disconnect();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}


}