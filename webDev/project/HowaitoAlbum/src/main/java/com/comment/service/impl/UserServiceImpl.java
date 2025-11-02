package com.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.comment.dto.LoginFormDTO;
import com.comment.dto.Result;
import com.comment.dto.UserDTO;
import com.comment.entity.User;
import com.comment.mapper.UserMapper;
import com.comment.service.IUserService;
import com.comment.utils.RegexUtils;
import com.comment.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.comment.utils.RedisConstants.*;
import static com.comment.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 服务实现类
 *
 * @author ada
 * @since 2025-10-25
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;


    /**
     * 发送验证码的业务
     * @param phone 手机号
     * @param session   session对象
     * @return  是否成功发送
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.手机号的合法校验
        if(RegexUtils.isPhoneInvalid(phone)){
            // 2.不合法,返回一个错误信息
            return Result.fail("手机号格式错误!");
        }
        // 3.生成随机验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到redis
        // session.setAttribute("code", code);
        // 设置验证码有效期,两分钟 ---> set key value ex
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.模拟验证码的发送
        log.debug("发送验证码成功,验证码:{}", code);

        return Result.ok();
    }

    /**
     * 登陆的流程,但是如果以前没有注册过,还会直接进行注册
     * @param loginForm 提交的登陆或者注册的表单
     * @param session   session对象
     * @return          是否登陆成功
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.先校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            // 2.不合法,返回一个错误信息
            return Result.fail("手机号格式错误!");
        }
        // 2.校验验证码
        // Object cacheCode = session.getAttribute("code");
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.equals(code)){
            return Result.fail("验证码错误!");
        }
        // 3.Mybatis plus查询,单表很方便
        User user = query().eq("phone", phone).one();

        // 4.判断这个用户是否存在,不存在就创建一个用户
        if(user == null){
            user = createUserWithPhone(phone);
        }

        // 随机生成token来作为登陆令牌
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        // 把要存储的对象转成map再进行存储
        // stringRedisTemplate只要string的值
        // 我们可以编辑对象字段的属性.
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        // 把这个登陆的信息存放到redis内部
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 这个登陆也设置有效期,30min
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 6.返回
        // 注意,用户login了之后,我们要返回token.
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        Long userId = UserHolder.getUser().getId();

        LocalDateTime now = LocalDateTime.now();

        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        // 1-31
        // 本月是第几天.
        int dayOfMonth = now.getDayOfMonth();
        --dayOfMonth;

        // 操作redis
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        Long userId = UserHolder.getUser().getId();

        LocalDateTime now = LocalDateTime.now();

        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        // 1-31
        // 本月是第几天.
        int dayOfMonth = now.getDayOfMonth();
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0)
        );

        if(result == null || result.isEmpty()){
            return Result.ok(0);
        }

        Long signNumber = result.get(0);
        if(signNumber == 0){
            return Result.ok(0);
        }

        int signCounter = 0;
        while ((signNumber & 1) != 0) {
            ++signCounter;
            signNumber >>>= 1;
        }

        return Result.ok(signCounter);
    }

    private User createUserWithPhone(String phone) {
        // 创建一个新的用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
