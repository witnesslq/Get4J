package com.bytegriffin.get4j.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;

import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Launcher;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.StringUtil;
import com.bytegriffin.get4j.core.UrlQueue;

/**
 * 健康状态：可用jconsole查询
 */
public class HealthStatus implements HealthStatusMXBean {

	private String seedName;

	public HealthStatus(String seedName) {
		this.seedName = seedName;
	}

	@Override
	public String getOS() {
		OperatingSystemMXBean operateSystemMBean = ManagementFactory.getOperatingSystemMXBean();
		String operateName = operateSystemMBean.getName();
		int processListCount = operateSystemMBean.getAvailableProcessors();
		String archName = operateSystemMBean.getArch();
		String version = operateSystemMBean.getVersion();
		return operateName + " " + archName + " " + version + " " + processListCount;
	}

	@Override
	public MemoryUsage getHeapMemory() {
		MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		return memory.getHeapMemoryUsage();
	}

	@Override
	public MemoryUsage getNonHeapMemory() {
		MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		return memory.getNonHeapMemoryUsage();
	}

	@Override
	public ThreadInfo[] getThreads() {
		ThreadMXBean tm = (ThreadMXBean) ManagementFactory.getThreadMXBean();
		long[] ids = tm.getAllThreadIds();
		return tm.getThreadInfo(ids);
	}

	@Override
	public ThreadInfo[] getDeadLockThreads() {
		ThreadMXBean thread = ManagementFactory.getThreadMXBean();
		long[] deadlockedIds = thread.findDeadlockedThreads();
		if (deadlockedIds != null && deadlockedIds.length > 0) {
			return thread.getThreadInfo(deadlockedIds);
		}
		return null;
	}

	@Override
	public int getVisitedUrlCount() {
		return UrlQueue.getVisitedUrlCount(seedName);
	}

	@Override
	public int getUnVisitUrlCount() {
		return UrlQueue.getUnVisitedUrlCount(seedName);
	}

	@Override
	public int getFailedUrlCount() {
		return UrlQueue.getFailVisitedUrlCount(seedName);
	}

	@Override
	public List<String> getExceptions() {
		return ExceptionCatcher.getExceptions(seedName);
	}

	@Override
	public String getCostTime() {
		String str = Globals.PER_START_TIME_CACHE.get(seedName);
		if(StringUtil.isNullOrBlank(str)){
			str = DateUtil.getCurrentDate();
		}
		return DateUtil.getCostDate(str);
	}

	@Override
	public String getSpiderStatus() {
		if (!Globals.LAUNCHER_CACHE.containsKey(seedName)) {
			return "已销毁";
		}
		Launcher launcher = Globals.LAUNCHER_CACHE.get(seedName);
		if (launcher.getCondition()) {
			return "运行中";
		} else {
			return "已暂停";
		}
	}

}
