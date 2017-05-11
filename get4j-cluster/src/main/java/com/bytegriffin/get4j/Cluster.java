package com.bytegriffin.get4j;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Configuration;
import com.bytegriffin.get4j.conf.ConfigurationXmlHandler;
import com.bytegriffin.get4j.conf.Context;
import com.bytegriffin.get4j.conf.CoreSeedsXmlHandler;
import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.DynamicField;
import com.bytegriffin.get4j.conf.DynamicFieldXmlHandler;
import com.bytegriffin.get4j.conf.ResourceSync;
import com.bytegriffin.get4j.conf.ResourceSyncYamlHandler;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.SpiderEngine;

public class Cluster{
	
	private static final Logger logger = LogManager.getLogger(Cluster.class);

	public static void main(String[] args) {
		DefaultConfig.closeHttpClientLog();
        Context context = new Context(new CoreSeedsXmlHandler());
        List<Seed> seeds = context.load();

        context = new Context(new ResourceSyncYamlHandler());
        ResourceSync synchronizer = context.load();

        context = new Context(new ConfigurationXmlHandler());
        Configuration configuration = context.load();

        context = new Context(new DynamicFieldXmlHandler());
        List<DynamicField> dynamicFields = context.load();

        SpiderEngine.create().setSeeds(seeds).setResourceSync(synchronizer).setConfiguration(configuration).setDynamicFields(dynamicFields).build();
        logger.info("爬虫集群开始启动...");
	}


}
