package com.comment.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    // caller定义锁的名称
    private final String name;
    private final StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";

    // 提前加载lua脚本,避免I/O流
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("/scripts/unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }



    /**
     * 获取lock的逻辑
     * @param timeoutSec 设置lock的过期时间
     * @return  是否获取成功
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取thread的唯一ID
        String threadID = ID_PREFIX + Thread.currentThread().getId();
        // 获取lock
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + this.name, threadID, timeoutSec, TimeUnit.SECONDS);

        // 注意这里是拆箱,如果success为NULL,那么就会空指针异常
        return Boolean.TRUE.equals(success);
    }

    /**
     * 利用lua脚本解决问题
     */
    @Override
    public void unlock(){
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId()
        );
    }

    /**
     * 1.释放lock的动作,解决业务阻塞导致多次释放lock的问题,设置线程唯一id作为lock name
     * 2.还有可能发生一种阻塞,thread1判断lock标识和释放lock之间发生阻塞,阻塞期间,超时释放lock,被thread2拿走
     *      那么thread1阻塞结束之后,会释放掉thread2的lock(因为之前已经判断过一次,而正是判断之后阻塞)
     *      那么我想实现这两个操作的原子性.(利用lua脚本)
     */
//    @Override
//    public void unlock() {
//        // 先获取当前线程标识
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        // 获取lock中的标识
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        // 接着判断线程是否一致
//        if(threadId.equals(id)) {
//            // ...这里发生了阻塞,怎么办?
//            stringRedisTemplate.delete(KEY_PREFIX + this.name);
//        }
//    }
}
