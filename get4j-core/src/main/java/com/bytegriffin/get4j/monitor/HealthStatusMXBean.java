package com.bytegriffin.get4j.monitor;

import java.util.List;

public interface HealthStatusMXBean {

	public int getVisitedUrlCount();

	public int getUnVisitUrlCount();

	public int getFailedUrlCount();

	public List<String> getExceptions();

	public String getCostTime();

	public String getSpiderStatus();

}
