package com.shaw.netty;

import com.alibaba.fastjson.JSONObject;
import com.shaw.netty.msg.*;
import io.netty.channel.*;
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
 * Created by shaw on 2017/2/7 0007.
 */
@Component
@ChannelHandler.Sharable
public class RemoteTaskServerHandler extends SimpleChannelInboundHandler<BaseMsg> {
    public static Logger logger = LoggerFactory.getLogger(RemoteTaskServerHandler.class);
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    //用于保存连接的channel，通过appkey 获取对应连接，给指定客户端发送数据 如果channel失效，从map中移除。
    public static ConcurrentMap<String, Channel> channelMap = new ConcurrentHashMap<String, Channel>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BaseMsg baseMsg) throws Exception {
        if (MsgType.AUTH.equals(baseMsg.getType())) {
            AuthMsg message = (AuthMsg) baseMsg;
            if (!StringUtils.isEmpty(message.getAppkey()) && !StringUtils.isEmpty(message.getAppsecret())) {
                String info = redisTemplate.opsForValue().get(String.format(USER_AUTH_KEY, message.getAppkey()));
                //未通过登录验证
                if (info == null) {
                    ctx.writeAndFlush("Please try to connect after login in https://shawblog.me/remoteTask/main.html ").addListener(ChannelFutureListener.CLOSE);
                    return;
                } else {
                    JSONObject infoObject = JSONObject.parseObject(info);
                    String appSercet = infoObject.getString("appsecret");
                    if (message.getAppsecret().equals(appSercet)) {
                        //登录验证通过
                        //当重复连接时，移除上一个连接
                        Channel channel = channelMap.get(message.getAppkey());
                        if (channel != null && channel.isActive()) {
                            logger.info("remove old channel. appkey:" + message.getAppkey());
                            channel.close();
                        }
                        logger.info("put channel. appkey:" + message.getAppkey());
                        channelMap.put(message.getAppkey(), ctx.channel());
                        redisTemplate.opsForSet().add(USER_CLIENT_CONNECT, message.getAppkey());
                        ctx.writeAndFlush("success");
                    } else {
                        ctx.writeAndFlush("AppKey or AppSecret is wrong").addListener(ChannelFutureListener.CLOSE);
                    }
                }
            } else {
                ctx.writeAndFlush("Params is Wrong").addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            switch (baseMsg.getType()) {
                case PING: {
//                    登录后才接收心跳连接
                    if (channelMap.containsKey(baseMsg.getAppkey())) {
                        PingMsg replyPing = new PingMsg();
                        ctx.channel().writeAndFlush(replyPing);
                    } else {
                        ctx.writeAndFlush("Please login first").addListener(ChannelFutureListener.CLOSE);
                    }
                }
                break;
                case ASK: {
                    //收到客户端的请求
                    AskMsg askMsg = (AskMsg) baseMsg;
                    ctx.close();
                }
                break;
                default:
                    ctx.close();
                    break;
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Handler exceptionCaught", cause);
        removeFromChannelMap(ctx.channel());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //当有连接断开时，检查非活跃channel，去除
        removeFromChannelMap(ctx.channel());
    }

    private void removeFromChannelMap(Channel channel) {
        for (Map.Entry entry : channelMap.entrySet()) {
            if (entry.getValue() == channel) {
                channelMap.remove(entry.getKey());
                redisTemplate.opsForSet().remove(USER_CLIENT_CONNECT, entry.getKey());
                logger.info(entry.getKey() + " is Inactive,remove from channelMap");
                break;
            }
        }
    }

    @PostConstruct
    private void runScheduler() {
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
                }, 20, 20, TimeUnit.MINUTES);
    }

    //当连接数开始增加到一定数量时，需要考虑移除的性能问题
    private void checkChannelMap() {
        for (String key : channelMap.keySet()) {
            final Channel channel = channelMap.get(key);
            if (channel != null) {
                if (!channel.isActive()) {
                    channel.close();
                    channelMap.remove(key);
                    redisTemplate.opsForSet().remove(USER_CLIENT_CONNECT, key);
                    logger.info(key + " is disconnect,Remove from channelMap");
                }
            } else {
                channelMap.remove(key);
                redisTemplate.opsForSet().remove(USER_CLIENT_CONNECT, key);
                logger.info(key + " is disconnect,Remove from channelMap");
            }
        }
    }

}
