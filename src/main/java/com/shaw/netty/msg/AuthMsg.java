package com.shaw.netty.msg;

/**
 * Created by shaw on 2017/2/7 0007.
 * 登录验证消息
 */
public class AuthMsg extends BaseMsg {
    private String appSecret;
    private String appKey;

    public AuthMsg() {
        setType(MsgType.AUTH);
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }
}
