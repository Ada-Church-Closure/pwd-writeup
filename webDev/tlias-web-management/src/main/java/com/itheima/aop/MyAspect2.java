package com.itheima.aop;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Proc;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@Component
// @Aspect
public class MyAspect2 {
    // 直接定义这个切点
    @Pointcut("execution(* com.itheima.service.impl.DeptServiceImpl.*(..))")
    private void pointcut(){}


    @Before("pointcut()")
    public void before(JoinPoint joinPoint){
        log.info("my8, before method will excute......");
    }

    // 注意环绕通知使用的类型就不一样了
    // 用joinPoint来获取一些信息
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        log.info("my8 around before");

        // 1.获取类名
        String className = joinPoint.getTarget().getClass().getName();
        log.info("ClassName: {}", className);

        // 2.方法签名
        String methodName = joinPoint.getSignature().getName();
        log.info("MethodName: {}", methodName);

        // 3.方法传入的参数
        Object[] args = joinPoint.getArgs();
        log.info("Args: {}", Arrays.toString(args));

        // 4.直接放行
        Object result = joinPoint.proceed();

        // 拿到这个返回值
        log.info("retValue: {}", result);
        log.info("my8 around after");

        return result;
    }
}
