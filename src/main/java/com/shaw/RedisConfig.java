package com.shaw;

import com.shaw.redis.RedisMessageReceiver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.concurrent.CountDownLatch;

@Configuration
public class RedisConfig {

    @Value("${redis.channel.download}")
    private String channel;

    @Bean
    CountDownLatch latch() {
        return new CountDownLatch(1);
    }

    /**
     * 操作模板
     */
    @Bean
    StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * redis消息接收/消费类
     */
    @Bean
    RedisMessageReceiver receiver(CountDownLatch latch) {
        return new RedisMessageReceiver(latch);
    }

    /**
     * 消息消费适配器
     */
    @Bean
    MessageListenerAdapter listenerAdapter(RedisMessageReceiver receiver) {
        return new MessageListenerAdapter(receiver, RedisMessageReceiver.DEFAULT_LISTENEER_METHOD);
    }

    /**
     * 消息消费
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic(channel));
        return container;
    }


}
