package org.nunotaba.jedis.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisConnectionFactory {
    private static final JedisPool jedisPool;

    static{
        // 配置连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        // 最大连接数
        poolConfig.setMaxTotal(8);

        // 最大空闲,即使没有人需求,为了有需求的时候不用创建,我先准备几个线程
        poolConfig.setMaxIdle(8);

        // 最小空闲连接
        poolConfig.setMinIdle(0);

        // 等待时间
        poolConfig.setMaxWaitMillis(1000);

        // 配置连接池对象
        jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379, 1000, "331979248");

    }

    public static Jedis getJedis(){
        return jedisPool.getResource();
    }

}
