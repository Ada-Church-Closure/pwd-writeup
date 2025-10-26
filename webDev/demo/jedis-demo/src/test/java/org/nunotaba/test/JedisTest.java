package org.nunotaba.test;



import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nunotaba.jedis.util.JedisConnectionFactory;
import redis.clients.jedis.Jedis;

import java.util.Map;

public class JedisTest {
    private Jedis jedis;

    /**
     * 在当前测试类的每一个@Test方法执行之前运行一次
     */
    @BeforeEach
    void setUp(){
        // 建立连接,从连接池获取一个连接
        jedis = JedisConnectionFactory.getJedis();
        // 设置密码
        jedis.auth("331979248");
        // 选择一个库
        jedis.select(0);
    }
    @Test
    void testString(){
        String result = jedis.set("name", "nunotaba");
        System.out.println("Result:" + result);

        String name = jedis.get("name");
        System.out.println("name:" + name);

    }
    @Test
    void testHash(){
        jedis.hset("user:1", "name", "ass");
        jedis.hset("user:1", "age", "22");

        Map<String, String> map = jedis.hgetAll("user:1");
        System.out.println(map);
    }
    /**
     * 测试完成之后释放资源,先进行资源检查
     */
    @AfterEach
    void close(){
        if(jedis != null){
            jedis.close();
        }
    }

}
