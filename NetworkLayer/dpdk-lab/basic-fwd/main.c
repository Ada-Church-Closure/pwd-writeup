// 1.基本的在网卡之间转发数据包的程序
// 这是一个基本的转发的dpdk的程序,我们努力搞清楚其中的每个细节,作为入门的学习

#include <stdint.h>
#include <stdlib.h>
#include <inttypes.h>

// 这里都是dpdk的核心库文件,我们先来细节的了解一下.
// 环境抽象层,这是准备的工作

/*
功能：Environment Abstraction Layer（环境抽象层）
负责DPDK的初始化工作，包括设置Hugepages、绑定CPU核、初始化内存和PCI设备等。
典型函数：
rte_eal_init(int argc, char **argv)：初始化DPDK环境，必须在调用其他DPDK函数前执行。
rte_lcore_id()：获取当前执行线程绑定的逻辑核ID。
rte_socket_id()：获取当前执行线程所属的NUMA节点ID。
作用：它像操作系统和硬件之间的桥梁，屏蔽了平台差异，让DPDK代码可以跨平台高效运行。
*/
#include <rte_eal.h>

/*
功能：以太网设备驱动接口 ethernet device interface
提供对网络接口卡（NIC）进行配置和操作的API。
典型功能：
配置端口参数（速率、队列数、RSS等）
启动和停止端口
收发数据包
典型函数：
rte_eth_dev_configure()：配置网卡端口参数。
rte_eth_rx_queue_setup() 和 rte_eth_tx_queue_setup()：设置接收和发送队列。
rte_eth_dev_start()：启动端口。
rte_eth_rx_burst() 和 rte_eth_tx_burst()：批量接收和发送数据包。
作用：直接与硬件交互，管理网卡收发行为。
*/
#include <rte_ethdev.h>

/*
功能：高精度计时和周期计数
提供获取CPU时钟周期数的接口，用于高精度时间测量和延迟计算。
典型函数：
rte_get_timer_cycles()：获取当前CPU时钟周期计数。
rte_get_timer_hz()：获取时钟频率，结合上面函数可以计算时间。
延迟函数如 rte_delay_us() 进行微秒级别的忙等待。
作用：实现高性能计时，避免系统调用开销，提高精度。
*/
#include <rte_cycles.h>

/*
功能：逻辑核心（CPU核）管理
提供当前线程和逻辑核（lcore）的相关信息和控制。
典型函数：
rte_lcore_id()：获取当前线程绑定的逻辑核ID。
rte_lcore_count()：获取可用逻辑核数量。
线程亲和性设置等。
作用：支持多核并行和线程与CPU核的绑定，充分利用多核优势。
*/
#include <rte_lcore.h>

/*
功能：数据包缓冲区（mbuf）管理
提供高效的内存管理和数据包缓存结构。
mbuf 是DPDK里数据包的基本单位，封装了数据缓冲区、元数据（如长度、端口号等）。
典型函数：
rte_pktmbuf_pool_create()：创建内存池，用于分配mbuf。
rte_pktmbuf_alloc() 和 rte_pktmbuf_free()：分配和释放mbuf。
访问和修改数据包内容的函数。
作用：高性能零拷贝数据包管理核心,简单来说,这里就是存放数据包的地方,并且是零拷贝,性能很高.
*/
#include <rte_mbuf.h>

// 一些宏定义
// 注意概念:分配的mbuf pool是一片大内存池,实际存放了数据包的内容,由rte_pktmbuf_pool_create()创建.
// 而所谓的收发队列实际上是一些指针,指向这些mbuf,rte_eth_rx_queue_setup()、rte_eth_tx_queue_setup()来创建.

// 过程:
// 当收到一个包时，RX ring 会从 mbuf pool 中取出一个空的 mbuf 指针交给网卡。
// 网卡把包的数据写入这个 mbuf,然后通知上层“我收到了”。
// 同理,发送一个包时,你构造好 mbuf,将它的指针交给 TX ring,网卡随后异步发送。

// 接收和发送的环形缓冲区的大小,最多容纳1024个未处理的数据包指针
#define RX_RING_SIZE 1024
#define TX_RING_SIZE 1024

// 内存缓冲区的大小为8191,所有数据包都在这里处理
// 也就是有8191个mbuf,大多数情况下一个mbuf对应一个data packet,如果太大的话就可能会分段发送
#define NUM_MBUFS 8191

// 每个lcore(逻辑核)能缓存的mbuf的数量为250
// 这里就是缓存的基本概念,减少锁竞争
#define MBUF_CACHE_SIZE 250

// 一次收包或者发包的数据包的量是32
#define BURST_SIZE 32

/*
* @function 初始化网卡端口port
* @params port:网卡端口编号,dpdk给网卡从0开始编号
* @params *mbuf_pool:指向内存池的指针,供RX队列来接收数据包使用
*/
static inline int
port_init(uint16_t port, struct rte_mempool *mbuf_pool)
{
    // port_conf:端口的配置结构
    struct rte_eth_conf port_conf;
    
    // 每个port上面的收发队列的数量
    const uint16_t rx_rings = 1, tx_rings = 1;

    // 每个队列的ring的大小
    uint16_t nb_rxd = RX_RING_SIZE;
    uint16_t nb_txd = TX_RING_SIZE;

    // retval的作用是获取每次调用的功能函数的返回值,如果失败的话就直接终止程序
    int retval;
    uint16_t q;

    // 获取 网卡能力 的信息
    struct rte_eth_dev_info dev_info;

    // tx的一些特定的配置项
    struct rte_eth_txconf txconf;

    //检查端口是否是有效的端口
    if(!rte_eth_dev_is_valid_port(port))
        return -1;

    // 必须的初始化步骤,把默认的配置结构清空
    memset(&port_conf, 0, sizeof(struct rte_eth_conf));    

    // 获取这个port的硬件能力的相关信息
    retval = rte_eth_dev_info_get(port, &dev_info);

    // 检查是否能够正确获取网卡的信息
    if(retval != 0){
        printf("OH!!!TYPESHIT,we got some problems get device (port %d) info: %s", 
        port,
        strerror(-retval));
        return retval;
    }

    // 这是一种性能优化,启用MBUF_FAST_FREE
    // 允许mbuf在发送完成之后快速地释放
    if(dev_info.tx_offload_capa & RTE_ETH_TX_OFFLOAD_MBUF_FAST_FREE)
        port_conf.txmode.offloads |= 
            RTE_ETH_TX_OFFLOAD_MBUF_FAST_FREE;
        

    /* 配置网卡设备 */
    // 配置队列TX, RX

    // 1.我们告诉这个网卡使用 几个队列 并且采用怎样的 offload 来配置
    retval = rte_eth_dev_configure(port, rx_rings, tx_rings, &port_conf);
    if(retval != 0)
        return retval;
    
    // 2.调整这个队列的大小(也就是ring的大小),不一定会按照我们给定的数字来
    retval = rte_eth_dev_adjust_nb_rx_tx_desc(port, &nb_rxd, &nb_txd);
    if(retval != 0)
        return retval;

    // 3.配置这个RX的接收队列:给这个接收队列分配资源,指定ring的大小,NUMA节点ID,和mbuf
    for(q = 0; q < rx_rings; ++q){
        retval = rte_eth_rx_queue_setup(port, q, nb_rxd, rte_eth_dev_socket_id(port),NULL, mbuf_pool);
        if(retval < 0){
            return retval;
        }
    }

    // 4.配置TX发送队列,首先是设置前面可能加上的offload优化参数
    txconf = dev_info.default_txconf;
    // 这里在前面设置过一次
    txconf.offloads = port_conf.txmode.offloads;
    // 注意这里就不用交mbuf的地址,我们是构造好一个mbuf,直接把指针给tx队列.
    for(q = 0; q < tx_rings; ++q){
        retval = rte_eth_tx_queue_setup(port, q, nb_txd, rte_eth_dev_socket_id(port), &txconf);
        if(retval < 0){
            return retval;
        }
    }
    
    /* 启动网卡设备 */
    retval = rte_eth_dev_start(port);
    if(retval < 0){
        return retval;
    }

    /* 打印网卡的MAC地址 */
    // 获取网卡的MAC地址
    struct rte_ether_addr addr;
    retval = rte_eth_macaddr_get(port, &addr);

    if(retval != 0)
        return retval;

    // %02" PRIx8 ---> %02x 打印固定的整数宽度
    // 后面的宏函数意思是把addr这个结构体的6个字节按照顺序展开,然后打印出来,就是前面的每个%02x
    printf("Port %u MAC address: %02" PRIx8 " %02" PRIx8 " %02" PRIx8
                                " %02" PRIx8 " %02" PRIx8 " %02" PRIx8 "\n", 
                            port, RTE_ETHER_ADDR_BYTES(&addr));
    
    // 开启混杂模式--->网卡会接收所有MAC地址来的数据包,同时也包括自己
    retval = rte_eth_promiscuous_enable(port);
    if(retval != 0){
        return retval;
    }
    return 0;
}

/* 实现核心的转发功能 */
// 从某个端口接收一个数据包,接着转发到另一个端口上面去
// 在一个lcore上面进行 无限循环 的数据包转发--->L2转发的基本逻辑

// dpdk的宏,表示这个函数不会返回
static __rte_noreturn void
lcore_main(void)
{
    uint16_t port;

    // NUMA警告的部分,如果网卡和当前访问的thread不在一个NUMA节点,证明是跨节点的访问,可能会导致性能的下降
    // 遍历所有可用的网口
    RTE_ETH_FOREACH_DEV(port)
        if(rte_eth_dev_socket_id(port) >= 0 &&
            rte_eth_dev_socket_id(port) != (int)rte_socket_id())
            printf("WARNING!!! port %u is on remote NUMA node to polling thread...\n", port);

    // 打印当前core的信息
    // rte_lcore_id()获取当前的逻辑核心的编号
    printf("\nCore %u forwarding packets. [Ctrl + C to quit]\n", rte_lcore_id());     

    // polling模式,无限循环处理数据包,也是核心功能的实现
    for(;;){
        // 从一个port接收数据包并且把他发送给一个相对的port
        
        // 遍历每个port
        RTE_ETH_FOREACH_DEV(port){
            // 从RX队列接收BURST_SIZE个数据包,这是一组结构体指针数组
            struct rte_mbuf *bufs[BURST_SIZE];

            // 返回实际接收到的数据包的数量
            const uint16_t nb_rx = rte_eth_rx_burst(port, 0, bufs, BURST_SIZE);

            // unlikely 性能优化的宏 表明CPU这个分支很少执行,用于分支预测
            if(unlikely(nb_rx == 0)){
                continue;
            }

            // 发送到这个配对的端口去, TX
            // 那么这里的TX就是 1->0 0->1, 改变低位从而有相邻配对的效果
            const int16_t nb_tx = rte_eth_tx_burst(port ^ 1, 0, bufs, nb_rx);
            
            // 如果数据包没有发完,我们要清空这里的内存,防止内存泄露
            if(unlikely(nb_tx < nb_rx)){
                for(uint16_t buf = nb_tx; buf < nb_rx; ++buf){
                    rte_pktmbuf_free(bufs[buf]);
                }
            }
        }
    }

}


// main,我们一般在main函数里面进行环境初始化,内存分配和网口初始化
int 
main(int argc, char* argv[])
{
    struct rte_mempool *mbuf_pool;
    unsigned nb_ports;
    uint16_t portid;

    // 初始化EAL环境:EAL内存分配,核心绑定等
    int ret = rte_eal_init(argc, argv);
    if(ret < 0){
        rte_exit(EXIT_FAILURE, "Error with the EAL initialization!!!\n");
    }

    // rte_init会消耗掉前面的参数,这里要进行更新
    argc -= ret;
    argv += ret;

    /* 检查网口的数量 */
    // 有多少个空闲的网口
    nb_ports = rte_eth_dev_count_avail();
    if(nb_ports < 2 || (nb_ports & 1)){
        rte_exit(EXIT_FAILURE, "Error:you must have even ports that is not ZERO!!! BRO!!!\n");
    }

    /* 创建mbuf内存池 */
/*
* rte_pktmbuf_pool_create()：创建 mbuf 池，每个 mbuf 用来存储一个数据包
* 参数解释：
* "MBUF_POOL"：池的名字
* NUM_MBUFS * nb_ports：总 mbuf 数量（根据端口数扩大),每个端口上面都有NUM_MBUFS的内存?
* MBUF_CACHE_SIZE：每个 lcore 缓存多少 mbuf--->涉及到缓存原理
* 0：私有数据空间大小（我们不需要）
* RTE_MBUF_DEFAULT_BUF_SIZE：默认数据包大小（2048）
* rte_socket_id()：NUMA 优化，分配内存在当前 socket 上
*/
    mbuf_pool = rte_pktmbuf_pool_create("MBUF_POOL", NUM_MBUFS * nb_ports, MBUF_CACHE_SIZE, 0, RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());
    
    // 初始化失败
    if(mbuf_pool == NULL){
        rte_exit(EXIT_FAILURE, "Cannot create mbuf pool!!!\n");
    }

    // 初始化所有的端口
    RTE_ETH_FOREACH_DEV(portid)
        if(port_init(portid, mbuf_pool) != 0)
            rte_exit(EXIT_FAILURE, "Cannot init port %"PRIu16 "\n", portid);
    
    
    // 检查逻辑核心的数量
    if(rte_lcore_count() > 1){
        printf("BRO!, too many lcores are enabled, ONLY one can be used!!!\n");
    }

    // 进入循环.
    lcore_main();

    rte_eal_cleanup();

    return 0;
}
