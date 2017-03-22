package com.bytegriffin;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.bytegriffin.core.Page;
import com.bytegriffin.store.DBStorage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class TestStore {

	public static void testDB() throws UnsupportedEncodingException {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://localhost:3306/anspider?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root");
		config.setMaximumPoolSize(50);
		config.setConnectionTestQuery("SELECT 1");
		HikariDataSource dataSource = new HikariDataSource(config);
		DBStorage dbs = new DBStorage();
		Page page = new Page("seed1","http://invest.ppdai.com/loan/info?id=38604623");
		page.setHtmlContent("test");
		Page dbpage = dbs.readOne(dataSource, page);
		System.out.println(URLDecoder.decode(dbpage.getHtmlContent(), "UTF-8"));
	}

	public static void main(String[] args) throws Exception {
		testDB();
		
	}
}
