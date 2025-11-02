package com.comment.service;

import com.comment.dto.Result;
import com.comment.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ada
 * @since ada
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryShopTypeList();
}
