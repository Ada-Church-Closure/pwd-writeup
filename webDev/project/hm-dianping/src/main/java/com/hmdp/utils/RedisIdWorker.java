package com.hmdp.utils;

import org.springframework.cglib.core.Local;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.hmdp.utils.RedisConstants.*;

@Component
public class RedisIdWorker {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix){
        // 1.当前的时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;

        // 2.生成序列号,可以拼接一个日期字符串
        // 还能统计日期销量
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count =  stringRedisTemplate.opsForValue().increment(INCREMENT_ID_KEY + keyPrefix + ":" + date);

        // 这就是我们构造ID的方案,时间戳 + 计数器
        return (timeStamp << SERIAL_NUMBER_BITS) | count;
    }
}
