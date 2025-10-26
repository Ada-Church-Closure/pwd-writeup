package com.itheima.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
// @Aspect // AOP类
@Component
public class TimeAspect {

    @Around("execution(* com.itheima.service.*.*(..))") // 切入点表达式,是否就是直接使用正则?
    public Object recordTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();

        log.info("{} consumes {}ms......", joinPoint.getSignature(), end - begin);

        return result;
    }
}
