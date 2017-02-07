package com.shaw.netty;

import com.alibaba.fastjson.JSON;
import com.shaw.netty.msg.AskMsg;
import com.shaw.netty.msg.AuthMsg;
import com.shaw.netty.msg.BaseMsg;
import com.shaw.netty.msg.PingMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by shaw on 2017/2/7 0007.
 */
@ChannelHandler.Sharable
public class JsonMsgDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        String s = msg.toString(Charset.defaultCharset());
        BaseMsg message = null;
        try {
            if (s.startsWith("{")) {
                message = JSON.parseObject(s, BaseMsg.class);
                switch (message.getType()) {
                    case ASK:
                        message = JSON.parseObject(s, AskMsg.class);
                        break;
                    case AUTH:
                        message = JSON.parseObject(s, AuthMsg.class);
                        break;
                    case PING:
                        message = JSON.parseObject(s, PingMsg.class);
                        break;
                }
            } else {
                ctx.channel().writeAndFlush("No JSON object could be decoded")
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }
        } catch (Exception e) {
            ctx.channel().writeAndFlush("No JSON object could be decoded")
                    .addListener(ChannelFutureListener.CLOSE);
            return;
        }
        out.add(message);
    }
}
