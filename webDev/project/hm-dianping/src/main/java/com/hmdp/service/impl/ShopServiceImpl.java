package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author mio
 * @since 2025-10-26
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;


    @Override
    public Result queryById(Long id) throws InterruptedException {
        // 解决缓存穿透
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        // Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if(shop == null){
            return Result.fail("店铺不存在!");
        }
        return Result.ok(shop);
    }

        /**
     * 利用逻辑过期解决缓存击穿的问题
     * 把查到的商铺,封装逻辑过期时间存入redis,这是缓存预热
     * @param id 商铺id
     */
    public void saveShop2Redis(Long id, Long expireMinutes){
        Shop shop = getById(id);

        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusMinutes(expireMinutes));
        // 没有真正的TTL,而是我们封装的逻辑过期时间.
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

//    /**
//     * 互斥锁解决缓存击穿问题
//     * @param id
//     * @return
//     */
//    public Shop queryWithMutex(Long id) throws InterruptedException {
//        // 1.redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//
//        // 2.判断是否存在,存在就返回这个shop信息
//        if (StrUtil.isNotBlank(shopJson)) {
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        // 这种情况就必然是空字符串,我们都不让他更新redis TTL
//        if(shopJson != null){
//            return null;
//        }
//
//        String lockKey = LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            if(!isLock){
//                // 获取lock失败,休眠,然后重新查询
//                Thread.sleep(50);
//                queryWithMutex(id);
//            }
//
//            // 获取lock了之后还要进行double check,如果是存在的还是直接返回.
//            shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//
//            if (StrUtil.isNotBlank(shopJson)) {
//                return JSONUtil.toBean(shopJson, Shop.class);
//            }
//
//            // 3.没有在redis中查询到,直接在sql中查询
//            shop = getById(id);
//
//            // 模拟重建的延时
//            Thread.sleep(200);
//
//            // 4.数据库中没有,返回错误
//            if(shop == null){
//                // 为了防止缓存穿透,我们暂时设置一个null对象在redis中
//                // 这就是"草船借箭",以后2min之内攻击无效.
//                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//
//            // 5.数据库中存在,写入redis
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            // 释放mutex
//            // 就算上面产生异常,也一定要释放lock
//            unlock(lockKey);
//        }
//        return shop;
//    }
    /**
     * 核心,实现更新数据库并且删除redis中的缓存数据
     * 原子性,事务
     * @param shop 商铺信息
     * @return  是否执行成功
     */
    @Transactional
    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null) {
            return Result.fail("商铺的id不能为空!");
        }
            // 1.更新数据库
            updateById(shop);
            // 2.删除缓存
            stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
            return Result.ok();
    }


}
