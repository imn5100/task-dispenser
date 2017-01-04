package com.shaw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class TaskExecutorApplication {

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext ctx = SpringApplication.run(TaskExecutorApplication.class, args);
        CountDownLatch latch = ctx.getBean(CountDownLatch.class);
        //让主进程Application 等待子进程RedisMessageListener 退出
        latch.await();
        System.exit(0);
    }
}
