package com.shaw;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.shaw.constants.Constants;
import com.shaw.utils.DownloadUtils;
import com.shaw.utils.ThreadPoolManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TaskExecuterApplicationTests {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Test
    public void testTemplate() {
        redisTemplate.opsForValue().set("test", "test");
        redisTemplate.expire("test", 1000L, TimeUnit.MINUTES);
        System.out.println(redisTemplate.opsForValue().get("test"));
    }

    /**
     * 模拟socket登录验证
     */
    @Test
    public void pubAuthMsg() {
        String key = String.format(Constants.USER_AUTH_KEY, "76d44f617b70c97e118c81dc579d9aa5");
        redisTemplate.opsForValue().set(key, "{\"appSecret\":\"a0de68bc6581303c202c3cb57567878a\",\"appkey\":\"76d44f617b70c97e118c81dc579d9aa5\",\"name\":\"shaw\"}");
        redisTemplate.expire(key, 1000L, TimeUnit.SECONDS);
        System.out.println(redisTemplate.opsForValue().get(key));
    }


    //  测试使用线程执行器，同时下载两幅插画
    public void testDownload() throws InterruptedException, ExecutionException, TimeoutException {
        ListenableFuture<String> result = ThreadPoolManager.INSTANCE.addExecuteTask(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return DownloadUtils.downloadByUrlAndSavePath("http://i2.pixiv.net/img-original/img/2016/12/26/12/33/55/60570189_p0.jpg", "D:/");
            }
        });
        ListenableFuture<String> result2 = ThreadPoolManager.INSTANCE.addExecuteTask(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return DownloadUtils.downloadByUrlAndSavePath("https://i4.pixiv.net/img-original/img/2015/08/26/19/28/26/52200483_p0.jpg", "D:/");
            }
        });
//        使用get方法是，当前线程发送阻塞。直到取到结果。
        ListenableFuture<List<String>> allFutures = Futures.successfulAsList(result, result2);
        //  最大阻塞5分钟
        allFutures.get();
    }
}
