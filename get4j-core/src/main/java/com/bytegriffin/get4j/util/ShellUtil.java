package com.bytegriffin.get4j.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ShellUtil {

    private static final Logger logger = LogManager.getLogger(ShellUtil.class);

    public static void executeShell(String shellCommand) {
        BufferedReader stdInput = null;
        BufferedReader stdError = null;
        try {
            String[] cmd = {"/bin/sh", "-c", shellCommand};
            ProcessBuilder pb = new ProcessBuilder(cmd);  
            Process p = pb.start();
            stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            //必须要输出
            String s;
            while ((s = stdInput.readLine()) != null) {
            	logger.error(s);
            }
            while ((s = stdError.readLine()) != null) {
            	logger.error(s);
            }
            try {
                 p.waitFor();
            } catch (InterruptedException e) {
                logger.info("执行Shell命令["+shellCommand+"]时发生异常：", e);
            }
            logger.info("Shell命令["+shellCommand+"]执行完毕");
        } catch (Exception ioe) {
            logger.info("执行Shell命令["+shellCommand+"]时发生异常：", ioe);
        } finally {
            try {
                if (stdInput != null) {
                    stdInput.close();
                }
                if (stdError != null) {
                    stdError.close();
                }
            } catch (IOException e) {
                logger.info("执行Shell命令["+shellCommand+"]时发生异常：", e);
            }
        }
    }



}