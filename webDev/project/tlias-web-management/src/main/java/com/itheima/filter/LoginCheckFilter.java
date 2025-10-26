package com.itheima.filter;


import com.alibaba.fastjson.JSONObject;
import com.itheima.pojo.Result;
import com.itheima.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
// @WebFilter(urlPatterns = "/*")
public class LoginCheckFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 强转类型
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        // 1.获取请求的url
        String url = req.getRequestURL().toString();
        log.info("请求的url:{}", url);

        // 2.如果是login,直接放行
        if(url.contains("login")) {
            log.info("login操作,直接放行");
            chain.doFilter(request, response);
            return;
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
            return;
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
            return;
        }
        // 令牌合法
        log.info("令牌合法,直接放行");
        chain.doFilter(request, response);

    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
