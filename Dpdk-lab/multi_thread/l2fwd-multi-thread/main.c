
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
#include <unistd.h>

#include <rte_common.h>
#include <rte_log.h>
#include <rte_malloc.h>
#include <rte_memory.h>
#include <rte_memcpy.h>
#include <rte_eal.h>
#include <rte_launch.h>
#include <rte_cycles.h>
#include <rte_prefetch.h>
#include <rte_lcore.h>
#include <rte_per_lcore.h>
#include <rte_branch_prediction.h>
#include <rte_interrupts.h>
#include <rte_random.h>
#include <rte_debug.h>
#include <rte_ether.h>
#include <rte_ethdev.h>
#include <rte_mempool.h>
#include <rte_mbuf.h>
#include <rte_string_fns.h>
#include <rte_ring.h>

#include <openssl/evp.h>
#include <openssl/aes.h>
#include <generic/rte_byteorder.h>

#define MARK_ETHER_TYPE 0x88B5

// 加解密逻辑
static const unsigned char aes_key[16] = "0123456789abcdef"; // 128-bit key
static const unsigned char aes_iv[16] = "abcdef9876543210";  // 初始向量

// 每个加密线程的本地上下文（避免频繁创建/销毁）
// 本地线程的做法,避免了数据竞争和锁开销
static __thread EVP_CIPHER_CTX *encrypt_ctx = NULL;
static __thread EVP_CIPHER_CTX *decrypt_ctx = NULL;

// 初始化加密上下文
// 初始化加密上下文
static void init_crypto_contexts(void)
{
    // ---- 初始化加密上下文 ----
    if (encrypt_ctx == NULL)
    {
        encrypt_ctx = EVP_CIPHER_CTX_new();
        if (encrypt_ctx == NULL)
            rte_exit(EXIT_FAILURE, "Cannot create encrypt context\n");
        
        // 在这里进行一次性初始化，设置模式、密钥和IV
        // 这会执行密钥扩展 (key scheduling)
        if (1 != EVP_EncryptInit_ex(encrypt_ctx, EVP_aes_128_cbc(), NULL, aes_key, aes_iv))
            rte_exit(EXIT_FAILURE, "Cannot init encrypt context\n");
    }

    // ---- 初始化解密上下文 ----
    if (decrypt_ctx == NULL)
    {
        decrypt_ctx = EVP_CIPHER_CTX_new();
        if (decrypt_ctx == NULL)
            rte_exit(EXIT_FAILURE, "Cannot create decrypt context\n");

        // 在这里进行一次性初始化
        if (1 != EVP_DecryptInit_ex(decrypt_ctx, EVP_aes_128_cbc(), NULL, aes_key, aes_iv))
            rte_exit(EXIT_FAILURE, "Cannot init decrypt context\n");
    }
}


// ----------------- AES 加密 -----------------
void real_encrypt(struct rte_mbuf *m)
{
    // printf("Encypted\n");
    struct rte_ether_hdr *eth = rte_pktmbuf_mtod(m, struct rte_ether_hdr *);
    uint8_t *payload = (uint8_t *)(eth + 1);
    int payload_len = rte_pktmbuf_data_len(m) - sizeof(struct rte_ether_hdr);

    int out_len1 = 0, out_len2 = 0;
    uint8_t outbuf[payload_len + EVP_MAX_BLOCK_LENGTH];

    // 重置上下文，使其恢复到 Init 后的状态（主要是重置IV）
    // 不再需要调用 EVP_EncryptInit_ex
    if (1 != EVP_CIPHER_CTX_reset(encrypt_ctx))
        rte_exit(EXIT_FAILURE, "Cannot reset encrypt context\n");

    EVP_EncryptUpdate(encrypt_ctx, outbuf, &out_len1, payload, payload_len);
    EVP_EncryptFinal_ex(encrypt_ctx, outbuf + out_len1, &out_len2);

    int total_len = out_len1 + out_len2;

    // 把加密后的内容写回 mbuf
    rte_memcpy(payload, outbuf, total_len);
    rte_pktmbuf_pkt_len(m) = sizeof(struct rte_ether_hdr) + total_len;
    rte_pktmbuf_data_len(m) = rte_pktmbuf_pkt_len(m);

    // 修改 EtherType → 标记"已加密"
    eth->ether_type = rte_cpu_to_be_16(MARK_ETHER_TYPE);
}

// ----------------- AES 解密 -----------------
void real_decrypt(struct rte_mbuf *m)
{
    // printf("Decrypted\n");
    struct rte_ether_hdr *eth = rte_pktmbuf_mtod(m, struct rte_ether_hdr *);
    uint8_t *payload = (uint8_t *)(eth + 1);
    int payload_len = rte_pktmbuf_data_len(m) - sizeof(struct rte_ether_hdr);

    int out_len1 = 0, out_len2 = 0;
    uint8_t outbuf[payload_len + EVP_MAX_BLOCK_LENGTH];

    // 重置上下文，使其恢复到 Init 后的状态
    // 不再需要调用 EVP_DecryptInit_ex
    if (1 != EVP_CIPHER_CTX_reset(decrypt_ctx))
        rte_exit(EXIT_FAILURE, "Cannot reset decrypt context\n");

    EVP_DecryptUpdate(decrypt_ctx, outbuf, &out_len1, payload, payload_len);
    EVP_DecryptFinal_ex(decrypt_ctx, outbuf + out_len1, &out_len2);

    int total_len = out_len1 + out_len2;

    // 解密结果写回 payload
    rte_memcpy(payload, outbuf, total_len);
    rte_pktmbuf_pkt_len(m) = sizeof(struct rte_ether_hdr) + total_len;
    rte_pktmbuf_data_len(m) = rte_pktmbuf_pkt_len(m);

    // 还原 EtherType（比如 IPv4）
    eth->ether_type = rte_cpu_to_be_16(0x0800);
}

static volatile bool force_quit;

// 我想用来做时间的统计
static uint64_t start_cycles = 0;
static uint64_t end_cycles = 0;
static uint64_t mid_cycles = 0;
bool begin_flag = false;
bool end_flag = false;

/* MAC updating enabled by default */
// 是否自动更新mac地址
static int mac_updating = 1;

/* Ports set in promiscuous mode off by default. */
static int promiscuous_on;

#define RTE_LOGTYPE_L2FWD RTE_LOGTYPE_USER1

#define MAX_PKT_BURST 32
#define BURST_TX_DRAIN_US 100 /* TX drain every ~100us */
#define MEMPOOL_CACHE_SIZE 256

/*
 * Configurable number of RX/TX ring descriptors
 */
#define RX_DESC_DEFAULT 1024
#define TX_DESC_DEFAULT 1024
static uint16_t nb_rxd = RX_DESC_DEFAULT;
static uint16_t nb_txd = TX_DESC_DEFAULT;

/* ethernet addresses of ports */
static struct rte_ether_addr l2fwd_ports_eth_addr[RTE_MAX_ETHPORTS];

/* mask of enabled ports */
static uint32_t l2fwd_enabled_port_mask = 0;

/* list of enabled ports */
static uint32_t l2fwd_dst_ports[RTE_MAX_ETHPORTS];

struct __rte_cache_aligned port_pair_params
{
#define NUM_PORTS 2
    uint16_t port[NUM_PORTS];
};

static struct port_pair_params port_pair_params_array[RTE_MAX_ETHPORTS / 2];
static struct port_pair_params *port_pair_params;
static uint16_t nb_port_pair_params;

static unsigned int l2fwd_rx_queue_per_lcore = 1;

#define MAX_RX_QUEUE_PER_LCORE 16
#define MAX_TX_QUEUE_PER_PORT 16
/* List of queues to be polled for a given lcore. 8< */
// 一个lcore该poll几个端口的rx队列--->两个网卡默认就是一个
struct __rte_cache_aligned lcore_queue_conf
{
    unsigned n_rx_port;
    unsigned rx_port_list[MAX_RX_QUEUE_PER_LCORE];
};
struct lcore_queue_conf lcore_queue_conf[RTE_MAX_LCORE];
/* >8 End of list of queues to be polled for a given lcore. */

static struct rte_eth_dev_tx_buffer *tx_buffer[RTE_MAX_ETHPORTS];

static struct rte_eth_conf port_conf = {
    .txmode = {
        .mq_mode = RTE_ETH_MQ_TX_NONE,
    },
};

struct rte_mempool *l2fwd_pktmbuf_pool = NULL;

// 做ring的参数的一些描述
/* rings for pipeline */
#define RING_SIZE 65536 // 增加到64K，提供更大缓冲区
#define MAX_CRYPTO_WORKERS 8 // 最多支持8个crypto worker

/* 每个crypto worker有独立的ring队列 */
static struct rte_ring *rx_to_crypto[MAX_CRYPTO_WORKERS];    // I/O → Crypto
static struct rte_ring *crypto_to_tx[MAX_CRYPTO_WORKERS];    // Crypto → I/O

/* 设置每次批量处理大小 */
#define RTE_RING_BURST 32

/* Per-port statistics struct */
struct __rte_cache_aligned l2fwd_port_statistics
{
    uint64_t tx;
    uint64_t rx;
    uint64_t dropped;
};
struct l2fwd_port_statistics port_statistics[RTE_MAX_ETHPORTS];

/* 添加ring队列丢包统计 - 每个worker独立统计 */
static uint64_t ring_rx_to_crypto_dropped[MAX_CRYPTO_WORKERS];
static uint64_t ring_crypto_to_tx_dropped[MAX_CRYPTO_WORKERS];

/* 添加加解密计数统计 - 每个worker独立统计 */
static uint64_t crypto_encrypted_count[MAX_CRYPTO_WORKERS];
static uint64_t crypto_decrypted_count[MAX_CRYPTO_WORKERS];

/* 验证: 统计发送的包中有多少是加密标记的 */
static uint64_t tx_encrypted_marked_count = 0;

#define MAX_TIMER_PERIOD 86400 /* 1 day max */
/* A tsc-based timer responsible for triggering statistics printout */
static uint64_t timer_period = 10; /* default period is 10 seconds */

/* Print out statistics on packets dropped */
// 打印port上数据包转发的情况
static void
print_stats(void)
{
    uint64_t total_packets_dropped, total_packets_tx, total_packets_rx;
    unsigned portid;

    total_packets_dropped = 0;
    total_packets_tx = 0;
    total_packets_rx = 0;

    const char clr[] = {27, '[', '2', 'J', '\0'};
    const char topLeft[] = {27, '[', '1', ';', '1', 'H', '\0'};

    /* Clear screen and move to top left */
    printf("%s%s", clr, topLeft);

    printf("\nPort statistics ====================================");

    for (portid = 0; portid < RTE_MAX_ETHPORTS; portid++)
    {
        /* skip disabled ports */
        if ((l2fwd_enabled_port_mask & (1 << portid)) == 0)
            continue;
        printf("\nStatistics for port %u ------------------------------"
               "\nPackets sent: %24" PRIu64
               "\nPackets received: %20" PRIu64
               "\nPackets dropped: %21" PRIu64,
               portid,
               port_statistics[portid].tx,
               port_statistics[portid].rx,
               port_statistics[portid].dropped);

        total_packets_dropped += port_statistics[portid].dropped;
        total_packets_tx += port_statistics[portid].tx;
        total_packets_rx += port_statistics[portid].rx;
    }

    /* 添加 Ring 状态调试信息 */
    printf("\nRing statistics (per worker) =======================");

    uint64_t total_rx_dropped = 0, total_tx_dropped = 0;
    uint64_t total_encrypted = 0, total_decrypted = 0;

    for (unsigned w = 0; w < MAX_CRYPTO_WORKERS; w++)
    {
        if (rx_to_crypto[w] == NULL)
            break; // 没有更多worker

        unsigned rx_count = rte_ring_count(rx_to_crypto[w]);
        unsigned tx_count = rte_ring_count(crypto_to_tx[w]);

        printf("\nWorker %u:", w);
        printf("\n  RX->Crypto: %u/%u used", rx_count, RING_SIZE);
        printf("\n  Crypto->TX: %u/%u used", tx_count, RING_SIZE);
        printf("\n  Encrypted: %" PRIu64, crypto_encrypted_count[w]);
        printf("\n  Decrypted: %" PRIu64, crypto_decrypted_count[w]);
        printf("\n  Dropped(RX): %" PRIu64, ring_rx_to_crypto_dropped[w]);
        printf("\n  Dropped(TX): %" PRIu64, ring_crypto_to_tx_dropped[w]);

        total_rx_dropped += ring_rx_to_crypto_dropped[w];
        total_tx_dropped += ring_crypto_to_tx_dropped[w];
        total_encrypted += crypto_encrypted_count[w];
        total_decrypted += crypto_decrypted_count[w];
    }

    printf("\n\nCrypto statistics (total) ==========================="
           "\nTotal encrypted: %" PRIu64
           "\nTotal decrypted: %" PRIu64
           "\nTotal RX dropped: %" PRIu64
           "\nTotal TX dropped: %" PRIu64
           "\n\nVerification ===================================="
           "\nTX encrypted mark: %" PRIu64
           "\n  (Should match total encrypted)",
           total_encrypted, total_decrypted,
           total_rx_dropped, total_tx_dropped,
           tx_encrypted_marked_count);

    printf("\nAggregate statistics ==============================="
           "\nTotal packets sent: %18" PRIu64
           "\nTotal packets received: %14" PRIu64
           "\nTotal packets dropped: %15" PRIu64,
           total_packets_tx,
           total_packets_rx,
           total_packets_dropped);
    printf("\n====================================================\n");
    fflush(stdout);
}

// mac地址更新--->可以禁用
static void
l2fwd_mac_updating(struct rte_mbuf *m, unsigned dest_portid)
{
    struct rte_ether_hdr *eth;
    void *tmp;

    eth = rte_pktmbuf_mtod(m, struct rte_ether_hdr *);

    /* 02:00:00:00:00:xx */
    tmp = &eth->dst_addr.addr_bytes[0];
    *((uint64_t *)tmp) = 0x000000000002 + ((uint64_t)dest_portid << 40);

    /* src addr */
    rte_ether_addr_copy(&l2fwd_ports_eth_addr[dest_portid], &eth->src_addr);
}

// 加解密的lcore的工作逻辑
static int
crypto_loop(void *arg)
{
    unsigned worker_id = (unsigned)(uintptr_t)arg; // 获取worker ID
    struct rte_mbuf *pkts[RTE_RING_BURST];
    struct rte_mbuf *encrypted_pkts[RTE_RING_BURST];
    unsigned nb, i;
    unsigned idle_count = 0;
    unsigned encrypted_count;

    printf("Crypto worker %u started\n", worker_id);

    // 初始化线程本地加密上下文
    init_crypto_contexts();

    while (!force_quit)
    {
        /* 从专属ring取包 */
        nb = rte_ring_dequeue_burst(rx_to_crypto[worker_id], (void **)pkts,
                                    RTE_RING_BURST, NULL);
        if (nb == 0)
        {
            idle_count++;
            if (idle_count > 100)
            {
                /* 长时间空闲时短暂休眠，减少CPU占用 */
                rte_delay_us(1);
                idle_count = 0;
            }
            else
            {
                rte_pause();
            }
            continue;
        }
        idle_count = 0;

        encrypted_count = 0;

        for (i = 0; i < nb; i++)
        {
            struct rte_mbuf *m = pkts[i];
            struct rte_ether_hdr *eth = rte_pktmbuf_mtod(m, struct rte_ether_hdr *);

            if (rte_be_to_cpu_16(eth->ether_type) == MARK_ETHER_TYPE)
            {
                /* 解密数据包，然后释放（避免循环转发） */
                real_decrypt(m);
                crypto_decrypted_count[worker_id]++;  // 统计解密包数
                rte_pktmbuf_free(m);
            }
            else
            {
                /* 加密数据包并转发 */
                real_encrypt(m); // 内部已设置 MARK_ETHER_TYPE
                crypto_encrypted_count[worker_id]++;  // 统计加密包数

                uint16_t src = m->port;
                uint16_t dst = l2fwd_dst_ports[src];
                m->port = dst;

                /* 收集加密后的包，稍后批量入队 */
                encrypted_pkts[encrypted_count++] = m;
            }
        }

        /* 批量将加密后的包入队到专属return ring */
        if (encrypted_count > 0)
        {
            unsigned enqueued = rte_ring_enqueue_burst(crypto_to_tx[worker_id],
                                (void **)encrypted_pkts, encrypted_count, NULL);

            /* 释放未能入队的包并统计丢包 */
            if (unlikely(enqueued < encrypted_count))
            {
                ring_crypto_to_tx_dropped[worker_id] += (encrypted_count - enqueued);
                for (i = enqueued; i < encrypted_count; i++)
                {
                    rte_pktmbuf_free(encrypted_pkts[i]);
                }
            }
        }
    }

    /* 清理线程本地上下文 */
    if (encrypt_ctx)
    {
        EVP_CIPHER_CTX_free(encrypt_ctx);
        encrypt_ctx = NULL;
    }
    if (decrypt_ctx)
    {
        EVP_CIPHER_CTX_free(decrypt_ctx);
        decrypt_ctx = NULL;
    }

    return 0;
}

/* main processing loop */
// lcore循环的主要逻辑:这里只负责I/O的处理
static void
l2fwd_main_loop(void)
{
    struct rte_mbuf *pkts_burst[MAX_PKT_BURST];
    struct rte_mbuf *m;
    int sent;
    unsigned lcore_id;
    uint64_t prev_tsc, diff_tsc, cur_tsc, timer_tsc;
    unsigned i, j, portid, nb_rx;
    struct lcore_queue_conf *qconf;
    const uint64_t drain_tsc = (rte_get_tsc_hz() + US_PER_S - 1) / US_PER_S *
                               BURST_TX_DRAIN_US;
    struct rte_eth_dev_tx_buffer *buffer;

    prev_tsc = 0;
    timer_tsc = 0;

    lcore_id = rte_lcore_id();
    qconf = &lcore_queue_conf[lcore_id];

    if (qconf->n_rx_port == 0)
    {
        RTE_LOG(INFO, L2FWD, "lcore %u has nothing to do\n", lcore_id);
        return;
    }

    RTE_LOG(INFO, L2FWD, "entering main loop on lcore %u\n", lcore_id);

    for (i = 0; i < qconf->n_rx_port; i++)
    {

        portid = qconf->rx_port_list[i];
        RTE_LOG(INFO, L2FWD, " -- lcoreid=%u portid=%u\n", lcore_id,
            
                portid);
    }

    while (!force_quit)
    {

        /* Drains TX queue in its main loop. 8< */
        cur_tsc = rte_rdtsc();

        /*
         * TX burst queue drain
         */
        // 定期强制刷新tx buffer内部的数据到tx发送队列中
        diff_tsc = cur_tsc - prev_tsc;
        if (unlikely(diff_tsc > drain_tsc))
        {
            // lcore遍历自己负责的每个port上的tx
            for (i = 0; i < qconf->n_rx_port; i++)
            {
                portid = l2fwd_dst_ports[qconf->rx_port_list[i]];
                buffer = tx_buffer[portid];

                sent = rte_eth_tx_buffer_flush(portid, 0, buffer);
                if (sent)
                    port_statistics[portid].tx += sent;
            }
            /* if timer is enabled */
            if (timer_period > 0)
            {
                /* advance the timer */
                timer_tsc += diff_tsc;
                /* if timer has reached its timeout */
                // 每隔10s打印一次状态
                if (unlikely(timer_tsc >= timer_period))
                {
                    /* do this only on main core */
                    if (lcore_id == rte_get_main_lcore())
                    {
                        print_stats();
                        /* reset the timer */
                        timer_tsc = 0;
                    }
                }
            }
            prev_tsc = cur_tsc;
        }
        /* >8 End of draining TX queue. */

        /* Read packet from RX queues. 8< */
        // lcore遍历自己负责的每一个的port上的rx队列,从rx队列收包,然后交给上面的simple_forward的转发程序来处理
        for (i = 0; i < qconf->n_rx_port; i++)
        {

            portid = qconf->rx_port_list[i];
            nb_rx = rte_eth_rx_burst(portid, 0,
                                     pkts_burst, MAX_PKT_BURST);

            if (unlikely(nb_rx == 0))
                continue;

            // 这里开始了发包,记录时间
            if (!begin_flag)
            {
                lcore_id = rte_lcore_id();
                printf("Lcore_id: %d\n", lcore_id);
                begin_flag = true;
                uint64_t hhz = rte_get_timer_hz();
                mid_cycles = rte_get_timer_cycles();
                double start_seconds = (double)(mid_cycles - start_cycles) / hhz;
                printf("Start cycles is set at %.5f seconds\n", start_seconds);
            }

            port_statistics[portid].rx += nb_rx;

            // ⚠️ 注意: 这里不能用 rx 判断结束,因为包还没真正处理完!
            // 需要在发送端(tx)判断才准确

            for (int j = 0; j < nb_rx; ++j)
            {
                pkts_burst[j]->port = portid;
            }
            /* 尝试批量入队 */
            // 把收到的包放进crypto队列进行加密操作
            unsigned enqueued = rte_ring_enqueue_burst(rx_to_crypto, (void **)pkts_burst, nb_rx, NULL);
            if (enqueued < nb_rx)
            {
                /* 未能全部入队，释放未入队的 mbuf 并统计丢包 */
                ring_rx_to_crypto_dropped += (nb_rx - enqueued);
                for (j = enqueued; j < nb_rx; j++)
                    rte_pktmbuf_free(pkts_burst[j]);
            }
        }

        /* 独立的发送逻辑：从 crypto_to_tx 取回已处理包并批量发送 */
        struct rte_mbuf *tx_pkts[RTE_RING_BURST];
        unsigned nb_to_send = rte_ring_dequeue_burst(crypto_to_tx, (void **)tx_pkts, RTE_RING_BURST, NULL);
        if (nb_to_send > 0)
        {
            /* 按目标端口分组批量发送（更高效） */
            struct rte_mbuf *port_pkts[RTE_MAX_ETHPORTS][RTE_RING_BURST];
            unsigned port_counts[RTE_MAX_ETHPORTS] = {0};

            /* 按端口分组 */
            for (unsigned k = 0; k < nb_to_send; k++)
            {
                uint16_t dst = tx_pkts[k]->port;
                if (dst < RTE_MAX_ETHPORTS && port_counts[dst] < RTE_RING_BURST)
                {
                    port_pkts[dst][port_counts[dst]++] = tx_pkts[k];
                }
                else
                {
                    rte_pktmbuf_free(tx_pkts[k]);
                }
            }

            /* 批量发送每个端口的包 */
            for (uint16_t dst = 0; dst < RTE_MAX_ETHPORTS; dst++)
            {
                if (port_counts[dst] > 0)
                {
                    // 验证: 检查发送的包是否有加密标记
                    for (unsigned k = 0; k < port_counts[dst]; k++)
                    {
                        struct rte_ether_hdr *eth = rte_pktmbuf_mtod(port_pkts[dst][k], struct rte_ether_hdr *);
                        if (rte_be_to_cpu_16(eth->ether_type) == MARK_ETHER_TYPE)
                        {
                            tx_encrypted_marked_count++;
                        }
                    }

                    unsigned sent = rte_eth_tx_burst(dst, 0, port_pkts[dst], port_counts[dst]);
                    port_statistics[dst].tx += sent;

                    // 修复: 在这里判断结束条件,因为这里才是真正发送出去的包!
                    if (!end_flag && port_statistics[dst].tx >= 5000000)
                    {
                        end_flag = true;
                        end_cycles = rte_get_timer_cycles();
                        uint64_t hz = rte_get_timer_hz();
                        double seconds = (double)(end_cycles - mid_cycles) / hz;
                        printf("\n========== 多线程性能测试结果 ==========\n");
                        printf("转发包数: 5000000 frames\n");
                        printf("耗时: %.5f seconds\n", seconds);
                        printf("吞吐量: %.2f frames/s (%.2f Kpps)\n",
                            (double)(5000000 / seconds), (double)(5000000 / seconds / 1000));
                        printf("==========================================\n\n");
                    }

                    /* 释放未发送成功的包 */
                    for (unsigned k = sent; k < port_counts[dst]; k++)
                    {
                        rte_pktmbuf_free(port_pkts[dst][k]);
                    }
                }
            }
        }
    }
}

// 运作一个lcore?
// 一个线程处理了这样的事物:1.rx收包 2.加密 3. 改mac地址 4.tx队列发包
static int
l2fwd_launch_one_lcore(__rte_unused void *dummy)
{
    l2fwd_main_loop();
    return 0;
}

/* display usage */
static void
l2fwd_usage(const char *prgname)
{
    printf("%s [EAL options] -- -p PORTMASK [-P] [-q NQ]\n"
           "  -p PORTMASK: hexadecimal bitmask of ports to configure\n"
           "  -P : Enable promiscuous mode\n"
           "  -q NQ: number of queue (=ports) per lcore (default is 1)\n"
           "  -T PERIOD: statistics will be refreshed each PERIOD seconds (0 to disable, 10 default, 86400 maximum)\n"
           "  --no-mac-updating: Disable MAC addresses updating (enabled by default)\n"
           "      When enabled:\n"
           "       - The source MAC address is replaced by the TX port MAC address\n"
           "       - The destination MAC address is replaced by 02:00:00:00:00:TX_PORT_ID\n"
           "  --portmap: Configure forwarding port pair mapping\n"
           "	      Default: alternate port pairs\n\n",
           prgname);
}

// 转换portmask
static int
l2fwd_parse_portmask(const char *portmask)
{
    char *end = NULL;
    unsigned long pm;

    /* parse hexadecimal string */
    pm = strtoul(portmask, &end, 16);
    if ((portmask[0] == '\0') || (end == NULL) || (*end != '\0'))
        return 0;

    return pm;
}

static int
l2fwd_parse_port_pair_config(const char *q_arg)
{
    enum fieldnames
    {
        FLD_PORT1 = 0,
        FLD_PORT2,
        _NUM_FLD
    };
    unsigned long int_fld[_NUM_FLD];
    const char *p, *p0 = q_arg;
    char *str_fld[_NUM_FLD];
    unsigned int size;
    char s[256];
    char *end;
    int i;

    nb_port_pair_params = 0;

    while ((p = strchr(p0, '(')) != NULL)
    {
        ++p;
        p0 = strchr(p, ')');
        if (p0 == NULL)
            return -1;

        size = p0 - p;
        if (size >= sizeof(s))
            return -1;

        memcpy(s, p, size);
        s[size] = '\0';
        if (rte_strsplit(s, sizeof(s), str_fld,
                         _NUM_FLD, ',') != _NUM_FLD)
            return -1;
        for (i = 0; i < _NUM_FLD; i++)
        {
            errno = 0;
            int_fld[i] = strtoul(str_fld[i], &end, 0);
            if (errno != 0 || end == str_fld[i] ||
                int_fld[i] >= RTE_MAX_ETHPORTS)
                return -1;
        }
        if (nb_port_pair_params >= RTE_MAX_ETHPORTS / 2)
        {
            printf("exceeded max number of port pair params: %hu\n",
                   nb_port_pair_params);
            return -1;
        }
        port_pair_params_array[nb_port_pair_params].port[0] =
            (uint16_t)int_fld[FLD_PORT1];
        port_pair_params_array[nb_port_pair_params].port[1] =
            (uint16_t)int_fld[FLD_PORT2];
        ++nb_port_pair_params;
    }
    port_pair_params = port_pair_params_array;
    return 0;
}

static unsigned int
l2fwd_parse_nqueue(const char *q_arg)
{
    char *end = NULL;
    unsigned long n;

    /* parse hexadecimal string */
    n = strtoul(q_arg, &end, 10);
    if ((q_arg[0] == '\0') || (end == NULL) || (*end != '\0'))
        return 0;
    if (n == 0)
        return 0;
    if (n >= MAX_RX_QUEUE_PER_LCORE)
        return 0;

    return n;
}

static int
l2fwd_parse_timer_period(const char *q_arg)
{
    char *end = NULL;
    int n;

    /* parse number string */
    n = strtol(q_arg, &end, 10);
    if ((q_arg[0] == '\0') || (end == NULL) || (*end != '\0'))
        return -1;
    if (n >= MAX_TIMER_PERIOD)
        return -1;

    return n;
}

static const char short_options[] =
    "p:" /* portmask */
    "P"  /* promiscuous */
    "q:" /* number of queues */
    "T:" /* timer period */
    ;

#define CMD_LINE_OPT_NO_MAC_UPDATING "no-mac-updating"
#define CMD_LINE_OPT_PORTMAP_CONFIG "portmap"

enum
{
    /* long options mapped to a short option */

    /* first long only option value must be >= 256, so that we won't
     * conflict with short options */
    CMD_LINE_OPT_NO_MAC_UPDATING_NUM = 256,
    CMD_LINE_OPT_PORTMAP_NUM,
};

static const struct option lgopts[] = {
    {CMD_LINE_OPT_NO_MAC_UPDATING, no_argument, 0,
     CMD_LINE_OPT_NO_MAC_UPDATING_NUM},
    {CMD_LINE_OPT_PORTMAP_CONFIG, 1, 0, CMD_LINE_OPT_PORTMAP_NUM},
    {NULL, 0, 0, 0}};

/* Parse the argument given in the command line of the application */
// 分析给定的参数
static int
l2fwd_parse_args(int argc, char **argv)
{
    int opt, ret, timer_secs;
    char **argvopt;
    int option_index;
    char *prgname = argv[0];

    argvopt = argv;
    port_pair_params = NULL;

    while ((opt = getopt_long(argc, argvopt, short_options,
                              lgopts, &option_index)) != EOF)
    {

        switch (opt)
        {
        /* portmask */
        case 'p':
            l2fwd_enabled_port_mask = l2fwd_parse_portmask(optarg);
            if (l2fwd_enabled_port_mask == 0)
            {
                printf("invalid portmask\n");
                l2fwd_usage(prgname);
                return -1;
            }
            break;
        case 'P':
            promiscuous_on = 1;
            break;

        /* nqueue */
        case 'q':
            l2fwd_rx_queue_per_lcore = l2fwd_parse_nqueue(optarg);
            if (l2fwd_rx_queue_per_lcore == 0)
            {
                printf("invalid queue number\n");
                l2fwd_usage(prgname);
                return -1;
            }
            break;

        /* timer period */
        case 'T':
            timer_secs = l2fwd_parse_timer_period(optarg);
            if (timer_secs < 0)
            {
                printf("invalid timer period\n");
                l2fwd_usage(prgname);
                return -1;
            }
            timer_period = timer_secs;
            break;

        /* long options */
        case CMD_LINE_OPT_PORTMAP_NUM:
            ret = l2fwd_parse_port_pair_config(optarg);
            if (ret)
            {
                fprintf(stderr, "Invalid config\n");
                l2fwd_usage(prgname);
                return -1;
            }
            break;

        case CMD_LINE_OPT_NO_MAC_UPDATING_NUM:
            mac_updating = 0;
            break;

        default:
            l2fwd_usage(prgname);
            return -1;
        }
    }

    if (optind >= 0)
        argv[optind - 1] = prgname;

    ret = optind - 1;
    optind = 1; /* reset getopt lib */
    return ret;
}

/*
 * Check port pair config with enabled port mask,
 * and for valid port pair combinations.
 */
// 检查port的配置并且进行端口配对
static int
check_port_pair_config(void)
{
    uint32_t port_pair_config_mask = 0;
    uint32_t port_pair_mask = 0;
    uint16_t index, i, portid;

    for (index = 0; index < nb_port_pair_params; index++)
    {
        port_pair_mask = 0;

        for (i = 0; i < NUM_PORTS; i++)
        {
            portid = port_pair_params[index].port[i];
            if ((l2fwd_enabled_port_mask & (1 << portid)) == 0)
            {
                printf("port %u is not enabled in port mask\n",
                       portid);
                return -1;
            }
            if (!rte_eth_dev_is_valid_port(portid))
            {
                printf("port %u is not present on the board\n",
                       portid);
                return -1;
            }

            port_pair_mask |= 1 << portid;
        }

        if (port_pair_config_mask & port_pair_mask)
        {
            printf("port %u is used in other port pairs\n", portid);
            return -1;
        }
        port_pair_config_mask |= port_pair_mask;
    }

    l2fwd_enabled_port_mask &= port_pair_config_mask;

    return 0;
}

/* Check the link status of all ports in up to 9s, and print them finally */
// 检查开始时port的初始化,状态
static void
check_all_ports_link_status(uint32_t port_mask)
{
#define CHECK_INTERVAL 100 /* 100ms */
#define MAX_CHECK_TIME 90  /* 9s (90 * 100ms) in total */
    uint16_t portid;
    uint8_t count, all_ports_up, print_flag = 0;
    struct rte_eth_link link;
    int ret;
    char link_status_text[RTE_ETH_LINK_MAX_STR_LEN];

    printf("\nChecking link status");
    fflush(stdout);
    for (count = 0; count <= MAX_CHECK_TIME; count++)
    {
        if (force_quit)
            return;
        all_ports_up = 1;
        RTE_ETH_FOREACH_DEV(portid)
        {
            if (force_quit)
                return;
            if ((port_mask & (1 << portid)) == 0)
                continue;
            memset(&link, 0, sizeof(link));
            ret = rte_eth_link_get_nowait(portid, &link);
            if (ret < 0)
            {
                all_ports_up = 0;
                if (print_flag == 1)
                    printf("Port %u link get failed: %s\n",
                           portid, rte_strerror(-ret));
                continue;
            }
            /* print link status if flag set */
            if (print_flag == 1)
            {
                rte_eth_link_to_str(link_status_text,
                                    sizeof(link_status_text), &link);
                printf("Port %d %s\n", portid,
                       link_status_text);
                continue;
            }
            /* clear all_ports_up flag if any link down */
            if (link.link_status == RTE_ETH_LINK_DOWN)
            {
                all_ports_up = 0;
                break;
            }
        }
        /* after finally printing all link status, get out */
        if (print_flag == 1)
            break;

        if (all_ports_up == 0)
        {
            printf(".");
            fflush(stdout);
            rte_delay_ms(CHECK_INTERVAL);
        }

        /* set the print_flag if all ports up or timeout */
        // 到这里就是成功了
        if (all_ports_up == 1 || count == (MAX_CHECK_TIME - 1))
        {
            print_flag = 1;
            printf("done\n");
        }
    }
}

static void
signal_handler(int signum)
{
    if (signum == SIGINT || signum == SIGTERM)
    {
        printf("\n\nSignal %d received, preparing to exit...\n",
               signum);
        force_quit = true;
    }
}

int main(int argc, char **argv)
{
    struct lcore_queue_conf *qconf;
    int ret;
    uint16_t nb_ports;
    uint16_t nb_ports_available = 0;
    uint16_t portid, last_port;
    unsigned lcore_id, rx_lcore_id;
    unsigned nb_ports_in_mask = 0;
    unsigned int nb_lcores = 0;
    unsigned int nb_mbufs;

    start_cycles = rte_get_timer_cycles();
    /* Init EAL. 8< */
    // 环境初始化
    ret = rte_eal_init(argc, argv);
    if (ret < 0)
        rte_exit(EXIT_FAILURE, "Invalid EAL arguments\n");

    // 因为这里是分为两部分参数,eal参数和应用本身的参数
    argc -= ret;
    argv += ret;

    force_quit = false;
    signal(SIGINT, signal_handler);
    signal(SIGTERM, signal_handler);

    /* parse application arguments (after the EAL ones) */
    ret = l2fwd_parse_args(argc, argv);
    if (ret < 0)
        rte_exit(EXIT_FAILURE, "Invalid L2FWD arguments\n");
    /* >8 End of init EAL. */

    printf("MAC updating %s\n", mac_updating ? "enabled" : "disabled");

    /* convert to number of cycles */
    timer_period *= rte_get_timer_hz();

    // 获取可用网卡的数量
    nb_ports = rte_eth_dev_count_avail();
    if (nb_ports == 0)
        rte_exit(EXIT_FAILURE, "No Ethernet ports - bye\n");

    if (port_pair_params != NULL)
    {
        if (check_port_pair_config() < 0)
            rte_exit(EXIT_FAILURE, "Invalid port pair config\n");
    }

    /* check port mask to possible port mask */
    if (l2fwd_enabled_port_mask & ~((1 << nb_ports) - 1))
        rte_exit(EXIT_FAILURE, "Invalid portmask; possible (0x%x)\n",
                 (1 << nb_ports) - 1);

    /* Initialization of the driver. 8< */

    /* reset l2fwd_dst_ports */
    for (portid = 0; portid < RTE_MAX_ETHPORTS; portid++)
        l2fwd_dst_ports[portid] = 0;
    last_port = 0;

    /* populate destination port details */
    if (port_pair_params != NULL)
    {
        uint16_t idx, p;

        for (idx = 0; idx < (nb_port_pair_params << 1); idx++)
        {
            p = idx & 1;
            portid = port_pair_params[idx >> 1].port[p];
            l2fwd_dst_ports[portid] =
                port_pair_params[idx >> 1].port[p ^ 1];
        }
    }
    else
    {
        RTE_ETH_FOREACH_DEV(portid)
        {
            /* skip ports that are not enabled */
            if ((l2fwd_enabled_port_mask & (1 << portid)) == 0)
                continue;

            if (nb_ports_in_mask % 2)
            {
                l2fwd_dst_ports[portid] = last_port;
                l2fwd_dst_ports[last_port] = portid;
            }
            else
            {
                last_port = portid;
            }

            nb_ports_in_mask++;
        }
        if (nb_ports_in_mask % 2)
        {
            printf("Notice: odd number of ports in portmask.\n");
            l2fwd_dst_ports[last_port] = last_port;
        }
    }
    /* >8 End of initialization of the driver. */

    rx_lcore_id = 0;
    qconf = NULL;

    /* Initialize the port/queue configuration of each logical core */
    RTE_ETH_FOREACH_DEV(portid)
    {
        /* skip ports that are not enabled */
        if ((l2fwd_enabled_port_mask & (1 << portid)) == 0)
            continue;

        /* get the lcore_id for this port */
        // 寻找可用的逻辑核心
        // 相当于是从0号开始向后遍历
        // 当disabled或者承载量满了的时候就向后遍历
        while (rte_lcore_is_enabled(rx_lcore_id) == 0 ||
               lcore_queue_conf[rx_lcore_id].n_rx_port ==
                   l2fwd_rx_queue_per_lcore)
        {
            rx_lcore_id++;
            if (rx_lcore_id >= RTE_MAX_LCORE)
                rte_exit(EXIT_FAILURE, "Not enough cores\n");
        }

        // 如果没有出现上面的两种情况就直接进行lcore的绑定
        if (qconf != &lcore_queue_conf[rx_lcore_id])
        {
            /* Assigned a new logical core in the loop above. */
            qconf = &lcore_queue_conf[rx_lcore_id];
            nb_lcores++;
        }

        qconf->rx_port_list[qconf->n_rx_port] = portid;
        qconf->n_rx_port++;
        printf("Lcore %u: RX port %u TX port %u\n", rx_lcore_id,
               portid, l2fwd_dst_ports[portid]);
    }

    nb_mbufs = RTE_MAX(nb_ports * (nb_rxd + nb_txd + MAX_PKT_BURST +
                                   nb_lcores * MEMPOOL_CACHE_SIZE),
                       8192U);

    /* Create the mbuf pool. 8< */
    // 创建mbuf pool来使用
    // 注意这里创建了8bytes的私有区,我们想用来存放是否是已经转发过的字段
    l2fwd_pktmbuf_pool = rte_pktmbuf_pool_create("mbuf_pool", nb_mbufs,
                                                 MEMPOOL_CACHE_SIZE, sizeof(uint64_t), RTE_MBUF_DEFAULT_BUF_SIZE,
                                                 rte_socket_id());
    if (l2fwd_pktmbuf_pool == NULL)
        rte_exit(EXIT_FAILURE, "Cannot init mbuf pool\n");
    /* >8 End of create the mbuf pool. */

    /* create rings (single-producer single-consumer flags if you guarantee 1 prod/1 cons,
     * else pass 0 for general case)
     */
    // 对每个端口进行初始化
    /* Initialise each port */
    RTE_ETH_FOREACH_DEV(portid)
    {
        struct rte_eth_rxconf rxq_conf;
        struct rte_eth_txconf txq_conf;
        struct rte_eth_conf local_port_conf = port_conf;
        struct rte_eth_dev_info dev_info;

        /* skip ports that are not enabled */
        if ((l2fwd_enabled_port_mask & (1 << portid)) == 0)
        {
            printf("Skipping disabled port %u\n", portid);
            continue;
        }
        nb_ports_available++;

        /* init port */
        printf("Initializing port %u... ", portid);
        fflush(stdout);

        ret = rte_eth_dev_info_get(portid, &dev_info);
        if (ret != 0)
            rte_exit(EXIT_FAILURE,
                     "Error during getting device (port %u) info: %s\n",
                     portid, strerror(-ret));

        if (dev_info.tx_offload_capa & RTE_ETH_TX_OFFLOAD_MBUF_FAST_FREE)
            local_port_conf.txmode.offloads |=
                RTE_ETH_TX_OFFLOAD_MBUF_FAST_FREE;
        /* Configure the number of queues for a port. */
        // 这里每个网卡上就配置了一个RX和一个TX队列.
        ret = rte_eth_dev_configure(portid, 1, 1, &local_port_conf);
        if (ret < 0)
            rte_exit(EXIT_FAILURE, "Cannot configure device: err=%d, port=%u\n",
                     ret, portid);
        /* >8 End of configuration of the number of queues for a port. */

        ret = rte_eth_dev_adjust_nb_rx_tx_desc(portid, &nb_rxd,
                                               &nb_txd);
        if (ret < 0)
            rte_exit(EXIT_FAILURE,
                     "Cannot adjust number of descriptors: err=%d, port=%u\n",
                     ret, portid);

        ret = rte_eth_macaddr_get(portid,
                                  &l2fwd_ports_eth_addr[portid]);
        if (ret < 0)
            rte_exit(EXIT_FAILURE,
                     "Cannot get MAC address: err=%d, port=%u\n",
                     ret, portid);

        /* init one RX queue */
        fflush(stdout);
        rxq_conf = dev_info.default_rxconf;
        rxq_conf.offloads = local_port_conf.rxmode.offloads;
        /* RX queue setup. 8< */
        ret = rte_eth_rx_queue_setup(portid, 0, nb_rxd,
                                     rte_eth_dev_socket_id(portid),
                                     &rxq_conf,
                                     l2fwd_pktmbuf_pool);
        if (ret < 0)
            rte_exit(EXIT_FAILURE, "rte_eth_rx_queue_setup:err=%d, port=%u\n",
                     ret, portid);
        /* >8 End of RX queue setup. */

        /* Init one TX queue on each port. 8< */
        fflush(stdout);
        txq_conf = dev_info.default_txconf;
        txq_conf.offloads = local_port_conf.txmode.offloads;
        ret = rte_eth_tx_queue_setup(portid, 0, nb_txd,
                                     rte_eth_dev_socket_id(portid),
                                     &txq_conf);
        if (ret < 0)
            rte_exit(EXIT_FAILURE, "rte_eth_tx_queue_setup:err=%d, port=%u\n",
                     ret, portid);
        /* >8 End of init one TX queue on each port. */

        /* Initialize TX buffers */
        tx_buffer[portid] = rte_zmalloc_socket("tx_buffer",
                                               RTE_ETH_TX_BUFFER_SIZE(MAX_PKT_BURST), 0,
                                               rte_eth_dev_socket_id(portid));
        if (tx_buffer[portid] == NULL)
            rte_exit(EXIT_FAILURE, "Cannot allocate buffer for tx on port %u\n",
                     portid);

        rte_eth_tx_buffer_init(tx_buffer[portid], MAX_PKT_BURST);

        ret = rte_eth_tx_buffer_set_err_callback(tx_buffer[portid],
                                                 rte_eth_tx_buffer_count_callback,
                                                 &port_statistics[portid].dropped);
        if (ret < 0)
            rte_exit(EXIT_FAILURE,
                     "Cannot set error callback for tx buffer on port %u\n",
                     portid);

        ret = rte_eth_dev_set_ptypes(portid, RTE_PTYPE_UNKNOWN, NULL,
                                     0);
        if (ret < 0)
            printf("Port %u, Failed to disable Ptype parsing\n",
                   portid);
        /* Start device */
        ret = rte_eth_dev_start(portid);
        if (ret < 0)
            rte_exit(EXIT_FAILURE, "rte_eth_dev_start:err=%d, port=%u\n",
                     ret, portid);

        printf("done: \n");
        if (promiscuous_on)
        {
            ret = rte_eth_promiscuous_enable(portid);
            if (ret != 0)
                rte_exit(EXIT_FAILURE,
                         "rte_eth_promiscuous_enable:err=%s, port=%u\n",
                         rte_strerror(-ret), portid);
        }

        printf("Port %u, MAC address: " RTE_ETHER_ADDR_PRT_FMT "\n\n",
               portid,
               RTE_ETHER_ADDR_BYTES(&l2fwd_ports_eth_addr[portid]));

        /* initialize port stats */
        memset(&port_statistics, 0, sizeof(port_statistics));
    }

    if (!nb_ports_available)
    {
        rte_exit(EXIT_FAILURE,
                 "All available ports are disabled. Please set portmask.\n");
    }

    check_all_ports_link_status(l2fwd_enabled_port_mask);

    /*----------lcore启动流程------------*/

    /* ring 创建：为每个crypto worker创建独立的ring队列对 */
    printf("\n========== Creating Ring Queues ==========\n");
    for (unsigned i = 0; i < MAX_CRYPTO_WORKERS; i++)
    {
        char ring_name[32];

        /* 创建 RX -> Crypto ring */
        snprintf(ring_name, sizeof(ring_name), "RX_TO_CRYPTO_%u", i);
        rx_to_crypto[i] = rte_ring_create(ring_name, RING_SIZE,
                                          rte_socket_id(), 0);
        if (rx_to_crypto[i] == NULL)
            rte_exit(EXIT_FAILURE, "Cannot create rx_to_crypto[%u] ring\n", i);

        /* 创建 Crypto -> TX ring */
        snprintf(ring_name, sizeof(ring_name), "CRYPTO_TO_TX_%u", i);
        crypto_to_tx[i] = rte_ring_create(ring_name, RING_SIZE,
                                          rte_socket_id(), 0);
        if (crypto_to_tx[i] == NULL)
            rte_exit(EXIT_FAILURE, "Cannot create crypto_to_tx[%u] ring\n", i);

        printf("  Created ring pair #%u: %s <-> %s\n", i, "RX_TO_CRYPTO", "CRYPTO_TO_TX");
    }
    printf("Successfully created %d ring pairs!\n", MAX_CRYPTO_WORKERS);
    printf("==========================================\n\n");

    /* 选择所有可用的 crypto lcore：优先选择已 enable 且没有被分配 RX 的 lcore */
    unsigned crypto_lcores[RTE_MAX_LCORE];
    unsigned nb_crypto_lcores = 0;
    unsigned l;

    /* 遍历所有启用的lcore,找出没有被分配RX任务的作为crypto worker */
    for (l = rte_get_next_lcore(rte_get_main_lcore(), 1, 0);
         l != RTE_MAX_LCORE;
         l = rte_get_next_lcore(l, 1, 0))
    {
        if (lcore_queue_conf[l].n_rx_port == 0)
        {
            crypto_lcores[nb_crypto_lcores++] = l;
        }
    }

    if (nb_crypto_lcores == 0)
    {
        printf("WARNING: No available lcore for crypto workers!\n");
        printf("Please use more lcores (e.g., -l 0-3 for 4 cores)\n");
    }
    else
    {
        printf("\n========== Crypto Worker Configuration ==========\n");
        printf("Launching %u crypto worker(s)\n", nb_crypto_lcores);

        /* 启动所有crypto workers，传递worker_id */
        for (unsigned i = 0; i < nb_crypto_lcores; i++)
        {
            /* 传递worker ID给crypto_loop，用于访问专属ring队列 */
            int r = rte_eal_remote_launch(crypto_loop, (void *)(uintptr_t)i, crypto_lcores[i]);
            if (r != 0)
            {
                printf("  Failed to launch crypto on lcore %u: %d\n", crypto_lcores[i], r);
            }
            else
            {
                printf("  Crypto worker #%u launched on lcore %u (using ring pair #%u)\n",
                       i, crypto_lcores[i], i);
            }
        }
        printf("==================================================\n\n");
    }

    /* 启动其它有 RX 的 lcore 来做 l2fwd（跳过所有 crypto workers） */
    /* 对 worker（非 main）使用 remote_launch；对 main 直接调用（如果 main 被分配 RX） */
    unsigned main_core = rte_get_main_lcore();
    for (l = rte_get_next_lcore(rte_get_main_lcore(), 1, 0);
         l != RTE_MAX_LCORE;
         l = rte_get_next_lcore(l, 1, 0))
    {
        /* 跳过所有crypto workers */
        bool is_crypto_worker = false;
        for (unsigned i = 0; i < nb_crypto_lcores; i++)
        {
            if (l == crypto_lcores[i])
            {
                is_crypto_worker = true;
                break;
            }
        }
        if (is_crypto_worker)
            continue;

        if (lcore_queue_conf[l].n_rx_port == 0)
            continue; /* 该 lcore 没有 RX 任务，不启动 l2fwd */

        int r = rte_eal_remote_launch(l2fwd_launch_one_lcore, NULL, l);
        if (r != 0)
        {
            printf("Failed to launch l2fwd on lcore %u: %d\n", l, r);
        }
        else
        {
            printf("Launched l2fwd on lcore %u\n", l);
        }
    }

    /* 如果 main core 被分配 RX（即有 qconf），就在 main 线程上运行 l2fwd（同步、阻塞） */
    /* 先检查main core是否是crypto worker */
    bool main_is_crypto = false;
    for (unsigned i = 0; i < nb_crypto_lcores; i++)
    {
        if (main_core == crypto_lcores[i])
        {
            main_is_crypto = true;
            break;
        }
    }

    if (!main_is_crypto && lcore_queue_conf[main_core].n_rx_port > 0)
    {
        printf("Running l2fwd on main core %u (this will block until exit)\n", main_core);
        l2fwd_launch_one_lcore(NULL); /* 该调用会阻塞直到 force_quit=true 并退出 main loop */
    }

    /* 等待所有 remote 启动的 lcore 结束（包括 crypto_lcore） */
    /* 遍历所有 worker 等待 */
    RTE_LCORE_FOREACH_WORKER(l)
    {
        if (l == main_core)
            continue; /* main 是本线程或已返回 */
        if (rte_eal_wait_lcore(l) < 0)
        {
            ret = -1;
            break;
        }
    }

    RTE_ETH_FOREACH_DEV(portid)
    {
        if ((l2fwd_enabled_port_mask & (1 << portid)) == 0)
            continue;

        printf("Closing port %d...", portid);
        ret = rte_eth_dev_stop(portid);
        if (ret != 0)
            printf("rte_eth_dev_stop: err=%d, port=%d\n",
                   ret, portid);
        rte_eth_dev_close(portid);
        printf(" Done\n");
    }

    /* clean up the EAL */
    rte_eal_cleanup();
    printf("Bye...\n");

    sleep(5);

    return ret;
}
