package com.shaw;

import com.sun.org.apache.bcel.internal.generic.Select;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.util.CharsetUtil;
import org.springframework.expression.spel.ast.Selection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

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

class PlainNioServer {

    /**
     * java 实现阻塞的 socket 连接
     */
    public void oioServe(int port) throws IOException {
        final ServerSocket socket = new ServerSocket(port);
        try {
            for (; ; ) {
                final Socket clientSocket = socket.accept();
                System.out.println("Accepted connection from " + clientSocket);
                //每接收到一个连接请求，创建一个线程，向客户端连接写入数据
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        OutputStream out;
                        try {
                            out = clientSocket.getOutputStream();
                            out.write("Hi\r\n".getBytes());
                            out.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                //ignore
                            }
                        }
                    }
                });
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


    /**
     * Java NIO 实现非阻塞的Socket连接
     */
    public void nioServe(int port) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        ServerSocket sscoket = serverChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        //创建一个非阻塞的SocketServer 并绑定地址 获取channel
        sscoket.bind(address);
        //打卡selector处理channel
        Selector selector = Selector.open();
        //注册selector用于接收新的连接，当有连接是，可通过selector获取连接channel
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        final ByteBuffer msg = ByteBuffer.wrap("Hi\r\n".getBytes());
        while (true) {
            try {
                //阻塞等待事件来临
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            //获取所有事件的selectionKey
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                try {
                    //事件是否是一个等待接收的连接请求
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        //获取客户端连接
                        java.nio.channels.SocketChannel client = server.accept();
                        //客户端连接为非阻塞的
                        client.configureBlocking(false);
                        //接受新的客户端 注册到selector中
                        client.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, msg.duplicate());
                        System.out.println("Accepted connection from " + client);
                    }
                    //查看socket当前是否可写入数据
                    if (key.isWritable()) {
                        java.nio.channels.SocketChannel client = (java.nio.channels.SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        //写入数据到客户端
                        while (buffer.hasRemaining()) {
                            if (client.write(buffer) == 0) {
                                break;
                            }
                        }
                        //关闭连接
                        client.close();
                    }
                } catch (IOException e) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException ex) {
                        //ignore
                    }
                }
            }
        }
    }
}

class NettyServer {
    public void oioServer(int port) throws IOException, InterruptedException {
        final ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hi\t\n", Charset.forName("UTF-8")));
        EventLoopGroup group = new OioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            //创建一个ServerBootstorp 绑定  oio  group，绑定 oio  scoketchannel，绑定本地端口，注册handler
            b.group(group).channel(OioServerSocketChannel.class).localAddress(new InetSocketAddress(port)).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        //ChannelHandlerContext handler group 处理过程中的上下文
                        public void channelActive(ChannelHandlerContext ctx) {
                            //写数据客户端中，一旦完成关闭连接
                            ctx.writeAndFlush(buf.duplicate()).addListener(ChannelFutureListener.CLOSE);
                        }
                    });
                }
            });
            //绑定服务器接收新连接
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public void nioServer(int port) throws IOException, InterruptedException {
        final ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hi\t\n", Charset.forName("UTF-8")));
        //创建一个nio的 事件处理group
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            //创建一个ServerBootstorp 绑定  nio  group，绑定 nio  scoketchannel，绑定本地端口，注册handler
            b.group(group).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress(port)).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        //ChannelHandlerContext handler group 处理过程中的上下文
                        public void channelActive(ChannelHandlerContext ctx) {
                            //写数据客户端中，异步完成添加监听器，一旦完成关闭连接
                            ctx.writeAndFlush(buf.duplicate()).addListener(ChannelFutureListener.CLOSE);
                        }
                    });
                }
            });
            //绑定服务器接收新连接
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}