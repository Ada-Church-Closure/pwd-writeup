# web入门

## http:超文本传输

1.基于TCP,面向连接

2.无状态请求

### 请求协议--->文本字符串.

**GET**请求--->直接作为请求行参数,没有请求体.

**POST**请求--->请求参数在请求体内部(payload),POST请求大小没有限制.

### 响应协议.

状态码:

1XX响应中

2XX成功

3XX重定向--->细节?--->自动重新请求资源.

302--->Location自动定向.	304--->直接看本地缓存.

4XX客户端发生错误.

400请求语法错误	403拒绝提供服务	404请求资源不存在	405请求方式有误

5XX服务端发生错误.

500服务器报错.

### 协议解析

手写一个简单的http即可稍微理解细节.

所以我们用web服务器--->封装http协议,比较繁琐,直接部署web项目.

## webServer:Tomcat

端口冲突问题.字符编码问题. 但是用linux会很简单.

localhost:80--->http协议的默认端口就是80.

webapps部署.

直接在linux内进行服务的操作即可.

springboot--->有很多起步依赖.--->直接集成了tomcat,占用端口号.(**内嵌tomcat**)

## 请求响应

DispatcherServlet--->核心,前端控制器.

我们编写Controller--->控制器程序.

### Postman使用

接口测试工具.

### 请求

参数问题

简单参数：--->直接自动进行转换,这是sb的方式.

```java
@RestController
public class RequestController {
    @RequestMapping("/simpleParam")
    public String simpleParam(String name, int age){
        System.out.println(name + ":" + age);
        return "はい、確かに届けました！";
    }
}

```

那么POST就是直接在请求体中夹带参数

可以使用RequestParam来控制参数的传递问题:

```java
@RestController
public class RequestController {
    @RequestMapping("/simpleParam")
    public String simpleParam(@RequestParam(name = "name", required = false)String username, int age){
        System.out.println(username + ":" + age);
        return "はい、確かに届けました！";
    }
}
```

实体参数:

有很多参数怎么说.--->封装一个实体类.

```java
  @RequestMapping("/simplePojo")
    public String simplePojo(User user){
        System.out.println(user);
        return "OK, we got the user!!!";
    }
```

有多个类?

类要一个一个进行赋值--->其实都差不多.

数组/集合参数.

日期时间类型参数.--->指定格式

json参数--->前后端异步交互.body raw数据,RequestBody--->直接封装到实体对象.

路径参数--->URL直接传递参数.

> 怎么玩路径绕过?

### 响应

设置响应数据.--->统一**返回的格式**.--->封装成一个结果类,这样前端好处理.

@RequestBody--->数据会自动转换并且返回.

### 分层解耦

#### *三层架构

**单一职责原则**

1.数据访问--->**Dao**,数据访问,持久层--->操作数据库.

2.逻辑处理--->**Service**,业务--->调用Dao层的数据

3.接收请求 + 响应数据--->**Controller**,控制器--->调用**service**

#### 分层解耦

> 面试考察注解的细节?

高内聚	低耦合--->三层之间不会相互影响.

容器.

**IOC**--->控制反转:Inversion of control--->**对象的创建控制权由程序自身转移到外部容器**(所以叫**反转**)

​	@Component--->交给容器管理

**DI**--->依赖注入:Dependency Injection--->**容器为应用程序提供运行时所依赖的资源**.

​	@Autowired--->**默认按照类型**在IOC容器中找对应的bean对象.(总的来说还是在玩注解这块的东西)--->spring的注解

​	@Resource--->**按照名称**进行注入--->jdk的注解

​	同类型bean存在多个的话就用注解指定,或者用@Primary来设置优先级

**Bean**对象--->IOC容器中**管理和创建的对象**.

## Mybatis

> DAO层的数据库操作.

JDBC--->开销很大,麻烦.

### 数据库连接池技术

容器,这个容器负责管理数据库的连接,不会带来频繁的创建开销.

我们JDBC的连接是:

```java
Connection conn = DriverManager.getConnection(url, user, pass);
```

每执行一次这行代码，就会：

1. 打开一个 **TCP 网络连接** 到数据库；
2. 数据库进行 **身份验证**；
3. 分配资源、创建 session；
4. 返回 **Connection** 对象。

这样的话开销很大,我们放在一个连接池中就会很好,这样数据库Connection就可以进行复用.

DataSource--->连接池接口,**Hikari**(ひかり)默认的接口,快,轻量级.

Druid--->阿里巴巴的库.

### lombok技术

> ​	问题:原来接收数据的实体类可能代码非常多,比如getter,setter和toString等方法,我们使用注解解决这个问题.--->自动生成

比如这样使用:

> 编译的时候决定生成什么方法--->class文件中生成了.

```Java
import lombok.*;
//@Getter
//@Setter
//@ToString
//@EqualsAndHashCode
@Data               // 直接使用这一个注解,等效上面四个
@NoArgsConstructor  // 创建一个无参的构造
@AllArgsConstructor // 一个全参数的构造
public class User {
    // 推荐使用包装类型
    private Integer id;
    private String name;
    private short age;
    private short gender;
    private String phone;
}
```

### DELETE

```java
package com.mio.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmpMapper {
    // 根据给定的主键ID删除数据
    // 通过#{} 传递形参
    @Delete("DELETE FROM emp where id = #{id}")
    public int delete(Integer id);
}
```

实际执行的指令:

```sql
JDBC Connection [HikariProxyConnection@1284326863 wrapping com.mysql.cj.jdbc.ConnectionImpl@4e8afdad] will not be managed by Spring
==>  Preparing: DELETE FROM emp where id = ?
==> Parameters: 16(Integer)
<==    Updates: 0
```

?就是参数占位符--->**预编译SQL**

1.**性能高**--->和**compiler**一样,我们会对于SQL语句进行优化,然后编译,这个结果会放在cache内部.

> ​	这个很好理解,就是对于不一样的参数实际上是不一样的**SQL**语句,这样每次都要进行**编译 + 优化 + 缓存**,但是预编译之后,**直接替换参数**即可,所以说性能优化了.

2.web安全的问题--->**防止SQL注入**,我们在pwn里面学习过,比如 ' OR '1' = 1' 这样就完成了注入.

​	一定要参数化--->这样相当于是限制用户的输入,进行一个类型的检查,不要进行原生SQL的拼接.--->**${}就是原生SQL的拼接**.

### INSERT

```java
 // 怎么插入一条数据.传入对象,可以直接写入对象的属性
 @Insert("INSERT INTO emp(username, name, gender, image, job, entrydate, dept_id, create_time, update_time)" +
                        "VALUES (#{username}, #{name}, #{gender}, #{image}, #{job}, #{entrydate}, #{deptId}, #{createTime}, #{updateTime})")
 public void insert(Emp emp);
```

在插入数据之后,我们还要进行主键的返回.

```java
 @Options(keyProperty = "id", useGeneratedKeys = true) // 自动生成的主键值会给id属性
```

### UPDATE

基本语句的写法:

```sql
@Update("UPDATE emp SET username = #{username}, name = #{name}, gender = #{gender}, image = #{image}, job = #{job}, entrydate = #{entrydate}, dept_id = #{deptId}, update_time = #{updateTime} where id = #{id}")
    public void update(Emp emp);
```

### SELECT

查询操作.

```java
 // 根据ID进行员工查询
    @Select("SELECT * FROM emp WHERE id = #{id}")
    public Emp getByID(Integer id);
```

数据封装,属性类名和数据库查询的类名一致的时候才会自动封装,否则不会,看查询结果:

```java
Emp(id=3, username=yangxiao, password=123456, name=杨逍, gender=1, image=3.jpg, job=2, entrydate=2008-05-01, deptId=null, createTime=null, updateTime=null)
```

后面的这些就没有封装到我们的Emp对象内部.

处理方法:

> 1.直接在SQL内部给字段取别名.
>
> 2.手动封装.
>
> ```java
>   // 根据ID进行员工查询
>     // 类名不一致封装
>     @Results({
>             @Result(column = "dept_id", property = "deptId"),
>             @Result(column = "update_time", property = "updateTime"),
>     })
>     @Select("SELECT * FROM emp WHERE id = #{id}")
>     public Emp getByID(Integer id);
> ```
>
> 3.Mybatis驼峰自动命名--->直接用这个就行.
>
> ```xml
> mybatis.configuration.map-underscore-to-camel-case=true
> ```

怎么实现多种条件查询.

注意,这里还要利用concat来避免SQL的注入问题.

```java
// 条件查询
    @Select("select * from emp where name like concat('%', #{name}, '%') and gender = #{gender} and entrydate between #{begin} and #{end} order by update_time desc;")
    public List<Emp> list(String name, Short gender, LocalDate begin, LocalDate end);
```

### XML映射文件

> 不用注解来编程,直接使用XML文件,能实现更复杂的功能.
>
> 注意规范.

1.和Mapper的接口一致,并且还要在相同的包下方--->**同包同名**

2.接口限定名称一致

3.方法名一致,返回类型一致

就比如我们之前定义的select的条件查询的语句:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mio.mapper.EmpMapper">
    <select id="list" resultType="com.mio.pojo.Emp">
        select * from emp where name like concat('%', #{name}, '%') and gender = #{gender} and entrydate between #{begin} and #{end} order by update_time desc
    </select>
</mapper>
```

这里都要使用全类名,怎么找到关联的SQL--->项目下面的目录.

### 动态SQL

> 我们不能**限定条件**,对么,孩子们.

用户输入变化而产生变化的SQL.

#### if

**test属性**判断,**条件成立直接拼接SQL.**

我们直接使用很多if条件来做判断,传进来**null**就不进行拼接.

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mio.mapper.EmpMapper">
    <select id="list" resultType="com.mio.pojo.Emp">
        select * from emp where
        <if test="name != null">
            name like concat('%', #{name}, '%')
        </if>
        <if test="gender != null">
            and gender = #{gender}
        </if>
        <if test="begin != null and end != null">
            and entrydate between #{begin} and #{end}
        </if>
        order by update_time desc
    </select>
</mapper>
```

中间的and怎么处理???

加一个where标签.

```xml
 <where>
        <if test="name != null">
            name like concat('%', #{name}, '%')
        </if>
        <if test="gender != null">
            and gender = #{gender}
        </if>
        <if test="begin != null and end != null">
            and entrydate between #{begin} and #{end}
        </if>
        order by update_time desc
</where>
```

#### choose、when、otherwise

**传入什么条件就按照什么条件来寻找**,要不然就**otherwise**的条件--->比较像**switch**语句.

```xml
<select id="findActiveBlogLike"
     resultType="Blog">
  SELECT * FROM BLOG WHERE state = ‘ACTIVE’
  <choose>
    <when test="title != null">
      AND title like #{title}
    </when>
    <when test="author != null and author.name != null">
      AND author_name like #{author.name}
    </when>
    <otherwise>
      AND featured = 1
    </otherwise>
  </choose>
</select>
```

#### trim、where、set

**set + if来进行动态语句的更新.**

```xml
<update id="updateAuthorIfNecessary">
  update Author
    <set>
      <if test="username != null">username=#{username},</if>
      <if test="password != null">password=#{password},</if>
      <if test="email != null">email=#{email},</if>
      <if test="bio != null">bio=#{bio}</if>
    </set>
  where id=#{id}
</update>
```

#### foreach

批量的**CRUD**?尤其是使用到in语句的时候.

比如定义了一个这样的方法:

```java
 public void deleteByIDs(List<Integer> ids);
```

那么:

```xml
 <delete id="deleteByIDs">
        delete from emp where id in
        <foreach collection="ids" item="id" separator="," open="(" close=")">
            #{id}
        </foreach>
 </delete>
```

中间就会生成(一串数字)--->类似于一种生成式的脚本.

#### sql include

xml文件内部的复用性问题.

预先定义一个可以复用的sql语句:

```xml
<sql id="commonSelect">
        select id, username, password, name, gender, image, job, entrydate, dept_id, create_time, update_time
        from emp
</sql>
```

使用的时候直接include:

```xml
<select id="list" resultType="com.mio.pojo.Emp">
        <include refid="commonSelect"/>
        <where>
        <if test="name != null">
            name like concat('%', #{name}, '%')
        </if>
        <if test="gender != null">
            and gender = #{gender}
        </if>
        <if test="begin != null and end != null">
            and entrydate between #{begin} and #{end}
        </if>
        order by update_time desc
        </where>
</select>
```

## OSS

> 用这样的方法来进行文件存储的管理.

**Object Storage Service**--->对象存储服务.

多种文件存储问题,不要在本地进行存储.

> ​	对象（Object）是OSS存储数据的基本单元，也被称为OSS的文件。和传统的文件系统不同，Object没有文件目录层级结构的关系。

## 常见配置文件

**yml**	结构清晰,没有小标签,很好.--->主流使用

properties	键值对,分层级的很多key,会很长,所以层级结构不清晰.

xml 	标签.

基本格式:

```yml
# 定义对象或者map集合
user:
  name: Tom
  age: 20
  address: beijing

# 定义数组或者List集合
hobby:
  - java
  - C
  - py
  - cobol
  - scheme
  - cpp
```

注意数据前或者层级分割都使用的是空格.

我们看一个简单的Spring的yml配置文件:

```yml
spring:
  # 数据库连接信息
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/tlias
    username: root
    password: 331979248
  servlet:
    # 文件上传的选项
    multipart:
      max-file-size: 10MB
      max-request-size: 100MB

# Mybatis的基本配置
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: on

# 阿里云oss配置
aliyun:
  oss:
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
    accessKeyId: LTAI5t9HK3XktRa9D8y8zL7e
    accessKeySecret: jErkFQa2INzG3ZgwJWL4tkeKyaIqFO
    bucketName: nunotabashinobu

```

## 登陆认证问题

直接根据用户名和密码查询看数据库里面有没有.

当客户端发过来请求的时候,我们首先要"校验",这个用户是否是一个已经登陆的用户.

http无状态.

### 会话技术

> 浏览器和Sever之间的一次的连接就是会话,包含多次请求和响应.

**会话跟踪**,要识别多次的请求是否来自于一个相同的浏览器.

**三种技术:**

1.客户端会话跟踪 **cookie**

2.服务端会话跟踪 **session**

3.令牌技术

#### cookie

http协议直接支持

​	浏览器请求--->Server直接生成cookie返回set-cookie请求头--->浏览器本地保存这个cookie--->之后发送请求的时候会携带cookie(请求头)--->Server验证是否有这个cookie.

> 1.不安全
>
> 2.移动端不能使用
>
> 3.不能**跨域**--->前后端没有部署在一台服务器上面

#### session

**session** ID--->直接基于**cookie**.

> **hashcode**直接存放在**Server**内部,比较安全.
>
> **1.session**不能在每个**Server**上都进行维护.(**服务器的集群**)
>
> **2.Server**要维护很多的连接,是一种负担.

#### JWT令牌技术

**JSON** Web Token

> Server不存储状态,只进行解析看是否合法,我们只会在Server存放一个key.

简洁 自包含

Server收到请求,返回JWT令牌,后面Client每次请求都会携带Token来在Server进行校验.

![image-20251019215955499](../../mio/static/img/image-20251019215955499.png)

JWT令牌会对于信息进行base64编码,并且根据内容 + 密钥来进行数字签名校验.

比如一个数字签名的生成:

```java
 /**
     * 生成Jwt Token
     */
    @Test
    public void testGenJwt(){
        Map<String, Object> claims = new HashMap<>();
        claims.put("nunotaba", 100);
        claims.put("shinobu", 22);


    String jwt = Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, "nunotaba") // 签名算法
                .setClaims(claims) // 设置自定义的payload
                .setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000)) // 设置有效期
                .compact();

        System.out.println(jwt);

    }
```

```json
eyJhbGciOiJIUzI1NiJ9.eyJudW5vdGFiYSI6MTAwLCJzaGlub2J1IjoyMiwiZXhwIjoxNzYwODg2NTYzfQ.li9qVm3_GsI9CfoPkLO_wn44iQ1le6Hb_y15Q0j_8iw
```

分成三段.前两段都能**base64**解码,**第一段**是采用的**加密算法类型**,第二段是**payload**.

我们解析一个**JWT**令牌:

```java
 /**
     * 解析一个JWT令牌
     */
    @Test
    public void testParseJwt(){
    Claims claims = Jwts.parser()
                .setSigningKey("nunotaba")
                .parseClaimsJws("eyJhbGciOiJIUzI1NiJ9.eyJudW5vdGFiYSI6MTAwLCJzaGlub2J1IjoyMiwiZXhwIjoxNzYwODg2NTYzfQ.li9qVm3_GsI9CfoPkLO_wn44iQ1le6Hb_y15Q0j_8iw")
                .getBody();

        System.out.println(claims);
}
```

**过期**(可能加入时间戳了)或者**篡改令牌**.

### Filter 过滤器

对于来自客户端的请求进行拦截的操作.--->强制登陆校验.

执行流程:

1.Filter放行了之后,还是会回到Filter中去,然后执行放行之后的逻辑.

2.过滤器chain,一个web应用设置多个过滤器,就像闯关一样.

> 过滤器的顺序直接按照**自然类名的字符串顺序**进行排序.

**登陆校验**

直接在Filter中对于JWT Token进行校验.

比如我们来看一个简单的web Filter的实现:

```java
@Slf4j
@WebFilter(urlPatterns = "/*")
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
```

### Intercepter 拦截器

​	过滤器先拿到,因为Filter不是Spring Framework的一部分,给DispatcherServlet,接着给Interceptor拦截(这是Spring环境中的一部分),然后给Controller处理请求.

**拦截器的逻辑**也是类似的:

```java
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
```

同时我们要在一个配置类里面注册拦截器:

```java
@Configuration  // 表明当前类是一个配置类
public class WebConfig implements WebMvcConfigurer {
    // 注册一个拦截器

    @Autowired  // 先注入这个拦截器
    private LoginCheckInterceptor loginCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 配置拦截的路径
        registry.addInterceptor(loginCheckInterceptor).addPathPatterns("/**").excludePathPatterns("/login");
    }
}
```

### 异常处理

全局**异常处理器**

> 先学个皮毛.
>













































