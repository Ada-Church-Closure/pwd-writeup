package com.comment.utils;


import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 实现登陆请求的拦截
 * 我们手动创建的类,不能做依赖注入
 * 那么可以谁利用这个构造方法,谁来做依赖注入即可
 *
 * interceptor的一个核心也是 ThreadLocal的使用,后面的三层架构都可以利用这里存储的信息.
 */
public class LoginInterceptor implements HandlerInterceptor {
    // 在进入之前做登陆校验
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 在这里真正进行拦截的操作
        if(UserHolder.getUser() == null){
            response.setStatus(401);
            return false;
        }

        // 存在用户就直接放行
        return true;
    }
}
