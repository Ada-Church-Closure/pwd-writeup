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

> ​	相当于就是每个CPU负责一个线程,然后都有自己独立的变量,分配在内存里面,但是不在hugepage里面,这部分变量在内存里面是连续的(为了性能),这样的独立变量有什么实际作用?
>
> ​	本地数据的统计,来进行一些调整的操作,但比如你如果要统计一共有多少数据包,那就要遍历从而聚合数据对么.

#### 实现/设计原理

lcore变量的设计思想.

buffer:这些变量就是在heap上分配的.--->分配失败就直接失败,用户不用进行错误处理这样的操作.

这些变量放在一组这样的结构体内部:

```C
struct lcore_var_buffer {
	char data[RTE_MAX_LCORE_VAR * RTE_MAX_LCORE];
	struct lcore_var_buffer *prev;
};
```

那么你也能注意到,这些变量之间通过**链表**来连接,用来最终进行释放的操作.

```C
static struct lcore_var_buffer *current_buffer;

/* initialized to trigger buffer allocation on first allocation */
static size_t offset = RTE_MAX_LCORE_VAR;
```

用来跟踪buffer中已经分配了多少个字节.

handle

​	Upon lcore variable allocation, the lcore variables API returns an opaque *handle* in the form of a pointer. The value of the pointer is `buffer->data + offset`.

这里要找到某个变量,就相当于是指针 + 偏移量.

要把一个baseptr和某个lcore的变量联系起来是比较直接的:

```C
static inline void *
rte_lcore_var_lcore(unsigned int lcore_id, void *handle)
{
	RTE_ASSERT(handle != NULL);
	return RTE_PTR_ADD(handle, lcore_id * RTE_MAX_LCORE_VAR);
}
```

直接把baseptr和偏移量相加即可.

内存布局:

lcore变量/index静态数组

比如我们给两个core各分配两种不同的struct变量--->注意就是每个lcore的变量内存布局是相同的:

RTE_MAX_LCORE--->2 最多有两个lcore.

RTE_MAX_LCORE_VAR--->64 lcore 变量最多有64个字节.

```C
/* -- Lcore variables -- */
/* rte_x.c */
struct x_lcore
{
    int a;
    char b;
};
static RTE_LCORE_VAR_HANDLE(struct x_lcore, x_lcores);
RTE_LCORE_VAR_INIT(x_lcores);

/* rte_y.c */
struct y_lcore
{
    long c;
    long d;
};
static RTE_LCORE_VAR_HANDLE(struct y_lcore, y_lcores);
RTE_LCORE_VAR_INIT(y_lcores);
```

那么实际上的内存布局就是这样的:

![image-20250907154608007](../../../../mio/static/img/image-20250907154608007.png)

可以看到内存中是连续的,性能会很好.

Lcore Id Index Static Array---lcore id的一个静态数组,这相当于是另外一种分配的方式.

![image-20250907162231886](../../../../mio/static/img/image-20250907162231886.png)

注意这样可能会造成内存碎片.

能够避免CPU硬件prefetch的问题,总之很好地提升了性能.

替代方案就是静态数组或者是线程本地存储.

TLS--->变量和线程的生命周期就是相关的.

> ​	注意,lcore变量的处理方式类似于传统的多线程,但是也不一样,从内存的视角来看是类似的,但是生命周期和共享的方式肯定不同.
>
> ​	高速创建并且销毁线程就会导致性能下降.

### 2.Memory Pool Library

关于内存池的内容.

基于ring,做对象的分配.

每个core会分配一小部份cache--->防止大量的CAS,提升性能.

![image-20250908083830730](../../../../mio/static/img/image-20250908083830730.png)

这里的cache其实就是类似于一个指针数组.

handler--->有硬件外设的情况下.

### 3.Packet Library

我们先理解是干什么的:

|                  | Mempool Library (内存池库)                                   | Mbuf (报文缓冲区)                                            |
| :--------------- | :----------------------------------------------------------- | ------------------------------------------------------------ |
| **角色定位**     | **内存管理者**：负责高效、安全地“生产”和“回收”内存对象       | **内存使用者**：是内存池“生产”出来的对象，用于承载和描述数据包 |
| **主要目的**     | 预分配和高效管理**固定大小**的内存对象，减少动态分配开销和碎片 | 存储数据包的**元数据**和**实际数据**，支持零拷贝操作和链式结构 |
| **本质**         | 一个**对象池**（Object Pool）或**内存分配器**                | 一个**数据结构**，是内存池中分配出来的一个具体对象           |
| **关键操作**     | 创建内存池 (`rte_mempool_create`)、批量获取/归还对象 (`rte_mempool_get_bulk`) | 获取数据指针 (`rte_pktmbuf_mtod`)、克隆 (`rte_pktmbuf_clone`)、释放 (`rte_pktmbuf_free`) |
| **性能设计重点** | 无锁环形队列、每核缓存（Local Cache）、NUMA 亲和性           | 缓存行对齐、热点字段分离、硬件卸载标志位                     |
| **关系**         | 是 **mbuf 的工厂和仓库**                                     | 是 **mempool 生产出来的产品**                                |

​	mempool进行内存的分配,生产出来mbuf给我们存放数据包使用--->实际上可以存放任意的我们设计好的数据结构.

metadata直接嵌入packetbuf内部.

这是一个mbuf承载一个数据包的情况:

![image-20250908091011039](../../../../mio/static/img/image-20250908091011039.png)

如果一个数据包过于大,比如jumbo frame,我们就会使用链表的形式,采用多个数据包:

(注意对于链式的mbuf来说,只有第一个mbuf携带元数据)![image-20250908091106032](../../../../mio/static/img/image-20250908091106032.png)

Buffers Stored in Memory Pools

mbuf中存放了自己在mempool中的地址,当free的时候,就会return到原来的mempool中去.

direct和indirect buffer--->当需要复制或者分段的时候非常方便,就相当于存放了一个指针.

attatch的方法把一个buffer变成indrectbuffer,附加到direct buffer上面去.

每增加一个,这个direct buffer就会增加一个引用计数.

直接使用rte_pktmbuf_clone()这个方法最好.

### 4. Multi-process Support

怎么处理这样的多进程的情况?

- primary processes, which can initialize and which have full permissions on shared memory
- secondary processes, which cannot initialize shared memory, but can attach to pre- initialized shared memory and create objects in it.

两种进程,主进程和辅助进程.

























































































































