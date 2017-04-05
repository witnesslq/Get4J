package com.bytegriffin.get4j;

public class TestBigFileDownload {

    public static void main(String[] args) throws Exception {
        String url = "http://dldir1.qq.com/qqfile/qq/QQ8.9.1/20453/QQ8.9.1.exe";
        //url="https://down1.3987.com/2010/cs6_extended_3987com.zip";
        //url = "https://avatars1.githubusercontent.com/u/5327447?v=3&s=40";
        //url = "http://www.zxjsq.net/";
        //url = "http://down2.m928.com:4563/xp/XTZJ_GhostXPSP3_2017.iso";
        Spider.cascade().fetchUrl(url).defaultDownloadDisk()
                .jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                .thread(1).start();
    }

}
