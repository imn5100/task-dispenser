package com.shaw.bo;

import java.io.Serializable;

/**
 * Created by shaw on 2017/1/3 0003.
 */
public class RedisMessage<T> implements Serializable {
    private T data;
    private String topic;
    private String extendedInfo;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getExtendedInfo() {
        return extendedInfo;
    }

    public void setExtendedInfo(String extendedInfo) {
        this.extendedInfo = extendedInfo;
    }

    @Override
    public String toString() {
        return "RedisMessage{" +
                "data=" + data +
                ", topic='" + topic + '\'' +
                ", extendedInfo='" + extendedInfo + '\'' +
                '}';
    }
}
