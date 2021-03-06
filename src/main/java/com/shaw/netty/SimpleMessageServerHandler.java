package com.shaw.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.*;

import static com.shaw.constants.Constants.USER_AUTH_KEY;
import static com.shaw.constants.Constants.USER_CLIENT_CONNECT;

/**
 * Created by shaw on 2017/1/10 0010.
 * 弃用，使用新的RemoteTaskServerHandler
 */
@Component
@ChannelHandler.Sharable
@Deprecated
public class SimpleMessageServerHandler extends SimpleChannelInboundHandler<String> {

    public static Logger logger = LoggerFactory.getLogger(SimpleMessageServerHandler.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    //用于保存连接的channel，通过appkey 获取对应连接，给指定客户端发送数据 如果channel失效，从map中移除。
    public static ConcurrentMap<String, Channel> channelMap = new ConcurrentHashMap<String, Channel>();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        ctx.close().addListener(
                new GenericFutureListener<io.netty.util.concurrent.Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        if (future.isSuccess()) {
                            checkChannelMap();
                        }
                    }
                });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //当有连接断开时，检查非活跃channel，去除
        for (Map.Entry entry : channelMap.entrySet()) {
            if (entry.getValue() == ctx.channel()) {
                channelMap.remove(entry.getKey());
                logger.info(entry.getKey() + " is Inactive,remove from channelMap");
                super.channelInactive(ctx);
                break;
            }
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        checkChannelMap();
        super.handlerRemoved(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        SocketMessage message;
        try {
            if (s.startsWith("{")) {
                message = JSON.parseObject(s, SocketMessage.class);
            } else {
                ctx.writeAndFlush("No JSON object could be decoded")
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }
        } catch (Exception e) {
            logger.warn("Get JsonStr:ParseObject fail" + s);
            ctx.writeAndFlush("No JSON object could be decoded").addListener(ChannelFutureListener.CLOSE);
            return;
        }
        if (message.getType() == 1) {
            if (!StringUtils.isEmpty(message.getAppKey())
                    && !StringUtils.isEmpty(message.getAppSecret())) {
                String info =
                        redisTemplate.opsForValue().get(String.format(USER_AUTH_KEY, message.getAppKey()));
                //未通过登录验证
                if (info == null) {
                    ctx.writeAndFlush(
                            "Please try to connect after login in https://shawblog.me/remoteTask/main.html ")
                            .addListener(ChannelFutureListener.CLOSE);
                    return;
                } else {
                    JSONObject infoObject = JSONObject.parseObject(info);
                    String appSercet = infoObject.getString("appsecret");
                    if (message.getAppSecret().equals(appSercet)) {
                        //登录验证通过
                        //当重复连接时，移除上一个连接
                        Channel channel = channelMap.get(message.getAppKey());
                        if (channel != null && channel.isActive()) {
                            logger.info("remove old channel. appkey:" + message.getAppKey());
                            channel.close();
                        }
                        logger.info("put channel. appkey:" + message.getAppKey());
                        channelMap.put(message.getAppKey(), ctx.channel());
                        redisTemplate.opsForSet().add(USER_CLIENT_CONNECT, message.getAppKey());
                        ctx.writeAndFlush("success");
                    } else {
                        ctx.writeAndFlush("AppKey or AppSecret is wrong")
                                .addListener(ChannelFutureListener.CLOSE);
                    }
                }
            } else {
                ctx.writeAndFlush("Params is Wrong").addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            //对其他类型socket请求暂时不做处理，直接忽视，关闭channel
            ctx.close();
        }
    }

    //当连接数开始增加到一定数量时，需要考虑移除的性能问题
    public void checkChannelMap() {
        for (String key : channelMap.keySet()) {
            final Channel channel = channelMap.get(key);
            if (channel != null) {
                if (!channel.isActive()) {
                    channel.close();
                    SimpleMessageServerHandler.channelMap.remove(key);
                    redisTemplate.opsForSet().remove(USER_CLIENT_CONNECT, key);
                    logger.info(key + " is disconnect,Remove from channelMap");
                }
            } else {
                SimpleMessageServerHandler.channelMap.remove(key);
                redisTemplate.opsForSet().remove(USER_CLIENT_CONNECT, key);
                logger.info(key + " is disconnect,Remove from channelMap");
            }
        }
    }

    @PostConstruct
    public void runScheduler() {
        //当SimpleMessageServerHandler 被创建时，开启一个监视channelMap的定时执行器。
        //定时检查channel,如果channel非活跃的 关闭channel，并从map中移除
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        //每20分钟检查一次所有连接（和博客主站登录session时效相同）
        scheduler.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        checkChannelMap();
                    }
                },
                20,
                20,
                TimeUnit.MINUTES);
    }
}
