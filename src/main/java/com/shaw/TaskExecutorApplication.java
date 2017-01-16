package com.shaw;

import com.shaw.netty.SocketMsgServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class TaskExecutorApplication {

    private static Logger logger = LoggerFactory.getLogger(TaskExecutorApplication.class);

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext ctx = SpringApplication.run(TaskExecutorApplication.class, args);
        SocketMsgServer socketMsgServer = ctx.getBean(SocketMsgServer.class);
        //启动消息发送socket服务器
        logger.info("run scoket server");
        socketMsgServer.run();
        logger.info("run scoket server over");
        //让主进程Application 等待子进程RedisMessageListener 退出
        System.exit(0);
    }
}
