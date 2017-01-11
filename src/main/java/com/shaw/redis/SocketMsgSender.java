package com.shaw.redis;

import com.shaw.netty.SimpleMessageServerHandler;
import com.shaw.utils.ThreadPoolManager;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;


/**
 * Created by shaw on 2017/1/11 0011.
 */
public class SocketMsgSender {
    public static Logger logger = LoggerFactory.getLogger(SocketMsgSender.class);

    public static void handlerRedisMsg(final SimpleMessageServerHandler.SocketMessage msg) {
        if (msg == null || StringUtils.isEmpty(msg.getAppKey())) {
            logger.info("msg is null");
            return;
        }
        ThreadPoolManager.INSTANCE.execute(new Runnable() {
            @Override
            public void run() {
                io.netty.channel.Channel channel = SimpleMessageServerHandler.channelMap.get(msg.getAppKey());
                if (channel != null)
                    if (channel.isActive()) {
                        logger.info("send msg to:" + msg.getAppKey() + " content:" + msg.getContents());
                        channel.writeAndFlush(msg.getContents() + "\n");
                    } else {
                        channel.close().addListener(new GenericFutureListener<Future<? super Void>>() {
                            @Override
                            public void operationComplete(Future<? super Void> future) throws Exception {
                                if (future.isSuccess()) {
                                    logger.info("close channel success,appkey:" + msg.getAppKey());
                                } else {
                                    logger.info("close channel fail,appkey:" + msg.getAppKey());
                                }
                            }
                        });
                        logger.info(msg.getAppKey() + " is disconnect.remove channel");
                        SimpleMessageServerHandler.channelMap.remove(msg.getAppKey());
                    }
            }
        });
    }
}
