package com.shaw;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by shaw on 2017/1/3 0003.
 */
@Configuration
public class ConfigBean {
    @Value("${http.timeout}")
    public static long TIMEOUT;
    @Value("${http.user-agent}")
    public static String USER_AGENT;
    @Value("${http.pixiv.refer}")
    public static String PIXIV_REFERER;
    public static Header UAHeader = new Header("User-Agent", USER_AGENT);
    public static Header REFHeader = new Header("Referer", PIXIV_REFERER);
}
