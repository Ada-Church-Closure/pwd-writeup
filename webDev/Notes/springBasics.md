# Spring

## 事务管理

### 基本概念

> 也是mysql中的事务,就是一组原子的流程.(同时成功或者同时失败)

比如在删除某些数据的时候,我们还要删除一些关联的数据.

> 就是说,这两个操作**理应是原子**的,但是中间如果抛了异常,就会有问题.

```java
 @Override
    public void delete(Integer id) {
        deptMapper.deleteByID(id);
        empMapper.deleteByDeptId(id);
    }
```

> 加 **@Transactional** 注解在
>
> ​	**Service**层的
>
> ​		**多次访问数据**的
>
> ​			**增删改的方法**上.(理解和sql是类似的)

​	那直接就是一个原子方法,一旦失败就会进行 **rollback**,也就是**事务回滚**(默认只有**RuntimeException**才会进行回滚的操作).--->这样会不全面

> 使用**rollbackFor**来指定的话,让任何事务都进行回滚的操作.

```java
@Transactional(rollbackFor = Exception.class)
    @Override
    public void add(Dept dept) {
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.insert(dept);
        }
```

**@propagation**

> 当一个事务方法调用另一个事务方法的时候,发生什么?

**REQUIRED**--->加入到一个已有的事务,全部回滚(比如插入一段日志,肯定不能用这个).

**REQUIRES_NEW**--->自己独立出来新的事务,调用方法的回滚不会影响到自己.

## AOP

​	**Aspect Oriented Programming**:面向**切面**编程--->面向特定方法的编程,比如针对一个类的所有方法有相同的一套操作.

​	底层就是**动态代理**机制.

> 思考:动态代理的实现,复习**反射**机制.

> 思考一下,transaction也是这样实现的.

#### 核心概念:

**1.JoinPoint** 连接点.

可以被AOP控制的方法.

**2.Advice** 

抽取出来的重复的功能.

**3.PointCut** 

匹配连接点的条件,比如正则匹配,我们什么时候进行切入的操作.

![image-20251021095519707](../../mio/static/img/image-20251021095519707.png) 

其实注入的时候,本身注入的就是一个**代理对象**.

#### 通知类型:

> 定义**环绕的类型以及进行切点的抽象.**
>
> @After:即使最终会抛出异常也会进行执行.
>
> @AfterReturning:只有正常返回才会执行.
>
> @AfterThrowing:只有抛出异常才会执行.

```java
@Slf4j
@Aspect
@Component
public class MyAspect1 {
    
    // 直接定义这个切点表达式,来进行抽象.
    @Pointcut("execution(* com.itheima.service.impl.DeptServiceImpl.*(..))")
    private void pointcut(){}

    // 定义环绕的方法
    @Before("pointcut()")
    public void before(){
        log.info("Before method will be executed......");
    }
    
    @After("pointcut()")
    public void after(){
        log.info("After the method was executed......");
    }
    // 这个是最重要的方法.
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{
        log.info("Around before...");
        Object result = proceedingJoinPoint.proceed();
        log.info("Around after...");
        return result;
    }
}
```

#### 通知顺序:

> 有**多个切面类对于一个的方法进行了切入**,那么通知执行的顺序?

自然状态下只和**类名的顺序**相关.

或者使用**@Order**注解,那么环绕的执行顺序就和stack一样,先执行的前面的方法,最后才会执行后面的方法.

#### 切入点表达式:

"execution + 匹配类型"--->匹配范围不能过大,影响匹配的效率(缩小范围).

通配符 * --->只能对于一个参数进行匹配,或者一部分的包名或者类名.

.. --->通配所有的方法形参.

|| --->可以组合

![image-20251021110203221](../../mio/static/img/image-20251021110203221.png)

**@annotation**--->匹配**标有特定注解**的方法.

我们可以自己加注解,确定给加了这个注解的方法来切入.

#### 连接点:

对于**连接点**(其实就是**切入的方法**)进行**抽象**,获取一些**连接点的信息**.

因为是动态代理,还是通过类似于反射的手法来获取参数:

```java
@Slf4j
@Component
@Aspect
public class MyAspect2 {
    // 直接定义这个切点
    @Pointcut("execution(* com.itheima.service.impl.DeptServiceImpl.*(..))")
    private void pointcut(){}


    @Before("pointcut()")
    public void before(JoinPoint joinPoint){
        log.info("my8, before method will excute......");
    }

    // 注意环绕通知使用的类型就不一样了
    // 用joinPoint来获取一些信息
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        log.info("my8 around before");

        // 1.获取类名
        String className = joinPoint.getTarget().getClass().getName();
        log.info("ClassName: {}", className);

        // 2.方法签名
        String methodName = joinPoint.getSignature().getName();
        log.info("MethodName: {}", methodName);

        // 3.方法传入的参数
        Object[] args = joinPoint.getArgs();
        log.info("Args: {}", Arrays.toString(args));

        // 4.直接放行
        Object result = joinPoint.proceed();

        // 拿到这个返回值
        log.info("retValue: {}", result);
        log.info("my8 around after");

        return result;
    }
}
```

## 优先级

properties > yml > yaml

**使用yml**即可.

还有命令行参数和VM 的系统属性.

其中命令行参数最高 > VM的属性 > 其余的配置文件.

## Bean管理

### 获取Bean对象

```java
@Autowired
    // 注入一个IOC容器对象
    private ApplicationContext applicationContext;

    @Test
    public void testGetBean(){
        // 根据Bean的名称获取
        DeptController bean1 = (DeptController) applicationContext.getBean("deptController");
        System.out.println(bean1);

        // 直接根据class获取
        DeptController bean2 = applicationContext.getBean(DeptController.class);
        System.out.println(bean2);

        // 根据名称及类型获取
        DeptController bean3 = applicationContext.getBean("deptController", DeptController.class);
        System.out.println(bean3);

    }
com.itheima.controller.DeptController@2d5ae78e
com.itheima.controller.DeptController@2d5ae78e
com.itheima.controller.DeptController@2d5ae78e
```

> 那么可以看到这个Bean对象**默认是一个单例**的对象.
>
> 容器创建的时候,就会直接初始化.
>
> 但是@Lazy--->懒加载,**第一次使用的时候**,才会进行**初始化**的操作.

### 作用域

> 大多数情况下,单例的Bean对象就是可以使用的.
>
> 不要把数据库的持久内存存放在一个单例的Bean中作为一个成员变量,线程不安全的.

@Scope

> 研究**作用域**,就是我们考虑**什么时候创建一个新的Bean对象**.

@Scope("prototype")

来配置**作用域**.

### 第三方Bean

**@Bean**

> 很简单,自己写的用**@Component**,第三方的用**@Bean**

就是想把别人库里面的class当成自己的Bean对象怎么处理.

```java
@Configuration // 配置类
public class CommonConfig {
    // 声明一个第三方的Bean对象
    @Bean   // 交给IOC容器管理
    public StAXStreamReader stAXStreamReader(){
        return new StAXStreamReader();
    }
}
```































