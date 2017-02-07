package com.shaw.netty.msg;

import java.io.Serializable;

/**
 * Created by shaw on 2017/2/7 0007.
 */
public class BaseMsg implements Serializable {
    //消息类型  默认为ping消息 不做任何操作
    private MsgType type = MsgType.PING;
    //消息clientId 标示消息来源，这里直接使用appkey
    private String appkey;


    public BaseMsg() {
    }

    public MsgType getType() {
        return type;
    }

    public void setType(MsgType type) {
        this.type = type;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }
}
