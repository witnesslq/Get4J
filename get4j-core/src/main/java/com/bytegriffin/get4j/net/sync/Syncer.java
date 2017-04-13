package com.bytegriffin.get4j.net.sync;

/**
 * 资源同步接口，有多种选项：ftp/rsync
 */
public interface Syncer {
	
	void sync();
}
