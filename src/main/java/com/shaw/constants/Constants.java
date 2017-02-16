package com.shaw.constants;

/**
 * Created by shaw on 2017/1/12 0012.
 */
public class Constants {
    //接收下载消息
    public static final String TOPIC_DOWNLOAD = "download";
    //接收socket推送任务消息
    public static final String TOPIC_TASK = "socketTask";
    //web用户登录 key
    public static final String USER_AUTH_KEY = "UserAuthKey:%s";
    //客户端连接状态 key
    public static final String USER_CLIENT_CONNECT = "user_client_connect";
    //客户端退出标识。接收到此信息，客户端主动退出
    public static final String QUIT_CMD = "quit";
}
