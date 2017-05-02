package com.bytegriffin.get4j.monitor;

import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.util.List;

public interface HealthStatusMXBean {

	public String getOS();

	public MemoryUsage getHeapMemory();

	public MemoryUsage getNonHeapMemory();

	public ThreadInfo[] getThreads();

	public ThreadInfo[] getDeadLockThreads();

	public int getVisitedUrlCount();

	public int getUnVisitUrlCount();

	public int getFailedUrlCount();

	public List<String> getExceptions();

	public String getCostTime();

	public String getSpiderStatus();

}
