package com.shaw.redis;

import com.alibaba.fastjson.JSONObject;
import com.shaw.netty.RemoteTaskServerHandler;
import com.shaw.netty.SocketMessage;
import com.shaw.utils.ThreadPoolManager;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import static com.shaw.constants.Constants.QUIT_CMD;

/**
 * Created by shaw on 2017/1/11 0011.
 * 用于解析redis推送消息，并将消息推送给指定的socket client。
 */
public class SocketMsgSender {
    public static Logger logger = LoggerFactory.getLogger(SocketMsgSender.class);

    public static void handlerRedisMsg(final SocketMessage msg) {
        if (msg == null || StringUtils.isEmpty(msg.getAppKey())) {
            logger.info("msg is null");
            return;
        }
        //异步执行，发送接收到的socket任务消息到指定的客户端。
        ThreadPoolManager.INSTANCE.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        io.netty.channel.Channel channel =
                                RemoteTaskServerHandler.clientChannelMap.getByAppKey(msg.getAppKey());
                        if (channel != null)
                            if (channel.isActive()) {
                                //检查是否是退出消息，如果是这直接发送关闭连接
                                if (checkQuit(msg.getContents())) {
                                    channel.writeAndFlush(QUIT_CMD).addListener(ChannelFutureListener.CLOSE);
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

    /**
     * 检查消息内容是否为退出，如果是退出命令，socket服务端要主动关闭channel连接
     */
    public static boolean checkQuit(String contents) {
        //内容为空则关闭连接
        if (StringUtils.isEmpty(contents)) {
            return true;
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(contents);
            if (QUIT_CMD.equalsIgnoreCase(jsonObject.getString("contents"))) {
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
