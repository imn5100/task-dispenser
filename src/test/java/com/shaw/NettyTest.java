package com.shaw;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * Created by shaw on 2017/1/5 0005.
 */
public class NettyTest {
    public static void main(String[] args) throws Exception {

//服务端启动
        int port = 9099;
        new EchoService(port).start();
//客户端启动
        String host = "127.0.0.1";
        new EchoClient(host, port).start();

    }
}


////////////////////////////////////////服务器代码

/**
 * 可被多个Channel安全共享的Handler
 * ChannelHandlers被不同类型的events调用
 * 应用程序通过实现或者扩展ChannelHandlers来钩挂到event的生命周期，并且提供定制的应用逻辑
 */
@ChannelHandler.Sharable
class EchoServiceHandler extends ChannelInboundHandlerAdapter {

    /**
     * 接收到channel的消息时，发出原消息。
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        System.out.println(in.toString(CharsetUtil.UTF_8));
        //而write()操作是异步的，直到channelRead()返回可能都没有完成
        //暂时不能释放in
        ctx.write(in);
    }


    /**
     * 读取完毕，刷新挂起的数据远端后关闭channel
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        //在writeAndFlush()被调用时，这个消息的内存引用会在EchoServerHandler的channelReadComplete()方法中被释放
    }


    /**
     * 跟踪到异常信息，打印异常信息栈，关闭channel
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}

class EchoService {
    private final int port;

    public EchoService(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        //创建handler
        final EchoServiceHandler serviceHandler = new EchoServiceHandler();
        //创建事件循环组，用于处理event
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建服务启动器
            ServerBootstrap b = new ServerBootstrap();
            //服务启动器指定NIO传输（NioServerSocketChannel为channel类型），指定socket端口，注册channelHandler
            b.group(group).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress((port
            ))).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //channelPipeline 中加入EchoServiceHandler
                    // EchoServiceHandler 注解了@Shareable ,可一直使用一个实例
                    ch.pipeline().addLast(serviceHandler);
                }
            });
            //异步绑定服务器 调用sync()让当前线程一直阻塞 等待绑定成功
            ChannelFuture f = b.bind().sync();
            //获得channel的closeFuture，阻塞当前线程到关闭操作完成。
            f.channel().closeFuture().sync();
        } finally {
            //关闭group，释放资源
            group.shutdownGracefully().sync();
        }

    }
}


///////////////////////////////////客户端代码

@ChannelHandler.Sharable
class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {


    /**
     * 和服务器的连接建立起来后被调用
     * 连接成功后发送一条消息
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rock!", CharsetUtil.UTF_8));
    }


    /**
     * 从服务器收到一条消息时被调用
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //直接打印出接收的信息
        System.out.println("Client received:" + msg.toString(CharsetUtil.UTF_8));
        //msg已无用 可以让 SimpleChannelInboundHandler 释放消息内存
        //所以EchoServiceHandler继承ChannelInboundHandlerAdapter 而 EchoClientHandler 继承SimpleChannelInboundHandler
    }


    /**
     * 处理过程中异常发生时被调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //打印异常信息，关闭 channel
        cause.printStackTrace();
        ctx.close();
    }
}

class EchoClient {
    private final String host;
    private final int port;

    EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建Bootstrap
            Bootstrap b = new Bootstrap();
            //指定EventLoopGroup的Nio实现完成客户端事件处理
            //绑定地址ip
            //使用channelInitializer 将echoClientHandler 加入 channel的pipeline1中
            b.group(group).channel(NioSocketChannel.class).remoteAddress(new InetSocketAddress(host, port)).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new EchoClientHandler());
                }
            });
//            所有东西都创建完毕后，调用Bootstrap.connet()连接到远端。
            ChannelFuture f = b.connect().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}

