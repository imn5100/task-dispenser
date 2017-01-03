package com.shaw.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

/**
 * 使用json序列化工具，保证python保存对象与java可共享
 */
public class GenericFastJsonRedisSerializer implements RedisSerializer<Object> {
    static final byte[] EMPTY_ARRAY = new byte[0];
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");


    public byte[] serialize(Object t) throws SerializationException {
        if (t == null) {
            return EMPTY_ARRAY;
        }
        return JSON.toJSONString(t, SerializerFeature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);
        Object object = JSON.parseObject(str, Object.class);
        return object;
    }
}