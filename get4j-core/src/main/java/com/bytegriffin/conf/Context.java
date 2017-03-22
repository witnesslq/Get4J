package com.bytegriffin.conf;


public class Context {

	private AbstractConfig config;

	public Context(AbstractConfig config) {
		this.config = config;
	}

	@SuppressWarnings("unchecked")
	public <T> T load() {
		return   (T) this.config.load();
	}
}
