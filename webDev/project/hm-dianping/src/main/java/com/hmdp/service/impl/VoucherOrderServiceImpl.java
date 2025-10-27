package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import com.sun.corba.se.spi.servicecontext.ORBVersionServiceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.ResourceTransactionManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;


    @Resource
    private RedisIdWorker redisIdWorker;
    @Autowired
    private ResourceTransactionManager resourceTransactionManager;

    @Override
    @Transactional // 一个秒杀操作一定是一个事务
    public Result secKillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);

        // 2.是否开卖
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("まだ時間ありますよ！");
        }

        // 3.是否过期
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("運が悪いなあああ、もうおわった！！！");
        }

        // 4.库存是否充足
        if (voucher.getStock() < 1){
            return Result.fail("库存不足了!!!");
        }

        // 5.减去库存
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).update();

        if(!success){
            return Result.fail("库存不足了!!!");
        }

        // 6.创建一个订单
        // 全局唯一订单ID
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 用户ID
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        // 代金券ID
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        return Result.ok(orderId);
    }
}
