package com.shaw.netty.msg;

/**
 * Created by shaw on 2017/2/7 0007.
 * 登录验证消息
 */
public class AuthMsg extends BaseMsg {
    private String appSecret;
    private String appkey;

    public AuthMsg() {
        setType(MsgType.AUTH);
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }
}
