package com.comment.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.comment.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 只要用户访问,我们都要刷新redis中的token,这是第一层拦截器
 */
public class RefreshTokenInterceptor implements HandlerInterceptor{
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 在进入之前做登陆校验
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取header中的token对象
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            return true;
                  }

        // 2.基于token获取redis中的用户对象
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);

        if(userMap.isEmpty()){
            return true;
        }

        // 3.从redis内部拿到了hash之后,转换成userDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO);

        // 4.每当用户访问一次,我们都要刷新登陆的有效期 30min
        // 但是没有拦截的话就不会更新,按道理讲,用户只要进行了浏览都应该更新.--->拦截所有路径,两个拦截器.
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 直接放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 结束之后要移除线程资源
        UserHolder.removeUser();
    }

}
