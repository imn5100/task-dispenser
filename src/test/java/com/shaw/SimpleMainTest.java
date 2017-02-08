package com.shaw;

import com.shaw.utils.DownloadUtils;
import com.shaw.utils.ThreadPoolManager;

import java.util.UUID;

/**
 * Created by Administrator on 2017/1/3 0003.
 */
public class SimpleMainTest {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(UUID.randomUUID().toString());
            System.out.println(UUID.nameUUIDFromBytes("appkey".getBytes()).toString());
        }
    }

    public static Runnable getDownloadTask(final String url, final String savePath) {
        return new Runnable() {
            @Override
            public void run() {
                DownloadUtils.downloadByUrlAndSavePath(url, savePath);
            }
        };
    }

}
