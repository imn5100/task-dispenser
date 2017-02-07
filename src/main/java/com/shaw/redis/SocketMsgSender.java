package com.shaw.redis;

import com.alibaba.fastjson.JSONObject;
import com.shaw.netty.RemoteTaskServerHandler;
import com.shaw.netty.SocketMessage;
import com.shaw.utils.ThreadPoolManager;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Created by shaw on 2017/1/11 0011.
 */
public class SocketMsgSender {
    public static Logger logger = LoggerFactory.getLogger(SocketMsgSender.class);
    public static final String QUIT = "quit";

    public static void handlerRedisMsg(final SocketMessage msg) {
        if (msg == null || StringUtils.isEmpty(msg.getAppKey())) {
            logger.info("msg is null");
            return;
        }
        //异步执行，发送接收到的socket任务消息到 已连接的客户端。
        ThreadPoolManager.INSTANCE.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        io.netty.channel.Channel channel =
                                RemoteTaskServerHandler.channelMap.get(msg.getAppKey());
                        if (channel != null)
                            if (channel.isActive()) {
                                //检查是否是退出消息，如果是这直接发送关闭连接
                                if (checkQuit(msg.getContents())) {
                                    channel.writeAndFlush("An quit command is received.").addListener(ChannelFutureListener.CLOSE);
                                } else {
                                    logger.info("send msg to:" + msg.getAppKey() + " content:" + msg.getContents());
                                    channel.writeAndFlush(msg.getContents());
                                }
                            } else {
                                channel.close();
                            }
                    }
                });
    }

    public static boolean checkQuit(String contents) {
        //内容为空则关闭连接
        if (StringUtils.isEmpty(contents)) {
            return true;
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(contents);
            if (QUIT.equalsIgnoreCase(jsonObject.getString("contents"))) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.warn("parse Json Object fail:" + contents);
            return false;
        }
    }
}
