package com.comment.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.comment.dto.Result;
import com.comment.entity.ShopType;
import com.comment.mapper.ShopTypeMapper;
import com.comment.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.comment.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ada
 * @since ada
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ApplicationArguments springApplicationArguments;

    @Override
    public Result queryShopTypeList() {
        // 1.redis内部查询商铺type缓存
        String shopTypeListJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);

        // 2.判断是否存在
        if(StrUtil.isNotBlank(shopTypeListJson)){
            List<ShopType> shopTypeList = JSONUtil.toList(shopTypeListJson, ShopType.class);
            return Result.ok(shopTypeList);
        }

        // 3.如果不存在,数据库中查询
        // 不会plus的使用
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();

        if(shopTypeList == null){
            return Result.fail("查询商铺类型失败!");
        }

        // 4.数据库中存在,写入redis缓存
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(shopTypeList));

        // 成功查询,返回查询信息.
        return Result.ok(shopTypeList);
    }
}
