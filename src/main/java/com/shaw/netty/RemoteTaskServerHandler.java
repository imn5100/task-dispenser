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
import javax.annotation.PreDestroy;
import java.util.UUID;

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
    //用于保存连接的channel，通过appKey,sessionId 获取对应连接，给指定客户端发送数据。
    public static ClientChannelMap clientChannelMap;

    /**
     * 数据接收处理逻辑。
     * 传入的对象由JsonMsgDecoder 先进行解码，这里直接使用对象。
     * 目前消息种类分3中Auth 登录消息 唯一不需要sessionId 。PING 心跳测试,ASK 普通数据请求 暂时不做处理,只把请求数据返回
     */
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
                    String appSecret = infoObject.getString("appSecret");
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
                        clientChannelMap.put(message.getAppKey(), sessionId, ctx.channel());
                        ctx.writeAndFlush("{\"success\":\"" + sessionId + "\"}");
                    } else {
                        ctx.writeAndFlush("AppKey or AppSecret is wrong").addListener(ChannelFutureListener.CLOSE);
                    }
                }
            } else {
                ctx.writeAndFlush("Params is Wrong").addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            //登录验证
            if (!clientChannelMap.isConnected(baseMsg.getSessionId())) {
                ctx.writeAndFlush("Please login in first").addListener(ChannelFutureListener.CLOSE);
                return;
            }
            switch (baseMsg.getType()) {
                case PING: {
                    //接收到心跳，返回心跳
                    PingMsg replyPing = new PingMsg();
                    ctx.channel().writeAndFlush(replyPing);
                }
                break;
                case ASK: {
                    //收到客户端的请求  暂时只做echo处理
                    AskMsg askMsg = (AskMsg) baseMsg;
                    String toAppKey = askMsg.getToAppKey();
                    Channel toChannel = clientChannelMap.getByAppKey(toAppKey);
                    if (toChannel == null || !toChannel.isActive()) {
                        ctx.writeAndFlush("Each other offline。Unable to receive message");
                    } else {
                        String fromAppKey = clientChannelMap.getAppKeyBySession(askMsg.getSessionId());
                        //TODO 通过appKey查询昵称，避免appKey辨识度不足问题
                        //TODO 这里需要设计一个 Username,和appKey 一一对应，appKey用于服务端辨识用户，username用于用户相互辨识。
                        toChannel.writeAndFlush("Get Message From " + fromAppKey + ":" + askMsg.getContents());
                    }
                }
                break;
                default:
                    //session验证通过，但消息属于未解析类型消息，直接关闭连接。
                    ctx.writeAndFlush("UnKnow Message Type").addListener(ChannelFutureListener.CLOSE);
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
        // 当channel是登录状态时退出的，可以得到appKey信息,才记录退出日志，否则无法得到，不记录日志
        if (appKey != null)
            logger.info(appKey + " is Inactive,remove from channelMap");
    }

    private String removeFromChannelMap(Channel channel) {
        return clientChannelMap.remove(channel);
    }

    @PostConstruct
    private void initClientChannelMap() {
        clientChannelMap = ClientChannelMap.getInstance(this.redisTemplate);
        //启动时清除所有未清除的垃圾连接信息
        redisTemplate.delete(USER_CLIENT_CONNECT);
    }

    @PreDestroy
    //改注解 需要托管给spring才能正常执行析构方法
    public void destroy() {
        //关闭服务器时，移除所有登录连接
        clientChannelMap.destroy();
    }

}
