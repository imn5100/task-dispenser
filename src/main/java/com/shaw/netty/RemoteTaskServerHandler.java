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
import java.util.UUID;
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
    public static ClientChannelMap clientChannelMap;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BaseMsg baseMsg) throws Exception {
        if (MsgType.AUTH.equals(baseMsg.getType())) {
            AuthMsg message = (AuthMsg) baseMsg;
            if (!StringUtils.isEmpty(message.getAppKey()) && !StringUtils.isEmpty(message.getAppSecret())) {
                String info = redisTemplate.opsForValue().get(String.format(USER_AUTH_KEY, message.getAppKey()));
                //未通过登录验证
                if (info == null) {
                    ctx.writeAndFlush("Please try to connect after login in https://shawblog.me/remoteTask/main.html ").addListener(ChannelFutureListener.CLOSE);
                    return;
                } else {
                    JSONObject infoObject = JSONObject.parseObject(info);
                    String appSecret = infoObject.getString("appsecret");
                    if (message.getAppSecret().equals(appSecret)) {
                        //登录验证通过
                        //当重复连接时，移除上一个连接
                        Channel channel = clientChannelMap.getByAppKey(message.getAppKey());
                        if (channel != null && channel.isActive()) {
                            logger.info("remove old channel. AppKey:" + message.getAppKey());
                            channel.close();
                            clientChannelMap.removeByAppKey(message.getAppKey());
                        }
                        logger.info("put channel. AppKey:" + message.getAppKey());
                        String sessionId = UUID.randomUUID().toString();
                        clientChannelMap.put(sessionId, message.getAppKey(), ctx.channel());
                        ctx.writeAndFlush("{\"success\":\"" + sessionId + "\"");
                    } else {
                        ctx.writeAndFlush("AppKey or AppSecret is wrong").addListener(ChannelFutureListener.CLOSE);
                    }
                }
            } else {
                ctx.writeAndFlush("Params is Wrong").addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            //登录验证
            if (clientChannelMap.isConnected(baseMsg.getSessionId())) {
                ctx.writeAndFlush("Please login in first").addListener(ChannelFutureListener.CLOSE);
                return;
            }
            switch (baseMsg.getType()) {
                case PING: {
                    PingMsg replyPing = new PingMsg();
                    ctx.channel().writeAndFlush(replyPing);
                }
                break;
                case ASK: {
                    //收到客户端的请求
//                    AskMsg askMsg = (AskMsg) baseMsg;
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
        String appKey = removeFromChannelMap(ctx.channel());
        logger.info(appKey + " is Inactive,remove from channelMap");
    }

    private String removeFromChannelMap(Channel channel) {
        return clientChannelMap.remove(channel);
    }

    @PostConstruct
    private void initClientChannelMap() {
        clientChannelMap = ClientChannelMap.getInstance(this.redisTemplate);
    }


}
