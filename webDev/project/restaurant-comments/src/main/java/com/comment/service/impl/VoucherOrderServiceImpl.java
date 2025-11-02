package com.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.comment.dto.Result;
import com.comment.entity.VoucherOrder;
import com.comment.mapper.VoucherOrderMapper;
import com.comment.service.ISeckillVoucherService;
import com.comment.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.comment.utils.RedisIdWorker;
import com.comment.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 *  服务实现类
 * @author ada
 * @since 2025-10-28
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    // 提前加载lua脚本,避免I/O流
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("/scripts/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    // 创建线程池(单线程),从阻塞队列里面取要写入sql数据库内部的数据
    public static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        // 直接在这里提交任务
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }


    // 这里是单开一个thread从Stream的消息队列中获取并且处理消息
    private class VoucherOrderHandler implements Runnable{
        String queueName = "stream.orders";
        @Override
        public void run() {
            while(true){
                // 这里进行阻塞等待
                try {
                    // 1.拿到消息队列的订单信息
                    // XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.order

                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    // 2.判断获取消息是否成功
                    // 2.1失败,没有消息,下个循环
                    if(list == null || list.isEmpty()){
                        continue;
                    }
                    // 3.成功,写入mysql
                    // 首先解析消息
                    MapRecord<String, Object, Object> record = list.get(0);
                    // 这个value就是我们传进去的键值对集合
                    Map<Object, Object> values = record.getValue();
                    // 把拿到的map转换成订单对象
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    // 4.处理完毕,返回ACK确认消息
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    // 到这里,证明消息出现异常,消息会放入pending list,我们要处理pending list内部的出现异常的消息
                    log.error("处理订单出现异常!!!", e);
                    handlePendingList();
                }
            }
        }
        // 处理异常消息,但是其实和上面是类似的
        private void handlePendingList(){
            while(true){
                // 这里进行阻塞等待
                try {
                    // 1.拿到pending-list的订单信息
                    // XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.order

                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    // 2.判断获取消息是否成功
                    // pending list中没有消息,直接退出循环
                    if(list == null || list.isEmpty()){
                        break;
                    }
                    // 3.成功,写入mysql
                    // 首先解析消息
                    MapRecord<String, Object, Object> record = list.get(0);
                    // 这个value就是我们传进去的键值对集合
                    Map<Object, Object> values = record.getValue();
                    // 把拿到的map转换成订单对象
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    // 4.处理完毕,返回ACK确认消息
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    // 到这里,证明消息出现异常,消息会放入pending list,我们要处理pending list内部的出现异常的消息
                    log.error("处理pending list消息出现异常!!!", e);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }


//    //1.1 阻塞队列:当有一个线程试图从阻塞队列中读取的时候,如果为空,这个线程会等待
//    private final BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
//    // 异步写入的任务,这个任务应当在我这个类初始化之后就开始执行
//    private class VoucherOrderHandler implements Runnable{
//        @Override
//        public void run() {
//            while(true){
//                // 这里进行阻塞等待
//                try {
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    handleVoucherOrder(voucherOrder);
//                } catch (Exception e) {
//                    log.error("处理订单出现异常!!!", e);
//                }
//            }
//        }
//    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) throws InterruptedException {
        // 这里我们直接使用redisson提供的lock
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        // 获取lock
        // 这里也有问题:
        // 如果业务阻塞导致lock提前释放,那么这个业务结束的时候还会再次释放lock
        // 此时他释放的lock就是别人的lock,这就是lock为什么需要name,我们释放之前先看是不是自己的thread id
        // 但是jvm内部,考虑到分布式,仅仅使用thread id还不够特殊
        boolean isLocked = lock.tryLock(1L, TimeUnit.SECONDS);

        // 如果获取失败
        if(!isLocked){
            return;
        }
        try {
            // 获取代理的对象/事务
            proxy.createVoucherOrder(voucherOrder);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

    }

    private IVoucherOrderService proxy;

    // 利用redis做优化
    @Override   // 一个秒杀操作一定是一个事务
    public Result secKillVoucher(Long voucherId) throws InterruptedException {
        long orderId = redisIdWorker.nextId("order");
        Long userId = UserHolder.getUser().getId();
        // 这里的lua脚本会生成订单信息送进消息队列,我们之后要进行处理
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        );

        // 如果不是0,证明错误
        int res = result.intValue();
        if(res != 0){
            return Result.fail(res == 1 ? "不幸,库存不足" : "求求别重复下单~");
        }
        // 提前获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 返回订单id
        return Result.ok(orderId);
    }


//    // 利用redis做优化
//    @Override   // 一个秒杀操作一定是一个事务
//    public Result secKillVoucher(Long voucherId) throws InterruptedException {
//        Long userId = UserHolder.getUser().getId();
//        Long result = stringRedisTemplate.execute(
//                SECKILL_SCRIPT,
//                Collections.emptyList(),
//                voucherId.toString(),
//                userId.toString()
//        );
//
//        // 如果不是0,证明错误
//        int res = result.intValue();
//        if(res != 0){
//            return Result.fail(res == 1 ? "不幸,库存不足" : "求求别重复下单~");
//        }
//
//        // 拿到了信息,直接进行封装,放到一个阻塞队列,异步写入数据库
//        VoucherOrder voucherOrder = new VoucherOrder();
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        voucherOrder.setUserId(userId);
//        // 代金券ID
//        voucherOrder.setVoucherId(voucherId);
//        orderTasks.add(voucherOrder);
//
//        // 提前获取代理对象
//        proxy = (IVoucherOrderService) AopContext.currentProxy();
//
//
//        // 返回订单id
//        return Result.ok(orderId);
//    }


//    @Override   // 一个秒杀操作一定是一个事务
//    public Result secKillVoucher(Long voucherId) throws InterruptedException {
//        // 1.查询优惠券
//        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
//
//        // 2.是否开卖
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("まだ時間ありますよ！");
//        }
//
//        // 3.是否过期
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("運が悪いなあああ、もうおわった！！！");
//        }
//
//        // 4.库存是否充足
//        // 这里可能是多线程不安全的.
//        if (voucher.getStock() < 1){
//            return Result.fail("库存不足了!!!");
//        }
//
//        // 5.减去库存
//        // 这就是乐观lock
//        boolean success = iSeckillVoucherService.update()
//                .setSql("stock = stock - 1") // set stock = stock - 1
//                .eq("voucher_id", voucherId).gt("stock", 0) // 只要更新时候的value和查询的时候是一样的即可/其实只要更新前大于0即可,否则会出问题
//                .update();
//
//        if(!success){
//            return Result.fail("库存不足了!!!");
//        }
//
//        Long userId = UserHolder.getUser().getId();
//        // lock放在这里是因为一定要等到整个事务提交之后,才能去释放lock.
//        // 事务没有提交,也就没有写到数据库内部,此时可能产生问题.
//
//        // 不能给整个this方法上lock,只给一个user上锁
//        // 我们要保证,只要是来自于一个用户的请求,使用的就是一把lock
//        // intern保证持有的lock取决于字符串的常量,而不是一个内存中的新对象
//        // synchronized (userId.toString().intern()) {
//            // return createVoucherOrder(voucherId);
//            // 这样直接调用,事务是不会生效的.要使用代理对象.
//
//        // 这里我们要使用基于redis的分布式lock
//        // SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//
//        // 这里我们直接使用redisson提供的lock
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//
//        // 获取lock
//        // 这里也有问题:
//        // 如果业务阻塞导致lock提前释放,那么这个业务结束的时候还会再次释放lock
//        // 此时他释放的lock就是别人的lock,这就是lock为什么需要name,我们释放之前先看是不是自己的thread id
//        // 但是jvm内部,考虑到分布式,仅仅使用thread id还不够特殊
//        boolean isLocked = lock.tryLock(1L, TimeUnit.SECONDS);
//
//        // 如果获取失败
//        if(!isLocked){
//            return Result.fail("不允许用户重复下单!!!");
//        }
//
//        try {
//            // 获取代理的对象/事务
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } catch (IllegalStateException e) {
//            throw new RuntimeException(e);
//        } finally {
//            lock.unlock();
//        }
//        // }
//    }

    // 直接给整个逻辑上悲观锁
    // 现在是异步执行,只要写入mysql数据库即可
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();

        // 其实这种情况应该不会发生,因为我们之前判断过.
        if(count > 0){
            return;
        }

        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0)
                .update();

        if(!success){
            log.error("扣减库存失败!!!");
            return;
        }
        // 最后创建订单,这里会把订单更新到mysql数据库
        save(voucherOrder);
    }
}
