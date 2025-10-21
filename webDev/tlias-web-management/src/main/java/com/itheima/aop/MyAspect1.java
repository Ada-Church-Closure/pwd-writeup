package com.itheima.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
// @Aspect
@Component
public class MyAspect1 {

    // 直接定义这个切点
    @Pointcut("execution(* com.itheima.service.impl.DeptServiceImpl.*(..))")
    private void pointcut(){}

    // 定义环绕的方法
    @Before("pointcut()")
    public void before(){
        log.info("Before method will be executed......");
    }

    @After("pointcut()")
    public void after(){
        log.info("After the method was executed......");
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{
        log.info("Around before...");
        Object result = proceedingJoinPoint.proceed();
        log.info("Around after...");
        return result;
    }
}
