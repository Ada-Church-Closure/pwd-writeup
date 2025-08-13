// 2.这个程序是计算在应用dpdk程序的时候,计算两个数据包之间发送的延迟(简单来说)
// 核心就是回调函数定义在port上面

#include <stdalign.h>
#include <stdint.h>
#include <stdlib.h>
#include <inttypes.h>
#include <getopt.h> // 命令行参数解析

#include <rte_eal.h>
#include <rte_ethdev.h>
#include <rte_cycles.h>
#include <rte_lcore.h>
#include <rte_mbuf.h>

// 动态字段扩展,自定义元数据(比如时间戳?)
#include <rte_mbuf_dyn.h>

#define RX_RING_SIZE 1024   
#define TX_RING_SIZE 1024   
#define NUM_MBUFS 8191     
#define MBUF_CACHE_SIZE 250 
#define BURST_SIZE 32

// 硬件的动态时间戳
// -1是字段还没有注册
static int hwts_dynfield_offset = -1;

// 时间戳的指针,指向一个结构体中时间戳的部分
// 注意这里使用的是硬件的时间戳
static inline rte_mbuf_timestamp_t* 
hwts_field(struct rte_mbuf* mbuf)
{
    // 用来提取rte_mbud中我们自己注册的时间戳字段
    // 就相当于做指针的偏移
    return RTE_MBUF_DYNFIELD(mbuf, hwts_dynfield_offset, rte_mbuf_timestamp_t*);
}

// use alias timestamp counter refer to uint64_t
// 时间戳的类型
typedef uint64_t tsc_t;
static int tsc_dynfield_offset = -1;
static inline tsc_t*
tsc_field(struct rte_mbuf* mbuf)
{
    return RTE_MBUF_DYNFIELD(mbuf, tsc_dynfield_offset, tsc_t*);
}

// 使用说明的字符串
static const char usage[] = "%s EAL_ARGS -- [-t]\n";

// 延迟统计的结构体
static struct {
    uint64_t total_cycles;
    uint64_t total_queue_cycles;
    uint64_t total_pkts;
} latency_numbers;

// 是否启用硬件时间戳的功能
int hw_timestamping;

// 时间戳转换常量
// 硬件时间戳来源于网卡,软件时间戳来源于CPU的rte_rdtsc(),我们要进行转换的操作
#define TICKS_PER_CYCLE_SHIFT 16
static uint64_t ticks_per_cycle_mult;

/* 实现RX/TX两个回调函数,就是把函数名称作为参数进行传递,在port上进行调用 */

// RX回调函数,记录数据包到达的时间,并且存储到每个pkts的字段里面去
// pkts 指向数据包数组的指针
// nb_pkts  收取到的最大数据包的数量
// __rte_unused 表示没有使用这个参数---dpdk特有的方式
static uint16_t
add_timestamps(uint16_t port __rte_unused, uint16_t qidx __rte_unused, struct rte_mbuf **pkts, uint16_t nb_pkts, uint16_t max_pkts __rte_unused, void*_ __rte_unused)
{
    uint64_t now = rte_rdtsc();
    for(uint64_t index = 0; index < nb_pkts; ++index)
        *tsc_field(pkts[index]) = now;
    return nb_pkts;
}

// TX回调函数,这个时候我们要计算延迟
static uint16_t
calc_latency(uint16_t port, uint16_t qidx __rte_unused, struct rte_mbuf **pkts, uint16_t nb_pkts, void *_ __rte_unused)
{
    uint64_t cycles = 0;
    uint64_t queue_ticks = 0;
    uint64_t now = rte_rdtsc();
    uint64_t ticks;
    
    // 如果启用-t参数,读取硬件网卡的时钟
    if (hw_timestamping)
        rte_eth_read_clock(port, &ticks);
    
        // 计算软件处理的延迟
    for(uint64_t index = 0; index < nb_pkts; ++index){
        cycles += now - *tsc_field(pkts[index]);
        if(hw_timestamping){
            queue_ticks += ticks - *hwts_field(pkts[index]);
        }
    }

    latency_numbers.total_cycles += cycles;
    if (hw_timestamping)
		latency_numbers.total_queue_cycles += (queue_ticks
			* ticks_per_cycle_mult) >> TICKS_PER_CYCLE_SHIFT;
    
    latency_numbers.total_pkts += nb_pkts;
    
    // 每处理100个包就输出一次平均延迟,减少频繁打印的开销问题.
    if (latency_numbers.total_pkts > (100)) {
		printf("Latency = %"PRIu64" cycles\n",
		latency_numbers.total_cycles / latency_numbers.total_pkts);
		if (hw_timestamping) {
			printf("Latency from HW = %"PRIu64" cycles\n",
			   latency_numbers.total_queue_cycles
			   / latency_numbers.total_pkts);
		}
		latency_numbers.total_cycles = 0;
		latency_numbers.total_queue_cycles = 0;
		latency_numbers.total_pkts = 0;
	}
    
    return nb_pkts;
}


// 端口初始化,和之前类似,我们看下逻辑上的改变
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

    // tx/rx的一些特定的配置项
    struct rte_eth_txconf txconf;
    struct rte_eth_rxconf rxconf;


    //检查端口是否是有效的端口
    if(!rte_eth_dev_is_valid_port(port))
        return -1;

    // 必须的初始化步骤,把默认的配置结构清空
    memset(&port_conf, 0, sizeof(struct rte_eth_conf));    

    // 获取这个port的硬件能力的相关信息
    retval = rte_eth_dev_info_get(port, &dev_info);

    // 检查是否能够正确获取网卡的信息
    if(retval != 0){
        printf("OH!!!SHIT,we got some problems get device (port %d) info: %s", 
        port,
        strerror(-retval));
        return retval;
    }

    // 这是一种性能优化,启用MBUF_FAST_FREE
    // 允许mbuf在发送完成之后快速地释放
    if(dev_info.tx_offload_capa & RTE_ETH_TX_OFFLOAD_MBUF_FAST_FREE)
        port_conf.txmode.offloads |= 
            RTE_ETH_TX_OFFLOAD_MBUF_FAST_FREE;
        

    // 是否支持硬件时间戳并且进行注册
    if (hw_timestamping) {
    if (!(dev_info.rx_offload_capa & RTE_ETH_RX_OFFLOAD_TIMESTAMP)) {
        printf("Port %u does not support hardware timestamping\n", port);
        return -1;
    }
    port_conf.rxmode.offloads |= RTE_ETH_RX_OFFLOAD_TIMESTAMP;
    rte_mbuf_dyn_rx_timestamp_register(&hwts_dynfield_offset, NULL);
    if (hwts_dynfield_offset < 0) {
        printf("Failed to register timestamp field\n");
        return -rte_errno;
    }
    }

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
    rxconf = dev_info.default_rxconf;
    for(q = 0; q < rx_rings; ++q){
        retval = rte_eth_rx_queue_setup(port, q, nb_rxd, rte_eth_dev_socket_id(port),&rxconf, mbuf_pool);
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

    // 进行硬件时间戳的校准
    if (hw_timestamping && ticks_per_cycle_mult == 0) {
    uint64_t cycles_base = rte_rdtsc();
    uint64_t ticks_base;
    rte_eth_read_clock(port, &ticks_base);
    rte_delay_ms(100);
    uint64_t cycles = rte_rdtsc() - cycles_base;
    uint64_t ticks;
    rte_eth_read_clock(port, &ticks);
    uint64_t t_freq = ticks - ticks_base;
    double freq_mult = (double)cycles / t_freq;
    ticks_per_cycle_mult = (1 << TICKS_PER_CYCLE_SHIFT) / freq_mult;
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

    // 注册回调函数,当接受或者发送数据包的时候,就会触发回调函数,进行计数
    // 这就是这个样例的核心,当我们的网卡在接收到数据包的时候,会触发我们的函数进行相应的处理
    rte_eth_add_rx_callback(port, 0, add_timestamps, NULL);
    rte_eth_add_tx_callback(port, 0, calc_latency, NULL);
    return 0;
}

static __rte_noreturn void
lcore_main(void)
{
    // 设置端口
    uint16_t port;

    RTE_ETH_FOREACH_DEV(port)
    if (rte_eth_dev_socket_id(port) >= 0 &&
            rte_eth_dev_socket_id(port) != (int)rte_socket_id())
        printf("WARNING, port %u is on remote NUMA node...\n", port);

    printf("\nCore %u forwarding packets. [Ctrl+C to quit]\n",
			rte_lcore_id());

    for(;;){
        RTE_ETH_FOREACH_DEV(port){
            struct rte_mbuf *bufs[BURST_SIZE];
            const uint16_t nb_rx = rte_eth_rx_burst(port, 0, bufs, BURST_SIZE);

            if (unlikely(nb_rx == 0))
                continue;
            
            const uint16_t nb_tx = rte_eth_tx_burst(port ^ 1, 0, bufs, nb_rx);
            if (unlikely(nb_tx < nb_rx)){
                uint16_t buf;

                for(buf = nb_tx; buf < nb_rx; ++buf){
                    rte_pktmbuf_free(bufs[buf]);
                }
            }
        }
    }
    
    
}

/* Main function, does initialisation and calls the per-lcore functions */
int
main(int argc, char *argv[])
{
	struct rte_mempool *mbuf_pool;
	uint16_t nb_ports;
	uint16_t portid;
	struct option lgopts[] = {
		{ NULL,  0, 0, 0 }
	};
	int opt, option_index;

	static const struct rte_mbuf_dynfield tsc_dynfield_desc = {
		.name = "example_bbdev_dynfield_tsc",
		.size = sizeof(tsc_t),
		.align = alignof(tsc_t),
	};

	/* init EAL */
	int ret = rte_eal_init(argc, argv);

	if (ret < 0)
		rte_exit(EXIT_FAILURE, "Error with EAL initialization\n");
	argc -= ret;
	argv += ret;

	while ((opt = getopt_long(argc, argv, "t", lgopts, &option_index))
			!= EOF)
		switch (opt) {
		case 't':
			hw_timestamping = 1;
			break;
		default:
			printf(usage, argv[0]);
			return -1;
		}
	optind = 1; /* reset getopt lib */

	nb_ports = rte_eth_dev_count_avail();
	if (nb_ports < 2 || (nb_ports & 1))
		rte_exit(EXIT_FAILURE, "Error: number of ports must be even\n");

	mbuf_pool = rte_pktmbuf_pool_create("MBUF_POOL",
		NUM_MBUFS * nb_ports, MBUF_CACHE_SIZE, 0,
		RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());
	if (mbuf_pool == NULL)
		rte_exit(EXIT_FAILURE, "Cannot create mbuf pool\n");

	tsc_dynfield_offset =
		rte_mbuf_dynfield_register(&tsc_dynfield_desc);
	if (tsc_dynfield_offset < 0)
		rte_exit(EXIT_FAILURE, "Cannot register mbuf field\n");

	/* initialize all ports */
	RTE_ETH_FOREACH_DEV(portid)
		if (port_init(portid, mbuf_pool) != 0)
			rte_exit(EXIT_FAILURE, "Cannot init port %"PRIu16"\n",
					portid);

	if (rte_lcore_count() > 1)
		printf("\nWARNING: Too much enabled lcores - "
			"App uses only 1 lcore\n");

	/* call lcore_main on main core only */
	lcore_main();

	/* clean up the EAL */
	rte_eal_cleanup();

	return 0;
}
// 注意上面打印的参数进行了调整,为了更快能观测到时钟
// 你先收集两个pcap包作为虚拟网卡
/*
sudo tcpdump -i lo -c 1000 -w /tmp/in0.pcap
sudo tcpdump -i lo -c 1000 -w /tmp/in1.pcap
*/

// 然后运行程序,有下面的结果
/*
sudo ./rxtx_callbacks/rxtx_callbacks -l 1 \
  --vdev=net_pcap0,rx_pcap=/tmp/in0.pcap,tx_pcap=/tmp/out0.pcap \
  --vdev=net_pcap1,rx_pcap=/tmp/in1.pcap,tx_pcap=/tmp/out1.pcap
EAL: Detected CPU lcores: 32
EAL: Detected NUMA nodes: 1
EAL: Detected shared linkage of DPDK
EAL: Multi-process socket /var/run/dpdk/rte/mp_socket
EAL: Selected IOVA mode 'VA'
Port 0 MAC address: 02 70 63 61 70 00
Port 1 MAC address: 02 70 63 61 70 01

Core 1 forwarding packets. [Ctrl+C to quit]
Latency = 600 cycles
Latency = 243 cycles
Latency = 227 cycles
Latency = 297 cycles
Latency = 228 cycles
Latency = 308 cycles
Latency = 301 cycles
Latency = 315 cycles
Latency = 268 cycles
Latency = 269 cycles
Latency = 325 cycles
Latency = 341 cycles
Latency = 256 cycles
Latency = 332 cycles
Latency = 271 cycles
*/



