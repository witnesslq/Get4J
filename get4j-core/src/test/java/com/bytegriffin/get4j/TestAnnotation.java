package com.bytegriffin.get4j;

import com.bytegriffin.get4j.annotation.Site;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

// 测试Annotation
@Site(url="http://www.baidu.com/")
public class TestAnnotation implements PageParser{


	@Override
	public void parse(Page page) {
		// TODO Auto-generated method stub
		
	}

	public static void main(String[] args) throws Exception{
		Spider.create().annotation(TestAnnotation.class).start();
	}
}
