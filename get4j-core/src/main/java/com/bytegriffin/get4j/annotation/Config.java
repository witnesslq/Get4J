package com.bytegriffin.get4j.annotation;

public @interface Config {

	String email() default "";
	
	String downloadFilenameRule() default "default";
	
	
}
