package com.shaw;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.Serializable;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TaskExecuterApplicationTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testPop() {
//        TestClass testClass = new TestClass();
//        testClass.setPath("E:/");
//        testClass.setName("title");
//        testClass.setUrl("http://sawb.mne");
//        redisTemplate.opsForSet().add("testSet", testClass);
        System.out.println(redisTemplate.opsForSet().pop("testSet"));
    }
}
