package com.shaw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class TaskExecuterApplication {

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext ctx = SpringApplication.run(TaskExecuterApplication.class, args);
        CountDownLatch latch = ctx.getBean(CountDownLatch.class);
        latch.await();
        System.exit(0);
    }
}
