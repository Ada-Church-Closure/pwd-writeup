// 主要是实现一个flow规则,基本就是 模式匹配(比如IP或者某个端口) + 动作(转发/丢弃/标记)
// snippets:定义了规则和模式
// 返回创建的flow

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <inttypes.h>
#include <sys/types.h>
#include <sys/queue.h>
#include <setjmp.h>
#include <stdarg.h>
#include <ctype.h>
#include <errno.h>
#include <getopt.h>
#include <signal.h>
#include <stdbool.h>

#include <rte_eal.h>
#include <rte_common.h>
#include <rte_malloc.h>
#include <rte_ether.h>
#include <rte_ethdev.h>
#include <rte_mempool.h>
#include <rte_mbuf.h>
#include <rte_net.h>
#include <rte_flow.h>
#include <rte_cycles.h>
#include <rte_argparse.h>

#include "common.h"
#include "snippets/snippet_match_ipv4.h"

bool enable_promiscuous_mode = true; /* 默认启用混杂模式 */
bool enable_flow_isolation; /* 某些片段可能需要隔离模式而非混杂模式 */

static int use_template_api = 1;

// volatile多线程可见,安全quit
static volatile bool force_quit;

static uint16_t port_id;

// 默认有5个队列
static uint16_t nr_queues = 5;

struct rte_mempool *mbuf_pool;
struct rte_flow *flow;

// 单个队列的最大容量
#define MAX_QUEUE_SIZE 256

/* 打印以太网的MAC地址 */
static inline void
print_ether_addr(const char* what, struct rte_ether_addr* eth_addr)
{
    char buf[RTE_ETHER_ADDR_FMT_SIZE];
    rte_ether_format_addr(buf, RTE_ETHER_ADDR_FMT_SIZE, eth_addr);
    printf("%s%s", what, buf);
}

/* 主循环函数---数据包的处理 */
// 就是接受数据包,转换成以太网头指针,然后打印MAC地址,接着清空
static int 
main_loop(void)
{
    struct rte_mbuf *mbufs[32];          // 存储接收到的数据包
    struct rte_ether_hdr *eth_hdr;       // 以太网头指针
    struct rte_flow_error error;         // 流规则错误信息
    uint16_t nb_rx;                      // 实际接收到的包数量
    uint16_t i, j;                       // 循环计数器（队列索引和包索引）
    int ret;                             // 返回值占位

    while(!force_quit) {
        // 遍历每个队列
        for(i = 0; i < nr_queues; ++i) {
            // 从对应的网卡和队列拿数据包
            nb_rx = rte_eth_rx_burst(port_id, i, mbufs, 32);

            // 处理接收到的数据包
            if(nb_rx) {
                for(j = 0; j < nb_rx; ++j){
                    // 每次都是按照顺序放进缓存池的,处理完就清空
                    struct rte_mbuf *m = mbufs[j];
                    // 把mbuf数据包转换成以太网头指针,然后我们就能获取关于以太网的信息
                    eth_hdr = rte_pktmbuf_mtod(m, struct rte_ether_hdr *);
                    print_ether_addr("src=", &eth_hdr->src_addr);
                    print_ether_addr(" - dest=", &eth_hdr->dst_addr);
                    printf(" - queue0x%x", (unsigned int)i);
                    printf("\n");

                    rte_pktmbuf_free(m);
                }
            }
        }
    }

    // Ctrl + C关闭和释放所有的资源
    rte_flow_flush(port_id, &error);
	ret = rte_eth_dev_stop(port_id);
	if (ret < 0)
		printf("Failed to stop port %u: %s",
			   port_id, rte_strerror(-ret));
	rte_eth_dev_close(port_id);
	return ret;
}

// 检查网卡链路的状态
#define CHECK_INTERVAL 1000  /* 检查间隔 1000ms（实际为1秒，注释有误） */
#define MAX_REPEAT_TIMES 90  /* 最大重试次数（90次 * 1秒 = 90秒超时） */

static void
assert_link_status(void)
{   
    // 存储链路状态的结构体
    struct rte_eth_link link;

    // 重试次数和错误码
    uint8_t rep_cnt = MAX_REPEAT_TIMES;
    int link_get_err = -EINVAL;

    memset(&link, 0, sizeof(link));

    // 尝试获取链路的状态
    do{
        link_get_err = rte_eth_link_get(port_id, &link);
        // 获取状态成功并且已经setup,就可以break
        if(link_get_err == 0 && link.link_status == RTE_ETH_LINK_UP){
            break;
        }
        // 暂停1s---延迟等待(避免忙等待)
        rte_delay_ms(CHECK_INTERVAL);
    }while(--rep_cnt);


    // 相关的错误处理
    if (link_get_err < 0)
        rte_exit(EXIT_FAILURE, ":: error: link get is failing: %s\n",
                 rte_strerror(-link_get_err));
    if (link.link_status == RTE_ETH_LINK_DOWN)
        rte_exit(EXIT_FAILURE, ":: error: link is still down\n");
}

/* 给制定的port配置流规则的模板 */
static void
configure_port_template(uint16_t port_id)
{
    int ret;
	uint16_t std_queue;
	struct rte_flow_error error;
    // 每个流队列的属性数组
	struct rte_flow_queue_attr queue_attr[RTE_MAX_LCORE];
    // 指向queue_attr的指针数组
	const struct rte_flow_queue_attr *attr_list[RTE_MAX_LCORE];

    // 端口级的属性,比如计数器的数量
	struct rte_flow_port_attr port_attr = { .nb_counters = 1 /* rules count */ };

    // 设置每个队列的大小
    // 然后用attr_list存储每个队列属性的指针
	for (std_queue = 0; std_queue < RTE_MAX_LCORE; std_queue++) {
		queue_attr[std_queue].size = MAX_QUEUE_SIZE;
		attr_list[std_queue] = &queue_attr[std_queue];
	}

    // 在这个端口上配置flow规则
	ret = rte_flow_configure(port_id, &port_attr,
				 1, attr_list, &error);
	if (ret != 0)
		rte_exit(EXIT_FAILURE,
			 "rte_flow_configure:err=%d, port=%u\n",
			 ret, port_id);
	printf(":: Configuring template port [%d] Done ..\n", port_id);
}


/* 端口配置 */ 
static void
init_port(void)
{
	int ret;
	uint16_t i;
	/* Ethernet port configured with default settings. */
	struct rte_eth_conf port_conf = {
		.txmode = {
			.offloads =
				RTE_ETH_TX_OFFLOAD_VLAN_INSERT |
				RTE_ETH_TX_OFFLOAD_IPV4_CKSUM  |
				RTE_ETH_TX_OFFLOAD_UDP_CKSUM   |
				RTE_ETH_TX_OFFLOAD_TCP_CKSUM   |
				RTE_ETH_TX_OFFLOAD_SCTP_CKSUM  |
				RTE_ETH_TX_OFFLOAD_TCP_TSO,
		},
	};
	struct rte_eth_txconf txq_conf;
	struct rte_eth_rxconf rxq_conf;
	struct rte_eth_dev_info dev_info;

	ret = rte_eth_dev_info_get(port_id, &dev_info);
	if (ret != 0)
		rte_exit(EXIT_FAILURE,
			"Error during getting device (port %u) info: %s\n",
			port_id, strerror(-ret));

	port_conf.txmode.offloads &= dev_info.tx_offload_capa;
	printf(":: initializing port: %d\n", port_id);
	ret = rte_eth_dev_configure(port_id,
				nr_queues, nr_queues, &port_conf);
	if (ret < 0) {
		rte_exit(EXIT_FAILURE,
			":: cannot configure device: err=%d, port=%u\n",
			ret, port_id);
	}

	rxq_conf = dev_info.default_rxconf;
	rxq_conf.offloads = port_conf.rxmode.offloads;

	/* Configuring number of RX and TX queues connected to single port. */
	for (i = 0; i < nr_queues; i++) {
		ret = rte_eth_rx_queue_setup(port_id, i, 512,
					 rte_eth_dev_socket_id(port_id),
					 &rxq_conf,
					 mbuf_pool);
		if (ret < 0) {
			rte_exit(EXIT_FAILURE,
				":: Rx queue setup failed: err=%d, port=%u\n",
				ret, port_id);
		}
	}

	txq_conf = dev_info.default_txconf;
	txq_conf.offloads = port_conf.txmode.offloads;

	for (i = 0; i < nr_queues; i++) {
		ret = rte_eth_tx_queue_setup(port_id, i, 512,
				rte_eth_dev_socket_id(port_id),
				&txq_conf);
		if (ret < 0) {
			rte_exit(EXIT_FAILURE,
				":: Tx queue setup failed: err=%d, port=%u\n",
				ret, port_id);
		}
	}

	if (enable_promiscuous_mode) {
		/* Setting the RX port to promiscuous mode. */
		ret = rte_eth_promiscuous_enable(port_id);
		printf(":: promiscuous mode enabled\n");
		if (ret != 0)
			rte_exit(EXIT_FAILURE,
				":: promiscuous mode enable failed: err=%s, port=%u\n",
				rte_strerror(-ret), port_id);
	} else if (enable_flow_isolation) {
		/* Setting the RX port to isolate mode. */
		ret = rte_flow_isolate(port_id, 1, NULL);
		printf(":: isolate mode enabled\n");
		if (ret != 0)
			rte_exit(EXIT_FAILURE,
				":: isolate mode enable failed: err=%s, port=%u\n",
				rte_strerror(-ret), port_id);
	}

	ret = rte_eth_dev_start(port_id);
	if (ret < 0) {
		rte_exit(EXIT_FAILURE,
			"rte_eth_dev_start:err=%d, port=%u\n",
			ret, port_id);
	}

	assert_link_status();

	printf(":: initializing port: %d done\n", port_id);

	if (use_template_api == 0)
		return;

	/* Adds rules engine configuration. 8< */
	ret = rte_eth_dev_stop(port_id);
	if (ret < 0)
		rte_exit(EXIT_FAILURE,
			"rte_eth_dev_stop:err=%d, port=%u\n",
			ret, port_id);
    // 配置端口的flow规则
	configure_port_template(port_id);
	ret = rte_eth_dev_start(port_id);
	if (ret < 0)
		rte_exit(EXIT_FAILURE,
			"rte_eth_dev_start:err=%d, port=%u\n",
			ret, port_id);
	/* >8 End of adding rules engine configuration. */
}

/* 信号处理 */
static void
signal_handler(int signum)
{
	if (signum == SIGINT || signum == SIGTERM) {
		printf("\n\nSignal %d received, preparing to exit...\n",
				signum);
		force_quit = true;
	}
}

/* Parse the argument given in the command line of the application */
/* 根据给定的命令行参数进行配置 */
static int
flow_filtering_parse_args(int argc, char **argv)
{
	static struct rte_argparse obj = {
		.prog_name = "flow_filtering",
		.usage = "[EAL options] -- [optional parameters]",
		.descriptor = NULL,
		.epilog = NULL,
		.exit_on_error = false,
		.callback = NULL,
		.opaque = NULL,
        // 这里的参数是什么意思
        .args = {
			{ "--template", NULL, "Enable template API flow",
			  &use_template_api, (void *)1,
			  // 这里的参数可能会不兼容,是由于你使用的库不是最新的导致的
			  RTE_ARGPARSE_VALUE_NONE, RTE_ARGPARSE_VALUE_TYPE_INT,
			},
			{ "--non-template", NULL, "Enable non template API flow",
			  &use_template_api, (void *)0,
			  RTE_ARGPARSE_VALUE_NONE, RTE_ARGPARSE_VALUE_TYPE_INT,
			},
			ARGPARSE_ARG_END(),
		},
	};

	return rte_argparse_parse(&obj, argc, argv);
}

int
main(int argc, char **argv)
{
	int ret;
	uint16_t nr_ports;
	struct rte_flow_error error;

	ret = rte_eal_init(argc, argv);
	if (ret < 0)
		rte_exit(EXIT_FAILURE, ":: invalid EAL arguments\n");
	argc -= ret;
	argv += ret;

	force_quit = false;
	signal(SIGINT, signal_handler);
	signal(SIGTERM, signal_handler);

	ret = flow_filtering_parse_args(argc, argv);
	if (ret < 0)
		rte_exit(EXIT_FAILURE, "Invalid flow filtering arguments\n");

	nr_ports = rte_eth_dev_count_avail();
	if (nr_ports == 0)
		rte_exit(EXIT_FAILURE, ":: no Ethernet ports found\n");
	port_id = 0;
	if (nr_ports != 1) {
		printf(":: warn: %d ports detected, but we use only one: port %u\n",
			nr_ports, port_id);
	}

	mbuf_pool = rte_pktmbuf_pool_create("mbuf_pool", 4096, 128, 0,
						RTE_MBUF_DEFAULT_BUF_SIZE,
						rte_socket_id());
	if (mbuf_pool == NULL)
		rte_exit(EXIT_FAILURE, "Cannot init mbuf pool\n");

	snippet_init();

	init_port();

	// 生成flow,这里是核心
	flow = generate_flow_skeleton(port_id, &error, use_template_api);
	

	if (!flow) {
    /* 大多数不支持 flow 的 PMD 会返回 ENOTSUP（Function not implemented） */
    if (rte_errno == ENOTSUP) {
        fprintf(stderr,
            "Warning: rte_flow is not supported by this device/PMD "
            "(type=%d, msg=%s). Continue WITHOUT hardware filtering.\n",
            error.type, error.message ? error.message : "no message");
        /* 不退出，直接进入主循环；这时仅作为普通收发/打印程序使用 */
    } else {
        fprintf(stderr, "Flow can't be created (type=%d): %s (%s)\n",
            error.type,
            error.message ? error.message : "no message",
            rte_strerror(rte_errno));
        rte_exit(EXIT_FAILURE, "error in creating flow");
    }
} else {
    printf("Flow created!!\n");
}

	ret = main_loop();

	rte_eal_cleanup();

	return ret;
}
