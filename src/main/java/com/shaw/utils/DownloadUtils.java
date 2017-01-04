package com.shaw.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by Administrator on 2017/1/3 0003.
 */
public class DownloadUtils {
    //下载相关常量配置
    //连接超时
    public static final long TIMEOUT = 5000;
    //下载超时 1min
    public static final long READTIMEOUT = 60 * 1000;
    // 连接UA
    public static final String UA = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.1599.101 Safari/537.36";
    // referer  pixiv 插画下载必须设置，否则403
    public static final String REFER = "http://spapi.pixiv.net/";

    public static Logger downloadLogger = LoggerFactory.getLogger(DownloadUtils.class);


    /**
     * url 下载链接
     * savePath 保存路径(由url获取名字)
     */
    public static String downloadByUrlAndSavePath(String url, String savePath) {
        try {
            URL connUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) connUrl.openConnection();
            setHeader(conn);
            InputStream is = conn.getInputStream();
            File file = new File(savePath);
            if (!file.exists()) file.mkdirs();
            savePath = file.getAbsolutePath() + "/" + url.substring(url.lastIndexOf("/"));
            return saveStreamToFile(is, savePath, conn.getContentLengthLong());
        } catch (Exception e) {
            downloadLogger.error(String.format("%s Download Error", url));
            downloadLogger.error("Exception：", e);
            return null;
        }
    }

    /**
     * url 下载链接
     * filePath 保存文件路径(名字由filepath定义)
     */
    public static String downloadByUrlAndFilePath(String url, File filePath) {
        try {
            URL connUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) connUrl.openConnection();
            setHeader(conn);
            InputStream is = conn.getInputStream();
            String savePath = filePath.getAbsolutePath();
            return saveStreamToFile(is, savePath, conn.getContentLengthLong());
        } catch (Exception e) {
            downloadLogger.error(String.format("%s Download Error", url));
            downloadLogger.error("Exception：", e);
            return null;
        }
    }


    //使用文件内存映射保存文件
    private static String saveStreamToFile(InputStream is, String savePath, long contentLength) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(savePath, "rw");
        FileChannel channel = randomAccessFile.getChannel();
        try {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, contentLength);
            byte[] bytes = new byte[2 * 1024];
            int len;
            while ((len = is.read(bytes)) != -1) {
                buffer.put(bytes, 0, len);
            }
            buffer.force();
            downloadLogger.info(String.format("%s Download Completed", savePath));
            return savePath;
        } finally {
            channel.close();
            randomAccessFile.close();
        }
    }

    //设置请求头
    private static void setHeader(URLConnection conn) {
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Referer", REFER);
        conn.setRequestProperty("Accept-Language", "en-us,en;q=0.7,zh-cn;q=0.3");
        conn.setRequestProperty("Accept-Encoding", "utf-8");
        conn.setRequestProperty("Keep-Alive", "300");
        conn.setRequestProperty("connnection", "keep-alive");
        conn.setConnectTimeout((int) TIMEOUT);
        conn.setReadTimeout((int) READTIMEOUT);
    }

    public static Runnable getDownloadTask(final String url, final String savePath) {
        return new Runnable() {
            @Override
            public void run() {
                DownloadUtils.downloadByUrlAndSavePath(url, savePath);
            }
        };
    }
}
