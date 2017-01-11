package com.shaw.netty;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by shaw on 2017/1/10 0010.
 */
@Component
@ChannelHandler.Sharable
public class SimpleMessageServerHandler extends SimpleChannelInboundHandler<String> {

    public static Logger logger = LoggerFactory.getLogger(SimpleMessageServerHandler.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public static Map<String, Channel> channelMap = new ConcurrentHashMap<String, Channel>();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        System.out.println("getMessage:" + s);
        SocketMessage message = null;
        try {
            message = JSON.parseObject(s, SocketMessage.class);
        } catch (Exception e) {
            logger.warn("Get JsonStr:ParseObject fail" + s);
            return;
        }
        if (message.getType() == 1) {
            if (!StringUtils.isEmpty(message.getAppKey())) {
                logger.info("put channel appkey:" + message.getAppKey());
                channelMap.put(message.getAppKey(), ctx.channel());
            }
        } else {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public class SocketMessage {
        private String appKey;
        private String appSecret;
        private String contents;
        // 1 连接登录请求 2.正常消息请求
        private Integer type;

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public String getContents() {
            return contents;
        }

        public void setContents(String contents) {
            this.contents = contents;
        }
    }
}
