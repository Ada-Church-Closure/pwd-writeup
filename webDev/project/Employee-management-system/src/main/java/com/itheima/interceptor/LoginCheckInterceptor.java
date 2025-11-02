package com.itheima.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.itheima.pojo.Result;
import com.itheima.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {
    // 目标资源方法执行前进行执行,返回true,表示放行
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        // 1.获取请求的url
        String url = req.getRequestURL().toString();
        log.info("请求的url:{}", url);

        // 2.如果是login,直接放行
        if(url.contains("login")) {
            log.info("login操作,直接放行");
            return true;
        }

        // 3.获取请求头中的jwt
        String jwt = req.getHeader("token");

        // 4.判断token是否存在
        if(!StringUtils.hasLength(jwt)){
            log.info("token为空,返回未登录的信息");
            Result error = Result.error("NOT_LOGIN");
            // 把错误消息转换为JSON格式并且返回
            String notLogin = JSONObject.toJSONString(error);
            resp.getWriter().write(notLogin);
            return false;
        }

        // 5.存在jwt,校验并且解析jwt令牌
        try {
            JwtUtils.parseJWT(jwt);
        }catch (Exception e){
            log.info("令牌解析失败...");
            Result error = Result.error("NOT_LOGIN");
            // 把错误消息转换为JSON格式并且返回
            String notLogin = JSONObject.toJSONString(error);
            resp.getWriter().write(notLogin);
            return false;
        }
        // 令牌合法
        log.info("令牌合法,直接放行");
        return true;
    }

    // 目标资源方法执行之后执行
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }
    // 渲染完毕之后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
