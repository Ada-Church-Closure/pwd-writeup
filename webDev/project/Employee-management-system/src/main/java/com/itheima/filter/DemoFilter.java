package com.itheima.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

// @WebFilter(urlPatterns = "/*")
public class DemoFilter implements Filter {

    @Override   // 初始化,仅调用一次
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("DemoFilter Init executed...");
        Filter.super.init(filterConfig);
    }

    @Override   // 拦截之后就会调用,调用多次
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("DemoFilter 拦截到请求,放行前逻辑");
        filterChain.doFilter(servletRequest, servletResponse);
        System.out.println("DemoFilter 放行后逻辑");
    }

    @Override   // 调用多次
    public void destroy() {
        System.out.println("DemoFilter Destroy Executed...");
        Filter.super.destroy();
    }
}
