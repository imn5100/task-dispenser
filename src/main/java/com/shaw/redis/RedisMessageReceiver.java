package com.shaw.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;

/**
 * Created by shaw on 2017/1/3 0003.
 */
public class RedisMessageReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisMessageReceiver.class);
    public static final String DEFAULT_LISTENEER_METHOD = "receiveMessage";

    private CountDownLatch latch;

    @Autowired
    public RedisMessageReceiver(CountDownLatch latch) {
        this.latch = latch;
    }

    public void receiveMessage(String message) {
        LOGGER.info("Received <" + message + ">");
    }
}
