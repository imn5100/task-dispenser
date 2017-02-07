package com.shaw;

import com.shaw.netty.NettyServerHandler;
import com.shaw.netty.SimpleMessageServerHandler;
import com.shaw.netty.SocketMsgServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by shaw on 2017/1/3 0003.
 */
@Configuration
public class ConfigBean {

    @Value("${scoket.port}")
    private Integer port;

    @Bean
    public SocketMsgServer socketMsgServer(NettyServerHandler handler) {
        return new SocketMsgServer(port, handler);
    }
}
