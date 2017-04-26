package com.bytegriffin.get4j.probe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.probe.ProbePageSerial.ProbePage;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.MD5Util;

/**
 * Probe文件存储器 <br>
 */
public class ProbeFileStorage {

    private static final Logger logger = LogManager.getLogger(ProbeFileStorage.class);

    /**
     * probe序列化文件中对象的固定长度字节，这样才容易遍历出文件中存储的多个相同大小的ProbePage对象
     */
    private static int probe_buffer_length = 89;
    public static String filename = "probe_pages.bin";
    public static String probe_file = DefaultConfig.probe_folder + filename;

    /**
     * 读取Probe文件中与url相等的值
     *
     * @param file
     * @return List
     */
    public static ProbePage read(String url) {
        FileInputStream fis = null;
        ByteArrayOutputStream swapStream = null;
        ProbePageSerial.ProbePage page = null;
        try {
            byte[] buff = new byte[probe_buffer_length];
            int rc = 0;
            swapStream = new ByteArrayOutputStream();
            fis = new FileInputStream(probe_file);
            while ((rc = fis.read(buff, 0, probe_buffer_length)) > 0) {
                swapStream.write(buff, 0, rc);
                ProbePageSerial.ProbePage probepage = ProbePageSerial.ProbePage.parseFrom(swapStream.toByteArray());
                if (probepage.getUrl().equalsIgnoreCase(MD5Util.convert(url))) {
                    page = probepage;
                    break;
                }
            }
            return page;
        } catch (Exception e) {
            logger.error("读probe文件时出错。", e);
        } finally {
            try {
                fis.close();
                swapStream.close();
            } catch (IOException e) {
                logger.error("读probe文件时出错。", e);
            }
        }
        return page;
    }

    /**
     * 读取Probe文件
     *
     * @param file
     * @return
     */
    public static List<ProbePage> read() {
        FileInputStream fis = null;
        ByteArrayOutputStream swapStream = null;
        List<ProbePage> list = new ArrayList<>();
        try {
            byte[] buff = new byte[probe_buffer_length];
            int rc = 0;
            swapStream = new ByteArrayOutputStream();
            fis = new FileInputStream(probe_file);
            while ((rc = fis.read(buff, 0, probe_buffer_length)) > 0) {
                swapStream.write(buff, 0, rc);
                ProbePageSerial.ProbePage page = ProbePageSerial.ProbePage.parseFrom(swapStream.toByteArray());
                list.add(page);
            }
            return list;
        } catch (Exception e) {
            logger.error("读probe文件时出错。", e);
        } finally {
            try {
                fis.close();
                swapStream.close();
            } catch (IOException e) {
                logger.error("读probe文件时出错。", e);
            }
        }
        return null;
    }

    /**
     * 更新Probe文件：不能直接删除并更新数据，需要生成一个临时文件进行转存
     *
     * @param probePage 本地ProbePage
     * @param content
     * @return 是否更新成功
     */
    public synchronized static ProbePage update(String url, String content) {
        FileOutputStream tempFos = null;
        ProbePageSerial.ProbePage newProbePage = null;
        List<ProbePage> list = read();
        String tempfile = probe_file + "." + System.currentTimeMillis() + ".tmp";
        try {
            if (!list.isEmpty()) {
                // 2.1 更新就是先创建一个临时文件  windows下是 C:\Users\用户名\AppData\Local\Temp\
                File tempFile = new File(tempfile);
                tempFos = new FileOutputStream(tempFile);
                // 2.2 将probe文件除了url相同的数据转存到临时文件中
                for (ProbePage pp : list) {
                    if (url.equals(pp.getUrl())) {
                        continue;
                    }
                    ProbePageSerial.ProbePage tempProbe = ProbePageSerial.ProbePage.newBuilder().setUrl(MD5Util.convert(pp.getUrl())).
                            setContent(MD5Util.convert(pp.getContent())).setProbeTime(DateUtil.getCurrentDate()).build();
                    tempProbe.writeTo(tempFos);
                }

                // 2.3 将要更新的数据追加到临时文件中
                return append(tempFile.getCanonicalPath(), url, content);
            }
        } catch (IOException e) {
            logger.error("更新probe文件时出错。", e);
        } finally {
            try {
                tempFos.flush();
                tempFos.close();
                // 2.4 将临时文件重命名为probe文件，原probe文件存在的话就直接覆盖
                // 必须要在流关闭之后才能重命名，否则在windows系统下报错
                if (!list.isEmpty()) {
                	Files.move(Paths.get(tempfile), Paths.get(probe_file), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                logger.error("更新probe文件时出错。", e);
            }
        }
        return newProbePage;
    }

    /**
     * 追加内容到Probe文件：将url和页面内容转成固定长度的MD5格式，以便每次探测时
     *
     * @param file
     * @param url
     * @param content
     */
    public synchronized static ProbePage append(String file, String url, String content) {
        FileOutputStream ff = null;
        ProbePageSerial.ProbePage probePage = null;
        try {
            ff = new FileOutputStream(file, true);// 追加内容
            probePage = ProbePageSerial.ProbePage.newBuilder().
                    setUrl(MD5Util.convert(url)).setContent(MD5Util.convert(content)).setProbeTime(DateUtil.getCurrentDate()).build();
            probePage.writeTo(ff);
            ff.flush();
            return probePage;
        } catch (Exception e) {
            logger.error("写probe文件时出错。", e);
        } finally {
            try {
                ff.close();
            } catch (IOException e) {
                logger.error("写probe文件时出错。", e);
            }
        }
        return probePage;
    }
}
