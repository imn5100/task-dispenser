package com.shaw;

import com.shaw.utils.DownloadUtils;
import com.shaw.utils.ThreadPoolManager;

/**
 * Created by Administrator on 2017/1/3 0003.
 */
public class SimpleMainTest {
    public static void main(String[] args) {
        ThreadPoolManager.INSTANCE.execute(getDownloadTask("https://i4.pixiv.net/img-original/img/2015/08/26/19/28/26/52200483_p0.jpg", "D:/"));
        ThreadPoolManager.INSTANCE.execute(getDownloadTask("http://i2.pixiv.net/img-original/img/2016/12/26/12/33/55/60570189_p0.jpg", "D:/"));
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
