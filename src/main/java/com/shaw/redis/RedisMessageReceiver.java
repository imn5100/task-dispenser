package com.shaw.redis;

import com.alibaba.fastjson.JSONObject;
import com.shaw.utils.DownloadUtils;
import com.shaw.utils.ThreadPoolManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;

/**
 * Created by shaw on 2017/1/3 0003.
 */
public class RedisMessageReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisMessageReceiver.class);
    /**
     * 消息消费方法名称
     */
    public static final String DEFAULT_LISTENEER_METHOD = "receiveMessage";
    //消息主题
    public static final String TOPIC = "download";


    private CountDownLatch latch;

    @Autowired
    public RedisMessageReceiver(CountDownLatch latch) {
        this.latch = latch;
    }

    public void receiveMessage(String message) {
        JSONObject messageObject = JSONObject.parseObject(message);
        System.out.println(messageObject);
        String topic = messageObject.getString("topic");
        if (TOPIC.equals(topic)) {
            String url = messageObject.getString("url");
            if (!StringUtils.isEmpty(url)) {
                String path = messageObject.getString("path");
                //启动下载任务线程
                if (!StringUtils.isEmpty(path)) {
                    ThreadPoolManager.INSTANCE.execute(DownloadUtils.getDownloadTask(url, path));
                } else {
                    //默认保存路径为项目路径根路径
                    ThreadPoolManager.INSTANCE.execute(DownloadUtils.getDownloadTask(url, "defaultSavePath/"));
                }
            } else {
                LOGGER.info("Wrong Message:" + message);
            }
        } else {
            //其他消息处理
        }
    }
}
