package com.bytegriffin.get4j.net.sync;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.bytegriffin.get4j.util.ShellUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Scp同步器：用于将下载的资源文件资源文件，比如：avatar文件同步到图片服务器
 * 由于Scp本身不支持增量同步，所以需要先在目标服务器端创建文件夹，然后一个一个文件进行复制
 * 需要用ssh-keygen配置无密码方式，目前只支持Unix，不支持windows
 * @see com.bytegriffin.get4j.core.JobController
 */
public class ScpSyncer implements Syncer{
	private static final Logger logger = LogManager.getLogger(ScpSyncer.class);
	private String host;
	private String username;
	private String port;
	private String dir;
	// key: seedname value: avatar list
	private Map<String, List<String>> resources = new HashMap<>();

    public ScpSyncer(String host, String username, String dir, String port) {
        this.host = host;
        this.username = username;
        this.port = port;
        this.dir = dir.endsWith(File.separator) ? dir : dir + File.separator;
    }

    /**
     * 设置不同seedName的资源列表
     * @param avatars 资源文件列表 一般特指avatar资源
     */
	public void setBatch(List<String> avatars) {
		int i=0;
		List<String> list;
		for(String resource : avatars){
			String dir = resource.substring(0, resource.indexOf(BatchScheduler.split));
			resource = resource.substring(resource.indexOf(BatchScheduler.split) + 1);
			if(i == 0){
				list = new ArrayList<>();
				list.add(resource);
				resources.put(dir, list);
			} else if(resources.containsKey(dir)){
				resources.get(dir).add(resource);
			} else {
				list = new ArrayList<>();
				list.add(resource);
				resources.put(dir, list);
			}
			i ++ ;
		}
		
	}

	@Override
	public void sync() {
		for(String seedname : resources.keySet()){
			List<String> files = resources.get(seedname);
			StringBuilder sb = new StringBuilder();
			for(String file : files){
				sb.append(file).append(" ");
			}
			String command = "scp -pB -P "+port+" "+sb.toString()+" "+username+"@"+host+":"+dir+seedname;
			ShellUtil.executeShell(command);
			try { //如果不同的seed太多，可以减慢同步速度
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error("Scp同步资源时出错。",e);
			}
		}
	}

	public String getHost() {
		return host;
	}

	public String getUsername() {
		return username;
	}

	public String getPort() {
		return port;
	}

	public String getDir() {
		return dir;
	}
	
	
}
