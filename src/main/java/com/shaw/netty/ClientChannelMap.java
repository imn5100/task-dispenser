package com.shaw.netty;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
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
    //记录连接channel
    private static Map<String, SocketChannel> map = new ConcurrentHashMap<String, SocketChannel>();
    //记录 连接 客户端对应appKey
    private static BiMap<String, String> sessionIdAppKeyMap = HashBiMap.create();
    private RedisTemplate redisTemplate;
    public static ClientChannelMap INSTANCE = null;

    public synchronized ClientChannelMap getInstance(RedisTemplate redisTemplate) {
        if (INSTANCE == null) {
            INSTANCE = new ClientChannelMap();
            INSTANCE.setRedisTemplate(redisTemplate);
        }
        return INSTANCE;
    }

    public synchronized void add(String appKey, String sessionId, SocketChannel socketChannel) {
        map.put(sessionId, socketChannel);
        sessionIdAppKeyMap.put(appKey, sessionId);
        redisTemplate.opsForSet().add(USER_CLIENT_CONNECT, appKey);
    }

    public synchronized void remove(SocketChannel socketChannel) {
        for (Map.Entry entry : map.entrySet()) {
            if (entry.getValue() == socketChannel) {
                String appKey = sessionIdAppKeyMap.inverse().get(entry.getKey());
                sessionIdAppKeyMap.remove(appKey);
                map.remove(entry.getKey());
                redisTemplate.opsForSet().remove(USER_CLIENT_CONNECT, appKey);
                return;
            }
        }
    }

    public boolean isConnected(String sessionId) {
        return map.containsKey(sessionId);
    }

    public Channel getBySessionId(String sessionId) {
        return map.get(sessionId);
    }

    public Channel getByAppkey(String appKey) {
        String sessionId = sessionIdAppKeyMap.get(appKey);
        if (!StringUtils.isEmpty(sessionId)) {
            return map.get(sessionId);
        }
        return null;
    }


//    public static final String getSessionId(Channel channel) {
////        return UUID.nameUUIDFromBytes(channel.hashCode()).toString();
//        return null;
//    }

    @PreDestroy
    public void destroy() {
        //关闭服务器时，移除所有登录连接
        redisTemplate.delete(USER_CLIENT_CONNECT);
        map.clear();
        sessionIdAppKeyMap.clear();
    }

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
