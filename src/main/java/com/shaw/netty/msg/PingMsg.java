package com.shaw.netty.msg;

/**
 * Created by shaw on 2017/2/7 0007.
 * 心跳检查ping 类型消息
 */
public class PingMsg extends BaseMsg {
    public PingMsg() {
        setType(MsgType.PING);
    }
}
