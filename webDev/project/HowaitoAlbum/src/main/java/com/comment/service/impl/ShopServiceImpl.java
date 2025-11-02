package com.comment.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comment.dto.Result;
import com.comment.entity.Shop;
import com.comment.mapper.ShopMapper;
import com.comment.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.comment.utils.CacheClient;
import com.comment.utils.RedisData;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.comment.utils.RedisConstants.*;
import static com.comment.utils.SystemConstants.DEFAULT_PAGE_SIZE;

/**
 *  服务实现类
 *
 * @author ada
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

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // TODO 这里分页也有一些问题,会不断重复查询
        // 判断是否要根据坐标查询
        if(x == null || y == null){
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        // 否则我们要计算分页参数,并且按照距离进行排序
        int from = (current - 1) * DEFAULT_PAGE_SIZE;
        int end = current * DEFAULT_PAGE_SIZE;

        // 在redis做查询
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(10000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );

        if(results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();

        if(list.size() < from){
            // 不存在下一页,结束
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        // 截取from-end的部分
        list.stream().skip(from).forEach(result -> {
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));

            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });

        // 根据id查询这些shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query()
                .in("id", ids)
                .last("ORDER BY FIELD( id," + idStr + ")").list();

        for (Shop shop : shops) {
            // 把距离和商店对应起来
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        return Result.ok(shops);
    }
}
