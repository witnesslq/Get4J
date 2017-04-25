package com.bytegriffin.get4j.probe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.core.Constants;
import com.bytegriffin.get4j.probe.ProbePageSerial;
import com.bytegriffin.get4j.probe.ProbePageSerial.ProbePage;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.MD5Util;

/**
 * Probe文件存储器 <br>
 * 注意：本类在Win10操作系统下会出现文件消失的状况。
 */
public class ProbeFileStorage {

    private static final Logger logger = LogManager.getLogger(ProbeFileStorage.class);

    /**
     * probe序列化文件中对象的固定长度字节，这样才容易遍历出文件中存储的多个相同大小的ProbePage对象
     */
    private static int probe_buffer_length = 89;
    public static String filename = "probe_pages";
    public static String probe_file = Constants.probe_folder + filename;

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
        try {
            if (!list.isEmpty()) {
                // 2.1 更新就是先创建一个临时文件  windows下是 C:\Users\用户名\AppData\Local\Temp\
                File tempFile = new File(probe_file + System.currentTimeMillis() + ".tmp");
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
                newProbePage = append(tempFile.getCanonicalPath(), url, content);

                // 2.4 删除原probe文件
                if (!new File(probe_file).delete()) {
                    logger.error("不能删除probe原文件。");
                }
                // 2.5 将临时文件重命名为probe文件
                if (!tempFile.renameTo(new File(probe_file))) {
                    logger.error("不能重命名probe新文件。");
                }

                return newProbePage;
            }
        } catch (IOException e) {
            logger.error("更新probe文件时出错。", e);
        } finally {
            // 必须关闭才能重命名成功，临时文件在关闭后会自动删除
            try {
                tempFos.flush();
                tempFos.close();
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
