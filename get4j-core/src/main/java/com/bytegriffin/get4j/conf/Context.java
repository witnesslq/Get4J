package com.bytegriffin.get4j.conf;

/**
 * 上下文环境
 */
public class Context {

    private AbstractConfig config;

    public Context(AbstractConfig config) {
        this.config = config;
    }

    public <T> T load() {
        return (T) this.config.load();
    }
}
