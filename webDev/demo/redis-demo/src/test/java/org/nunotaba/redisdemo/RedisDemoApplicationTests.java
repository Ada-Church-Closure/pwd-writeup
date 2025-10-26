package org.nunotaba.redisdemo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nunotaba.redisdemo.redis.config.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class RedisDemoApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testString() {
        stringRedisTemplate.opsForValue().set("name", "Shinoba");
        Object name = stringRedisTemplate.opsForValue().get("name");
        System.out.println("name:" + name);
    }


    // 使用序列化工具
    public static final ObjectMapper mapper = new ObjectMapper();
    @Test
    void saveUser() throws JsonProcessingException {
        // 创建对象,并且手动序列化
        User user = new User("nadeshiko", 23);
        String json = mapper.writeValueAsString(user);

        stringRedisTemplate.opsForValue().set("user:200", json);

        // 获取数据,还要手动反序列化
        String jsonUser = stringRedisTemplate.opsForValue().get("user:200");
        User user1 = mapper.readValue(jsonUser, User.class);
        System.out.println("user1" + user1);
    }

}
