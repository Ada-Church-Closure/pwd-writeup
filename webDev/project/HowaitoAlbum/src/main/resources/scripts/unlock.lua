---- 锁的key,实现原子性的基于redis的分布式lock,整个lua脚本是具有原子性的
--local key = KEYS[1]
---- 当前线程的标识
--local threadId = ARGV[1]
--
---- 获取lock中的线程的标识
--local id = redis.call('get', key)
--
---- 比较线程标识与lock中的标识是否一致
--if(id == threadId) then
--    -- 释放lock
--    return redis.call('del', key)
--end
--return 0

if(redis.call('get', KEYS[1]) == ARGV[1]) then
    return redis.call('del', KEYS[1])
end
return 0