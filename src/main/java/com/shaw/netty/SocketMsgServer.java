package com.shaw.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

/**
 * Created by Administrator on 2017/1/11 0011.
 */
public class SocketMsgServer {

    private Integer port;
    private ChannelHandler handler;

    public SocketMsgServer(int port, ChannelHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    public void run() throws InterruptedException {
        //用于接收连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //用于处理连接
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)//指定接收连接的形式  Socket nio
                    .childHandler(
                            //用来处理一个最近的已经接收的 Channel
                            new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) throws Exception {
                                    //增加多个的处理类到 ChannelPipeline 上
                                    ChannelPipeline pipeline = ch.pipeline();
//                                          pipeline.addLast("framer",new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                                    //数据编码解码直接使用自定义Json 解码
                                    pipeline.addLast("decoder", new JsonMsgDecoder());
                                    pipeline.addLast("encoder", new StringEncoder());
                                    pipeline.addLast("handler", handler);
                                }
                            })
                    .option(ChannelOption.SO_BACKLOG, 128)//设置 socket 的参数选项比如tcpNoDelay 和 keepAliv
                    .option(ChannelOption.TCP_NODELAY, true) //通过NoDelay禁用Nagle,使消息立即发出去
                    .childOption(ChannelOption.SO_KEEPALIVE, true);//设置保持长连接
            ChannelFuture f = bootstrap.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
