package com.bytegriffin.get4j.system;

import com.bytegriffin.get4j.probe.ProbeFileStorage;
import com.bytegriffin.get4j.probe.ProbePageSerial.ProbePage;

public class TestProbeFileStorage {
	
	public static void main(String[] args){
		ProbePage pp = ProbeFileStorage.read("http://blog.chinaunix.net/site/index/page/1.html");
		System.out.println(pp.getFinish());
	}

}
