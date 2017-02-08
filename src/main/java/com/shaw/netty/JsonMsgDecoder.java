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
        // 直接将接收到的数据编码为 字符串。
        String s = msg.toString(Charset.defaultCharset());
        //如果接受到的消息格式不对直接关闭连接。
        try {
            if (s.startsWith("{")) {
                BaseMsg message = JSON.parseObject(s, BaseMsg.class);
                switch (message.getType()) {
                    case PING:
                        message = JSON.parseObject(s, PingMsg.class);
                        break;
                    case AUTH:
                        message = JSON.parseObject(s, AuthMsg.class);
                        break;
                    case ASK:
                        message = JSON.parseObject(s, AskMsg.class);
                        break;
                }
                out.add(message);
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
    }
}
