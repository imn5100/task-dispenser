package com.shaw.netty;

/** Created by shaw on 2017/1/11 0011. */
public class SocketMessage {
  private String appKey;
  private String appSecret;
  private String contents;
  // 1 连接登录请求 2.正常消息请求
  private Integer type;

  public String getAppKey() {
    return appKey;
  }

  public void setAppKey(String appKey) {
    this.appKey = appKey;
  }

  public String getAppSecret() {
    return appSecret;
  }

  public void setAppSecret(String appSecret) {
    this.appSecret = appSecret;
  }

  public Integer getType() {
    return type;
  }

  public void setType(Integer type) {
    this.type = type;
  }

  public String getContents() {
    return contents;
  }

  public void setContents(String contents) {
    this.contents = contents;
  }
}
