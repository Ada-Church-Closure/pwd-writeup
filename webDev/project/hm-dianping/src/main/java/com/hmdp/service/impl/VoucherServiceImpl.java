package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * @author ada
 * @since 2025-10-30
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }


    /**
     * 每次增加voucher的时候.先保存在mysql,接着加载进redis内存.
     * @param voucher 要增加的voucher信息
     */
    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);

        // 同时还要写入redis内部, 实现秒杀的优化
        // 存放秒杀券数量
        // 这个接口是只有在add的时候,才会写入redis
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());

    }

    /**
     * 由于只有add的时候才会加载到redis,每次重新启动的时候,都要从mysql里面加载回redis内存.
     */
    @PostConstruct
    public void loadSeckillVouchersToRedis(){
        // list是mybatis-plus的一个方法,从数据库查询记录并且返回.
        List<SeckillVoucher> list = seckillVoucherService.list();
        for(SeckillVoucher voucher : list){
            stringRedisTemplate.opsForValue()
                    .set(SECKILL_STOCK_KEY + voucher.getVoucherId(), voucher.getStock().toString());
        }
    }
}
