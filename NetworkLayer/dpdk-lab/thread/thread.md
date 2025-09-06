# 多线程/数据包处理

在了解了简单的转发的机制之后,我们来理解多线程的情况.

收到的包需要处理,比如加解密,完整性验证,窗口验证的情况,对比单线程和多线程之间的区别.

还是读文档.

这里我们还是从一些比较基础的概念入手:

## 1.内存管理

### 1. Lcore Variables

#### 概述

Lcore变量:每个EAL线程和注册的非EAL线程保存一个唯一的值.

variable Handle---就是一个不透明的指针,用来分配和访问这个变量的值.

Allocation:

​	使用 `RTE_LCORE_VAR_ALLOC`或 `RTE_LCORE_VAR_INIT`宏来为 Lcore 变量分配存储空间并初始化句柄,分配操作通常在**模块初始化时**进行，但也可以在任何时候进行。

Lcore 变量的**生命周期**与创建它的线程**无关**,只有在最后clean的时候才会结束.

每个 Lcore 变量都拥有 `RTE_MAX_LCORE`个值，每个可能存在的 lcore id 都对应一个值。

从 Lcore 变量**创建的那一刻起**，在整个 EAL 的生命周期内（即直到调用 `rte_eal_cleanup()`之前），都可以访问该 Lcore 变量的所有值。

Lcore 变量**不需要被释放，并且也无法被释放**。



变量可以被其他线程访问,但是只能被owner频繁读写.

使用原子读写的方式防止发生错误.

这个变量通常是一个结构体.(不要对于这个结构体进行内存对齐的操作)

变量默认初始化为0,并且不再hugepages中.

我们来看一个例子:

```C
// 创建这样的一个结构体的类型,每个核心都要维护这样的一个数据结构
struct foo_lcore_state {
        int a;
        long b;
};

// 创建一个handle
static RTE_LCORE_VAR_HANDLE(struct foo_lcore_state, lcore_states);

long foo_get_a_plus_b(void)
{
    // 使用VAR这个宏,相当于是可以快捷地访问当前执行线程自己持有的变量
        const struct foo_lcore_state *state = RTE_LCORE_VAR(lcore_states);
        return state->a + state->b;
}

RTE_INIT(rte_foo_init)
{
    // 这个宏直接为每个lcore分配内存并且初始化handle
        RTE_LCORE_VAR_ALLOC(lcore_states);

        unsigned int lcore_id;
        struct foo_lcore_state *state;
        RTE_LCORE_VAR_FOREACH(lcore_id, state, lcore_states) {
                /* initialize state */
            	/* 这里,你就可以对于lcore的变量进行一些初始化的操作 */
        }

        /* other initialization */
}
```

> ​	相当于就是每个CPU负责一个线程,但是都有自己独立的变量,分配在内存里面,但是不在hugepage里面,但是在内存里面是连续的(为了性能),这样的独立变量有什么实际作用?
>
> 本地数据的统计,来进行一些调整的操作,但比如你如果要统计一共有多少数据包,那就要遍历从而聚合数据对么.

#### 实现/设计原理























































































