package com.bytegriffin.get4j.net.http;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import com.bytegriffin.get4j.util.StringUtil;

/**
 * Http代理
 */
public class HttpProxy {

	private String ip;
	private String port;
	private String username;
	private String password;
	private HttpHost httpHost;
	private CredentialsProvider credsProvider;
	
	public HttpProxy(String ip) {
		if(!StringUtil.isNullOrBlank(ip)){
			this.ip = ip.trim();
		}
		this.port = "80";
		this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port));
	}

	public HttpProxy(String ip,String port) {
		if(!StringUtil.isNullOrBlank(ip)){
			this.ip = ip.trim();
		}
		if(!StringUtil.isNullOrBlank(port)){
			this.port = port.trim();
		}
		this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port));
	}
	
	public HttpProxy(String ip,Integer port) {
		if(!StringUtil.isNullOrBlank(ip)){
			this.ip = ip.trim();
		}
		if(port != null){
			this.port = String.valueOf(port);
		}
		this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port));
	}
	
	public HttpProxy(String ip,String port,String schema) {
		if(!StringUtil.isNullOrBlank(ip)){
			this.ip = ip.trim();
		}
		if(!StringUtil.isNullOrBlank(port)){
			this.port = port.trim();
		}
		this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port),schema);
	}

	public HttpProxy(String ip, String port,String username, String password) {
		if(!StringUtil.isNullOrBlank(ip)){
			this.ip = ip.trim();
		}
		if(!StringUtil.isNullOrBlank(port)){
			this.port = port.trim();
		}
		this.username = username.trim();
		if(StringUtil.isNullOrBlank(password)){
			this.password = "";
		} else {
			this.password = password.trim();
		}
		this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port));
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(this.ip, Integer.valueOf(this.port)), new UsernamePasswordCredentials(this.username, this.password));
		this.credsProvider = credsProvider;
	}
	
	public String toString(){
		String str = this.ip + ":" + this.port;
		if(!StringUtil.isNullOrBlank(this.username)){
			str+= "@" +this.username ;
		}
		if(!StringUtil.isNullOrBlank(this.password)){
			str+= ":" + this.password;
		}
		return str;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public HttpHost getHttpHost() {
		return httpHost;
	}

	public void setHttpHost(HttpHost httpHost) {
		this.httpHost = httpHost;
	}

	public CredentialsProvider getCredsProvider() {
		return credsProvider;
	}

	public void setCredsProvider(CredentialsProvider credsProvider) {
		this.credsProvider = credsProvider;
	}


	
}
