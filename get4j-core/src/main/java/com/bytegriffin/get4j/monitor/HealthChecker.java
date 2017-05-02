package com.bytegriffin.get4j.monitor;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 系统健康检查入口
 */
public class HealthChecker {

	private static final String jmx_server_name = "Get4J";
	private static final Logger logger = LogManager.getLogger(HealthChecker.class);
	private static Map<String, HealthStatus> healthStatusMap = new HashMap<>();

	public void register(String seedName) {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		try {
			HealthStatus healthStatus = new HealthStatus(seedName);
			ObjectName objectName = new ObjectName(jmx_server_name + ":name=" + seedName);
			server.registerMBean(healthStatus, objectName);
			healthStatusMap.put(seedName, healthStatus);
			// CounterMonitor monitor = new CounterMonitor();
			// monitor.addObservedObject(objectName);
			// monitor.setDifferenceMode(true);
			// monitor.setGranularityPeriod(100);
			// monitor.setObservedAttribute("unVisitUrlCount");
			// monitor.start();
		} catch (Exception e) {
			logger.error("种子[" + seedName + "]注册health失败。", e);
		}
	}

	public HealthStatus snapshot(String seedName){
		return healthStatusMap.get(seedName);
	}


}
