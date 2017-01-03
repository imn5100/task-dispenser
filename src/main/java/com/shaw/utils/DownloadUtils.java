package com.shaw.utils;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by Administrator on 2017/1/3 0003.
 */
@Component
public class DownloadUtils {
    //下载相关常量配置
    public static final long TIMEOUT = 5000;
    public static final String UA = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.1599.101 Safari/537.36";
    public static final String REFER = "http://spapi.pixiv.net/";
    public static final Header UAHeader = new Header("User-Agent", UA);
    public static final Header REFHeader = new Header("Referer", REFER);

    public static HttpClient httpClient;
    public static Logger downloadLogger = LoggerFactory.getLogger(DownloadUtils.class);

    static {
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setConnectionManagerTimeout(TIMEOUT);
        httpClient = new HttpClient(clientParams);
    }

    public static String downloadByUrl(String url, String savePath) {
        GetMethod method = new GetMethod(url);
        method.setRequestHeader(REFHeader);
        method.setRequestHeader(UAHeader);
        try {
            httpClient.executeMethod(method);
            InputStream is = method.getResponseBodyAsStream();
            File file = new File(savePath);
            if (!file.exists()) file.mkdirs();
            savePath = file.getAbsolutePath() + "/" + url.substring(url.lastIndexOf("/"));
            if (new File(savePath).exists()) {
                downloadLogger.info("Existing file with the same name, deemed to have been downloaded");
                return savePath;
            } else {
                //使用文件内存映射保存文件
                RandomAccessFile randomAccessFile = new RandomAccessFile(savePath, "rw");
                FileChannel channel = randomAccessFile.getChannel();
                MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, method.getResponseContentLength());
                byte[] bytes = new byte[2 * 1024];
                int len;
                while ((len = is.read(bytes)) != -1) {
                    buffer.put(bytes, 0, len);
                }
                buffer.force();
                channel.close();
                randomAccessFile.close();
                downloadLogger.info(String.format("%s Download Completed", savePath));
                return savePath;
            }
        } catch (Exception e) {
            downloadLogger.error(String.format("%s Download Error", url));
            downloadLogger.error("Exception：", e);
            return null;
        }
    }

}
