package com.shaw.netty.msg;

/**
 * Created by shaw on 2017/2/7 0007.
 * 请求类型消息
 */
public class AskMsg extends BaseMsg {
    public AskMsg() {
        setType(MsgType.ASK);
    }

    //请求消息内容
    private String contents;
    private String toAppKey;

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public String getToAppKey() {
        return toAppKey;
    }

    public void setToAppKey(String toAppKey) {
        this.toAppKey = toAppKey;
    }
}
