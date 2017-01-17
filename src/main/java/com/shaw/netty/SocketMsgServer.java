package com.shaw.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/** Created by Administrator on 2017/1/11 0011. */
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
      ServerBootstrap bootstrap =
          new ServerBootstrap()
              .group(bossGroup, workerGroup)
              .
              //指定接收连接的形式  Socket nio
              channel(NioServerSocketChannel.class)
              .
              //用来处理一个最近的已经接收的 Channel
              childHandler(
                  new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                      //                            增加多个的处理类到 ChannelPipeline 上
                      ChannelPipeline pipeline = ch.pipeline();
                      pipeline.addLast(
                          "framer",
                          new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                      pipeline.addLast("decoder", new StringDecoder());
                      pipeline.addLast("encoder", new StringEncoder());
                      pipeline.addLast("handler", handler);
                    }
                  })
              .
              //设置 socket 的参数选项比如tcpNoDelay 和 keepAliv
              option(ChannelOption.SO_BACKLOG, 128)
              .childOption(ChannelOption.SO_KEEPALIVE, true);
      ChannelFuture f = bootstrap.bind(port).sync();
      f.channel().closeFuture().sync();
    } finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    }
  }
}
