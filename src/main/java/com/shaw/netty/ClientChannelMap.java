package com.shaw.netty;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.shaw.constants.Constants.QUIT_CMD;
import static com.shaw.constants.Constants.USER_CLIENT_CONNECT;

/**
 * Created by shaw on 2017/2/9 0009.
 * 用于装载 客户端连接的Channel的工具类。
 * 提供基本的获取channel方法，并维护channel。
 */
public class ClientChannelMap {
    public static Logger logger = LoggerFactory.getLogger(ClientChannelMap.class);
    //记录连接channel
    private static Map<String, Channel> map = new ConcurrentHashMap<String, Channel>();
    //记录 连接 客户端对应appKey
    private static BiMap<String, String> appKeySessionIdMap = HashBiMap.create();
    private RedisTemplate redisTemplate;
    public static ClientChannelMap INSTANCE = null;

    public static synchronized ClientChannelMap getInstance(RedisTemplate redisTemplate) {
        if (INSTANCE == null) {
            INSTANCE = new ClientChannelMap();
            INSTANCE.setRedisTemplate(redisTemplate);
        }
        return INSTANCE;
    }

    private ClientChannelMap() {

    }

    public synchronized void put(String appKey, String sessionId, Channel socketChannel) {
        map.put(sessionId, socketChannel);
        appKeySessionIdMap.forcePut(appKey, sessionId);
        redisTemplate.opsForSet().add(USER_CLIENT_CONNECT, appKey);
    }

    public synchronized String remove(Channel socketChannel) {
        if (socketChannel == null) {
            return null;
        }
        for (Map.Entry entry : map.entrySet()) {
            if (entry.getValue() == socketChannel) {
                String appKey = appKeySessionIdMap.inverse().get(entry.getKey());
                appKeySessionIdMap.remove(appKey);
                map.remove(entry.getKey());
                redisTemplate.opsForSet().remove(USER_CLIENT_CONNECT, appKey);
                return appKey;
            }
        }
        return null;
    }

    public synchronized String removeByAppKey(String appKey) {
        if (StringUtils.isEmpty(appKey)) {
            return null;
        }
        String sessionId = appKeySessionIdMap.get(appKey);
        if (sessionId != null) {
            map.remove(sessionId);
            redisTemplate.opsForSet().remove(USER_CLIENT_CONNECT, appKey);
            return appKey;
        }
        return null;
    }

    public synchronized String removeBySessionId(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            return null;
        }
        String appKey = appKeySessionIdMap.inverse().get(sessionId);
        if (appKey != null) {
            map.remove(sessionId);
            redisTemplate.opsForSet().remove(USER_CLIENT_CONNECT, appKey);
            return appKey;
        }
        return null;
    }

    public boolean isConnected(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            return false;
        }
        return map.containsKey(sessionId);
    }

    public Channel getBySessionId(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            return null;
        }
        return map.get(sessionId);
    }

    public Channel getByAppKey(String appKey) {
        if (StringUtils.isEmpty(appKey)) {
            return null;
        }
        String sessionId = appKeySessionIdMap.get(appKey);
        if (!StringUtils.isEmpty(sessionId)) {
            return map.get(sessionId);
        }
        return null;
    }

    public void destroy() {
        redisTemplate.delete(USER_CLIENT_CONNECT);
        for (Channel channel : map.values()) {
            if (channel != null && channel.isActive())
                channel.writeAndFlush(QUIT_CMD).addListener(ChannelFutureListener.CLOSE);
        }
        map.clear();
        appKeySessionIdMap.clear();
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
