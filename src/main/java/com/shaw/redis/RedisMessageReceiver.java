package com.shaw.redis;

import com.alibaba.fastjson.JSONObject;
import com.shaw.netty.SocketMessage;
import com.shaw.utils.DownloadUtils;
import com.shaw.utils.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.concurrent.CountDownLatch;

import com.shaw.constants.Constants;

/**
 * Created by shaw on 2017/1/3 0003.
 */
public class RedisMessageReceiver {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageReceiver.class);
    //消息消费方法名称
    public static final String DEFAULT_LISTENEER_METHOD = "receiveMessage";

    //同步计数器,控制消息监听器进程退出时机
    private CountDownLatch latch;

    @Autowired
    public RedisMessageReceiver(CountDownLatch latch) {
        this.latch = latch;
    }

    /**
     * 消息消费方法
     */
    public void receiveMessage(String message) {
        JSONObject messageObject = JSONObject.parseObject(message);
        String topic = messageObject.getString("topic");
        if (Constants.TOPIC_DOWNLOAD.equals(topic)) {
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
                logger.info("Wrong Message:" + message);
            }
        } else if (Constants.TOPIC_TASK.equals(topic)) {
            SocketMessage socketMessage = null;
            try {
                socketMessage = JSONObject.parseObject(messageObject.getString("content"),
                        SocketMessage.class);
            } catch (Exception e) {
                logger.error("parse msg fail:" + message);
            }
            SocketMsgSender.handlerRedisMsg(socketMessage);
        } else {

        }
    }
}
