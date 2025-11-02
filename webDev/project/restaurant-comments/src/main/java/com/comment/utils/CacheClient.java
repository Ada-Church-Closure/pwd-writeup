package com.comment.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.comment.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 设置redis内部TTL
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }


    // 设置逻辑过期时间
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 解决缓存穿透,巧妙利用泛型和函数式编程.
     * @param id    商铺id
     * @param type 返回类型
     * @param dbFallback 数据库中查询的逻辑
     * @return redis中查询的结果
     */
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1.redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        // 2.判断是否存在,存在就返回这个shop信息
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        // 这种情况就必然是空字符串,我们都不让他更新redis TTL
        if(json != null){
            return null;
        }

        // 3.没有在redis中查询到,直接在sql中查询
        R r = dbFallback.apply(id);

        // 4.数据库中没有,返回错误
        if(r == null){
            // 为了防止缓存穿透,我们暂时设置一个null对象在redis中
            // 这就是"草船借箭",以后2min之内攻击无效.
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        // 数据库中存在,直接写入redis内部
        this.set(key, r, time,unit);
        return r;
    }

    // 创建一个线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    /**
     * 利用redis的setnx来进行获取lock和释放lock的操作
     * @param key 这个key是lock的key,你可以认为我们在使用redis的特性来实现这个互斥锁
     */
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }



    /**
     * 利用逻辑过期解决缓存击穿的问题.
     * 注意,这里我们认为,但凡是访问的数据都是热点key,也就是只能解决热点key的问题.
     * 其余的是无法访问的,我们可能会在某个活动开始之前,提前把所有热点相关的key都放进来.
     * @param id
     * @return
     * @throws InterruptedException
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) throws InterruptedException {
        String key = keyPrefix + id;
        // 1.redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isBlank(json)) {
            return null;
        }

        // 缓存命中,把json String反序列化成一个对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        // 因为redis data里面的data的类型是object
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 未过期,直接返回
        if(expireTime.isAfter(LocalDateTime.now())){
            return r;
        }

        // 过期,获取互斥锁并且重建缓存,获取不到,直接返回过期的
        String lockKey = LOCK_SHOP_KEY + id;
        // 如果拿到了lock,开一个新的thread并且尝试重建
        boolean isLock = tryLock(lockKey);
        if(isLock){
            // 每次拿到lock都要进行double Check
            // 因为你拿到lock的时候,有可能是一个thread刚重建完
            json = stringRedisTemplate.opsForValue().get(key);
            redisData = JSONUtil.toBean(json, RedisData.class);
            r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
            expireTime = redisData.getExpireTime();
            if(expireTime.isAfter(LocalDateTime.now())){
                unlock(lockKey);
                return r;
            }

            // 新开一个线程进行更新的操作
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 最后一定要进行lock的释放
                    unlock(lockKey);
                }
            });

        }

        // 没拿到lock也直接返回旧的数据
        return r;
    }


}
