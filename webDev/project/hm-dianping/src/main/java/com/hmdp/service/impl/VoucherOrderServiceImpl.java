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
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.AopContext;
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


    @Override   // 一个秒杀操作一定是一个事务
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
        // 这里可能是多线程不安全的.
        if (voucher.getStock() < 1){
            return Result.fail("库存不足了!!!");
        }

        // 5.减去库存
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0) // 只要更新时候的value和查询的时候是一样的即可/其实只要更新前大于0即可,否则会出问题
                .update();

        if(!success){
            return Result.fail("库存不足了!!!");
        }

        Long userId = UserHolder.getUser().getId();
        // lock放在这里是因为一定要等到整个事务提交之后,才能去释放lock.
        // 事务没有提交,也就没有写到数据库内部,此时可能产生问题.

        // 不能给整个this方法上lock,只给一个user上锁
        // 我们要保证,只要是来自于一个用户的请求,使用的就是一把lock
        // intern保证持有的lock取决于字符串的常量,而不是一个内存中的新对象
        synchronized (userId.toString().intern()) {
            // return createVoucherOrder(voucherId);
            // 这样直接调用,事务是不会生效的.要使用代理对象.
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    // 直接给整个逻辑上悲观锁
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 先进行一人一单地查询逻辑, 这里的查询也是会出现超卖的问题
        Long userId = UserHolder.getUser().getId();

            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                // 存在,表示用户已经购买
                return Result.fail("用户已经购买了,别当黄牛!!!");
            }
            // 6.创建一个订单
            // 全局唯一订单ID
            VoucherOrder voucherOrder = new VoucherOrder();
            long orderId = redisIdWorker.nextId("order");
            voucherOrder.setId(orderId);
            voucherOrder.setUserId(userId);
            // 代金券ID
            voucherOrder.setVoucherId(voucherId);
            save(voucherOrder);

            return Result.ok(orderId);

    }
}
