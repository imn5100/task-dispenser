package com.shaw.netty.msg;

/**
 * Created by shaw on 2017/2/7 0007.
 * 登录验证消息
 */
public class AuthMsg extends BaseMsg {
    private String appsecret;

    public AuthMsg() {
        setType(MsgType.AUTH);
    }

    public String getAppsecret() {
        return appsecret;
    }

    public void setAppsecret(String appsecret) {
        this.appsecret = appsecret;
    }
}
