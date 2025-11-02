package com.comment.service;

import com.comment.dto.Result;
import com.comment.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 *  服务类
 *
 * @author ada
 * @since 2025-10-25
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id) throws InterruptedException;

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
