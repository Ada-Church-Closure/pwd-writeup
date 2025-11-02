package com.itheima.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

// @WebFilter(urlPatterns = "/*")
public class AbcFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("Abc 拦截到请求,放行前逻辑");
        chain.doFilter(request, response);
        System.out.println("Abc 放行后逻辑");
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
