package com.comment.config;

import com.comment.utils.LoginInterceptor;
import com.comment.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 配置类,配置拦截器
     * @param registry  拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 这是拦截登陆的拦截器
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/user/code",
                "/user/login",
                "/blog/hot",
                "/shop/**",
                "/shop-type/**",
                "/upload/**",
                "/voucher/**"
        ).order(1);

        // 配置默认拦截器,拦截所有请求并且刷新token,用order来进行排序
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);

    }
}
