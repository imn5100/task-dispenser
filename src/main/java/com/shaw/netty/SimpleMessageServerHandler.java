package com.shaw.netty;

import com.alibaba.fastjson.JSON;
import com.shaw.utils.ThreadPoolManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by shaw on 2017/1/10 0010.
 */
public class SimpleMessageServerHandler extends SimpleChannelInboundHandler<String> {

    public static Map<String, Channel> channelMap = new ConcurrentHashMap<String, Channel>();
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        Message message = JSON.parseObject(s, Message.class);
        if (message.getType() == 1) {
            if (!StringUtils.isEmpty(message.getAppKey())) {
                channelMap.put(message.getAppKey(), ctx.channel());
            }
        } else {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static void main(String[] args) {
        //用于接收连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //用于处理连接
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup, workerGroup).
                    //指定接收连接的形式  Socket nio
                            channel(NioServerSocketChannel.class).
                    //用来处理一个最近的已经接收的 Channel
                            childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
//                            增加多个的处理类到 ChannelPipeline 上
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                            pipeline.addLast("decoder", new StringDecoder());
                            pipeline.addLast("encoder", new StringEncoder());
                            pipeline.addLast("handler", new SimpleMessageServerHandler());

                        }
                    }).
                    //设置 socket 的参数选项比如tcpNoDelay 和 keepAliv
                            option(ChannelOption.SO_BACKLOG, 128).
                            childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = bootstrap.bind(8001).sync();
            ThreadPoolManager.INSTANCE.execute(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("execute!");
                        Channel channel = SimpleMessageServerHandler.channelMap.get("123456");
                        if (channel != null) {
                            System.out.println("execute write!");
                            if (channel.isActive())
                                channel.writeAndFlush("test\n");
                            else {
                                System.out.println("remove");
                                SimpleMessageServerHandler.channelMap.remove("123456");
                            }
                        }
                    }
                }
            });
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    static class Message {
        private String appKey;
        private String appSecret;
        // 1 连接登录请求 2.正常消息请求
        private Integer type;
        private String contents;

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
