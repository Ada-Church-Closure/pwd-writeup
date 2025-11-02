package com.comment.service;

import com.comment.dto.Result;
import com.comment.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 *
 * @author ada
 * @since 2025-10-27
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result secKillVoucher(Long voucherId) throws InterruptedException;

    void createVoucherOrder(VoucherOrder voucherId);
}
