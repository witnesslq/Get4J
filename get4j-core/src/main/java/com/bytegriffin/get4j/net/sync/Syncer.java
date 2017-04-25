package com.bytegriffin.get4j.net.sync;

import java.util.List;

/**
 * 资源同步接口，有多种选项：ftp/rsync/scp
 */
public interface Syncer {

    void sync();

    void setBatch(List<String> batch);

}
