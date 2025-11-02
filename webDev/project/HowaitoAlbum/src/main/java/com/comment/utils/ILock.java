package com.comment.utils;

/**
 * 实现分布式lock
 */
public interface ILock {

    /**
     * 尝试去获取lock
     * @param timeoutSec 设置lock的过期时间
     * @return  true获取成功,false获取失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
