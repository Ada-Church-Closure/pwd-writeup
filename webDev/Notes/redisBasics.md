# Redis

> 学习黑马的Redis基础课程的笔记,我想自己写,而不是找个所谓的"项目",能运行了之后就直接开始"背诵",太愚蠢了.
>
> 但是沉下心来学,没办法,环境就是这样.

## 简介

**key-value 键值对数据库**

NoSql--->非关系型数据库.

**非结构化**并且数据之间**没有关联**.

键值/图类型/Document

基本一致性/BASE不满足**ACID**

存储在内存中,更快/水平扩展.--->对性能要求很高的情况.



**RE**mote **DI**ctionary **S**erver.

单线程/命令原子性

性能:内存/IO多路复用/良好编码

内存持久化

主从/分片 集群

## 常见数据结构和命令

### 通用命令

模糊查询:

```sh
127.0.0.1:6379> set name fuck
OK
127.0.0.1:6379> get name
"fuck"
127.0.0.1:6379> set age 222
OK
127.0.0.1:6379> get sage
(nil)
127.0.0.1:6379> get age
"222"
127.0.0.1:6379> KEYS a**
1) "age"
127.0.0.1:6379> KEYS a?
(empty array)
127.0.0.1:6379> KEYS a??
1) "age"
127.0.0.1:6379> KEYS *
1) "test"
2) "name"
3) "age"
```

删除命令:

```sh
127.0.0.1:6379> MSET k1 v1 k2 v2 k3 v3		# 批量增加key
OK
127.0.0.1:6379> KEYS *
1) "test"
2) "k1"
3) "age"
4) "k2"
5) "k3"
127.0.0.1:6379> DEL k2
(integer) 1		# 删除的数量
127.0.0.1:6379> KEYS k*
1) "k1"
2) "k3"
```

判断key是否存在:

```sh
127.0.0.1:6379> EXISTS age
(integer) 1
127.0.0.1:6379> EXISTS age name
(integer) 1
127.0.0.1:6379> EXISTS age k1
(integer) 2
```

有效期(验证码):

```sh
127.0.0.1:6379> KEYS *
1) "test"
2) "k1"
3) "age"
4) "k3"
127.0.0.1:6379> EXPIRE test 20	# 设置有效期20s自动过期
(integer) 1
127.0.0.1:6379> TTL test
(integer) 14
127.0.0.1:6379> TTL test
(integer) 12
127.0.0.1:6379> TTL test
(integer) 11
127.0.0.1:6379> TTl test
(integer) -2
127.0.0.1:6379> TTl test
(integer) -2		# -2无效 -1永久有效
127.0.0.1:6379> KEYS *
1) "k1"
2) "age"
3) "k3"
```

### String

> 最基本的类型.

查询修改:

```sh
127.0.0.1:6379> set name fuck
OK
127.0.0.1:6379> get name
"fuck"
127.0.0.1:6379> set name shit
OK
127.0.0.1:6379> get name
"shit"
127.0.0.1:6379> MSET k1 v1 k2 v2 k3 v3
OK
127.0.0.1:6379> MGET k3 k2 k1
1) "v3"
2) "v2"
3) "v1"
```

增加:

```sh
127.0.0.1:6379> get age
"222"
127.0.0.1:6379> INCR age
(integer) 223
127.0.0.1:6379> INCR age
(integer) 224
127.0.0.1:6379> INCRBY age 114
(integer) 338
127.0.0.1:6379> get age
"338"
```

不存在设置以及有效期设置:

```sh
127.0.0.1:6379> SETNX name shit
(integer) 0
127.0.0.1:6379> SETEX name 10 fuck
OK
127.0.0.1:6379> TTL name
(integer) 7
127.0.0.1:6379> TTL name
(integer) 5
127.0.0.1:6379> TTL name
(integer) 4
127.0.0.1:6379> TTL name
(integer) 2
127.0.0.1:6379> TTL name
(integer) 1
127.0.0.1:6379> TTL name
(integer) -2
```

> 那么怎么进行key的区分?
>
> 用**:**来**区分层级结构**即可.--->相当于只在**表示**上面进行一种区分.

### Hash

> 原来只用字符串很麻烦,但是value用hash表来存储的话,修改很方便.

**无序**字典.

设置field字段:

```sh
# 设置字段
127.0.0.1:6379> HSET HTT:mem:1 name mio
(integer) 0
127.0.0.1:6379> HSET HTT:mem:2 name yui
(integer) 1
127.0.0.1:6379> HSET HTT:mem:2 age 17
(integer) 1
127.0.0.1:6379> KEYS *
1) "HTT:mem:1"
2) "k1"
3) "age"
4) "k2"
5) "k3"
6) "HTT:mem:2"
# 获取字段
127.0.0.1:6379> HGET HTT:mem:2 name
"yui"
127.0.0.1:6379> HGET HTT:mem:2 age
"17"
# 批量设置和获取
127.0.0.1:6379> HMSET HTT:mem:4 name azisa age 16
OK
127.0.0.1:6379> HMGET HTT:mem:4 name age
1) "azisa"
2) "16"
# 一次获取所有
127.0.0.1:6379> HGETALL HTT:mem:2
1) "name"
2) "yui"
3) "age"
4) "17"
# 拿到key或者value集合
127.0.0.1:6379> HKEYS HTT:mem:2
1) "name"
2) "age"
127.0.0.1:6379> HVALS HTT:mem:4
1) "azisa"
2) "16"
```

### List

> value暂时可以看成是一个双向链表.

模型是左右:LPUSH LPOP RPUSH RPOP--->这些来进行这个队列的操作.

很简单:

```sh
127.0.0.1:6379> LPUSH users 1 2 3		# 3 2 1
(integer) 3
127.0.0.1:6379> LPOP users
"3"		# (3) 2 1
127.0.0.1:6379> RPOP users
"1"		# 2 (1)
```

BLPOP/BRPOP--->进行**阻塞等待**.

### Set

> 无序,不可重复,交集,并集,差集等,都是比较基础的操作......

```sh
127.0.0.1:6379> SADD mem mio yui azisa
(integer) 3
127.0.0.1:6379> SMEMBERS mem
1) "mio"
2) "yui"
3) "azisa"
127.0.0.1:6379> SISMEMBER mem mio
(integer) 1
127.0.0.1:6379> SISMEMBER mem mugi
(integer) 0
127.0.0.1:6379> SREM mem azisa
(integer) 1
127.0.0.1:6379> SMEMBERS mem
1) "mio"
2) "yui"
# 还可以进行集合求交
127.0.0.1:6379> SMEMBERS mem
1) "mio"
2) "yui"
127.0.0.1:6379> SADD jazzu mio azusa sawako
(integer) 3
127.0.0.1:6379> SADD mem azusa
(integer) 1
127.0.0.1:6379> SINTER mem jazzu
1) "mio"
2) "azusa"
# 求差集
127.0.0.1:6379> SDIFF mem jazzu
1) "yui"
# 求并集
127.0.0.1:6379> SUNION mem jazzu
1) "mio"
2) "yui"
3) "azusa"
4) "sawako"
```

### SortedSet

> 可排序集合,数据结构和**TreeSet**不一样.
>
> 实现排行榜.

跳表(速度很高)和hash表.

```sh
# 增加元素
127.0.0.1:6379> get school
(error) WRONGTYPE Operation against a key holding the wrong kind of value
127.0.0.1:6379> ZADD school 100 mio 98 mugi 96 ritsu 33 yui 101 azusa
(integer) 0
# ZREM删除元素
127.0.0.1:6379> ZREM school mio
(integer) 1
127.0.0.1:6379> ZREM school yui
(integer) 1
127.0.0.1:6379> ZADD school 100 mio 98 mugi 96 ritsu 33 yui 101 azusa
(integer) 2
# 默认升序排序
127.0.0.1:6379> ZRANK school mio
(integer) 3
# 任何命令加REV进行降序的排序
127.0.0.1:6379> ZREVRANK school mio
(integer) 1
# 数量
127.0.0.1:6379> ZCARD school
(integer) 5
# 确定key某个区间有多少对象
127.0.0.1:6379> ZCOUNT school 0 40
(integer) 1
# 加减一个对象的key
127.0.0.1:6379> ZINCRBY school 10 yui
"43"
# 直接根据key排名,返回对象
127.0.0.1:6379> ZRANGE school 0 2
1) "yui"
2) "ritsu"
3) "mugi"
# 倒序排名,返回对象
127.0.0.1:6379> ZREVRANGE school 0 2
1) "azusa"
2) "mio"
3) "mugi"
# 在给定的范围内进行排名
127.0.0.1:6379> ZRANGEBYSCORE school 0 80
1) "yui"
```

## Java客户端的用法

直接根据命令名称来进行创建:

```java
public class JedisTest {
    private Jedis jedis;

    /**
     * 在当前测试类的每一个@Test方法执行之前运行一次
     */
    @BeforeEach
    void setUp(){
        // 建立连接
        jedis = new Jedis("127.0.0.1", 6379);
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
```

### jedis连接池

> 解决的问题和sql的连接池是一样的,**避免频繁的创建和销毁**.
>
> 线程**不安全**.

同样我们还可以通过一个连接池来进行创建:

```java
public class JedisConnectionFactory {
    private static final JedisPool jedisPool;

    static{
        // 配置连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        // 最大连接数
        poolConfig.setMaxTotal(8);

        // 最大空闲,即使没有人需求,为了有需求的时候不用创建,我先准备几个线程
        poolConfig.setMaxIdle(8);

        // 最小空闲连接
        poolConfig.setMinIdle(0);

        // 等待时间
        poolConfig.setMaxWaitMillis(1000);

        // 配置连接池对象
        jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379, 1000, "331979248");

    }

    public static Jedis getJedis(){
        return jedisPool.getResource();
    }

}
```

### Spring Data Redis

> Spring内部整合 **Lettuce**和**Jedis**.
>

对象**序列化**和**反序列化**.

还是一样,基本参数在yml里面配置:

> 方便,不用手写连接池.

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 331979248
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: 1000ms
```

使用的时候直接注入即可:

```java
@SpringBootTest
class RedisDemoApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void testString() {
        // 直接调用对应类型的方法即可.
        redisTemplate.opsForValue().set("name", "Shinobu");
        Object name = redisTemplate.opsForValue().get("name");
        System.out.println("name:" + name);
    }

}
```

但是这时候你会发现,存储进来之后就成这样了:

```java
127.0.0.1:6379> KEYS * 
1) "HTT:mem:2" 
2) "jazzu" 
3) "name" 
4) "HTT:mem:1" 
5) "HTT:mem:4" 
6) "mem" 
7) "\xac\xed\x00\x05t\x00\x04name"  // 直接jdk序列化了
8) "k2" 
9) "users" 
10) "school" 
11) "k1" 
12) "age" 
13) "k3" 
14) "user:1"
```

>  **改变序列化的方式**,上面把key也序列化了.

那么我们就写一个配置类,改变序列化的方式:

```java
@Configuration // 配置类
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        // 创建template对象
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置连接工厂
        template.setConnectionFactory(redisConnectionFactory);

        // 接下来,我们就可以指定序列化工具
        // 创建JSON序列化工具
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();

        // 设置key的序列化
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        // 设置Value的序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        return template;
    }
}
```

对于一个class来说:

```java
@Test
    void saveUser(){
        redisTemplate.opsForValue().set("user:101", new User("tamaki", 29));
        // 存入的时候,会进行序列化
        User user = (User) redisTemplate.opsForValue().get("user:101");
        // 读取的时候,进行反序列化
        System.out.println("User:" + user);
    }
```

因为要反序列化,所以获取的时候我们要知道对象的类型:

```sh
127.0.0.1:6379> get user:101
"{\"@class\":\"org.nunotaba.redisdemo.redis.config.pojo.User\",\"name\":\"tamaki\",\"age\":29}"
```

这里要拿到全类名,带来了内存开销,很愚蠢.

其实直接默认使用String反序列化,省去自定义过程,当我们要用到对象的时候,反序列化应该在java客户端进行.

```java
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
```

存进去的就是纯字符串了,之后把这个干成一个工具类就行.





