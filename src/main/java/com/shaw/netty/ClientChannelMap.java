package com.shaw.netty;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.shaw.constants.Constants.USER_CLIENT_CONNECT;

/**
 * Created by shaw on 2017/2/9 0009.
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
        appKeySessionIdMap.put(appKey, sessionId);
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

    @PreDestroy
    public void destroy() {
        //关闭服务器时，移除所有登录连接
        redisTemplate.delete(USER_CLIENT_CONNECT);
        map.clear();
        appKeySessionIdMap.clear();
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
