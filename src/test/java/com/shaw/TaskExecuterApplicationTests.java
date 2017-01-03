package com.shaw;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.shaw.bo.RedisMessage;
import com.shaw.utils.DownloadUtils;
import com.shaw.utils.ThreadPoolManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TaskExecuterApplicationTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testPop() {
        RedisMessage<TestClass> redisMessage = new RedisMessage<>();
        TestClass testClass = new TestClass();
        testClass.setPath("E:/");
        testClass.setName("title");
        testClass.setUrl("http://sawb.mne");
        redisMessage.setData(testClass);
        redisMessage.setExtendedInfo("test2");
        redisMessage.setTopic("Pixiv_Download");
        System.out.println(redisTemplate.opsForSet().pop("testSet"));


        final String channel = "testChannel";
        redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.publish("PUBLISH".getBytes(), channel.getBytes());

            }
        });
    }

    //  测试使用线程执行器，同时下载两幅插画
    @Test
    public void testDownload() throws InterruptedException, ExecutionException, TimeoutException {
        ListenableFuture<String> result = ThreadPoolManager.INSTANCE.addExecuteTask(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return DownloadUtils.downloadByUrl("http://i2.pixiv.net/img-original/img/2016/12/26/12/33/55/60570189_p0.jpg", "D:/");
            }
        });
        ListenableFuture<String> result2 = ThreadPoolManager.INSTANCE.addExecuteTask(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return DownloadUtils.downloadByUrl("https://i4.pixiv.net/img-original/img/2015/08/26/19/28/26/52200483_p0.jpg", "D:/");
            }
        });
//        使用get方法是，当前线程发送阻塞。直到取到结果。
        ListenableFuture<List<String>> allFutures = Futures.successfulAsList(result, result2);
        //  最大阻塞5分钟
        allFutures.get();
    }
}
