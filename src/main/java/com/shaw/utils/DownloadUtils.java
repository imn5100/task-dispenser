package com.shaw.utils;

import com.shaw.ConfigBean;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Administrator on 2017/1/3 0003.
 */
public class DownloadUtils {
    public static HttpClient httpClient;
    public static Logger downloadLogger = LoggerFactory.getLogger(DownloadUtils.class);

    static {
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setConnectionManagerTimeout(ConfigBean.TIMEOUT);
        httpClient = new HttpClient(clientParams);
    }

    public static boolean downloadByUrl(String url, String savePath) {
        GetMethod method = new GetMethod(url);
        method.setRequestHeader(ConfigBean.REFHeader);
        method.setRequestHeader(ConfigBean.UAHeader);
        try {
            httpClient.executeMethod(method);
            InputStream is = method.getResponseBodyAsStream();
            saveFile(url, savePath, is);
            return true;
        } catch (Exception e) {
            downloadLogger.error(String.format("Download error:url:%s,savePath", e.getMessage()));
            return false;
        }
    }

    public static void saveFile(String url, String savePath, InputStream is) throws IOException {
        File file = new File(savePath);
        if (!file.exists()) file.mkdirs();
        savePath = file.getAbsolutePath() + "/" + url.substring(url.lastIndexOf("/"));
        File saveFile = new File(savePath);
        if (saveFile.exists())
            downloadLogger.info("Existing file with the same name, deemed to have been downloaded");
        FileUtils.copyInputStreamToFile(is, saveFile);
    }
}
