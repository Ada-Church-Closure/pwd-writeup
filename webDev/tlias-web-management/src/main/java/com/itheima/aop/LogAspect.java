package com.itheima.aop;

import com.alibaba.fastjson.JSONObject;
import com.itheima.mapper.OperateLogMapper;
import com.itheima.pojo.OperateLog;
import com.itheima.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

@Aspect
@Slf4j
@Component
public class LogAspect {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private OperateLogMapper operateLogMapper;

    /**
     * 插入操作日志,利用AOP变成
     * @param joinPoint 连接点,执行的操作
     * @return          操作返回值
     * @throws Throwable    抛出异常
     */
    // 这就是能体现AOP功能强大的地方了.
    // 对于加了Log注解的函数都进行Around的操作.
    @Around("@annotation(com.itheima.anno.Log)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable{


        // 拿jwt Token并且解析令牌
        String jwt = request.getHeader("token");
        Claims claims = JwtUtils.parseJWT(jwt);
        Integer operateUser = (Integer)claims.get("id");

        // 操作时间
        LocalDateTime operateTime = LocalDateTime.now();

        // 目标类名
        String className = joinPoint.getClass().getName();

        // 操作方法名
        String methodName = joinPoint.getSignature().getName();

        // 方法参数
        Object[] args = joinPoint.getArgs();
        String methodParams = Arrays.toString(args);

        // 先运行原始目标方法

        long begin = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();

        String returnValue = JSONObject.toJSONString(result);

        Long costTime = end - begin;

        // 一次 new 到我们新建的Log类内部.
        OperateLog operateLog = new OperateLog(null, operateUser, operateTime, className, methodName, methodParams, returnValue, costTime);

        log.info("AOP 记录操作日志:{} ", operateLog);
        operateLogMapper.insert(operateLog);
        return result;
    }
}
