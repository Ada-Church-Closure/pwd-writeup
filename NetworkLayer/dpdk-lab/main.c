#include <stdio.h>
#include <stdint.h>
#include <inttypes.h>
#include <rte_eal.h>
#include <rte_ethdev.h>
#include <rte_mbuf.h>

#define NUM_MBUFS 8191
#define MBUF_CACHE_SIZE 250
#define BURST_SIZE 32

static int pkt_count = 0;

int main(int argc, char *argv[]) {
    int ret = rte_eal_init(argc, argv);
    if (ret < 0) rte_exit(EXIT_FAILURE, "Error with EAL init\n");

    uint16_t port_id = 0;
    if (!rte_eth_dev_is_valid_port(port_id))
        rte_exit(EXIT_FAILURE, "Invalid port\n");

    struct rte_mempool *mbuf_pool = rte_pktmbuf_pool_create("MBUF_POOL",
        NUM_MBUFS * 2, MBUF_CACHE_SIZE, 0, RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());

    if (mbuf_pool == NULL)
        rte_exit(EXIT_FAILURE, "Cannot create mbuf pool\n");

    struct rte_eth_conf port_conf = {0};
    ret = rte_eth_dev_configure(port_id, 1, 1, &port_conf);
    if (ret < 0)
        rte_exit(EXIT_FAILURE, "Cannot configure device: err=%d\n", ret);

    ret = rte_eth_rx_queue_setup(port_id, 0, 128, rte_eth_dev_socket_id(port_id), NULL, mbuf_pool);
    if (ret < 0)
        rte_exit(EXIT_FAILURE, "RX queue setup failed\n");

    ret = rte_eth_dev_start(port_id);
    if (ret < 0)
        rte_exit(EXIT_FAILURE, "Device start failed\n");

    printf("Started DPDK RX on port %u\n", port_id);

    struct rte_mbuf *bufs[BURST_SIZE];
    while (1) {
        const uint16_t nb_rx = rte_eth_rx_burst(port_id, 0, bufs, BURST_SIZE);
        if (nb_rx == 0) continue;
        
        ++pkt_count;
        for (int i = 0; i < nb_rx; i++) {
            struct rte_mbuf *mbuf = bufs[i];
            struct rte_ether_hdr *eth_hdr = rte_pktmbuf_mtod(mbuf, struct rte_ether_hdr *);
            printf("Got packet %d: SRC MAC = %02X:%02X:%02X:%02X:%02X:%02X\n",
                                         pkt_count,
                   eth_hdr->src_addr.addr_bytes[0],
                   eth_hdr->src_addr.addr_bytes[1],
                   eth_hdr->src_addr.addr_bytes[2],
                   eth_hdr->src_addr.addr_bytes[3],
                   eth_hdr->src_addr.addr_bytes[4],
                   eth_hdr->src_addr.addr_bytes[5]);
            rte_pktmbuf_free(mbuf);
        }
    }

    return 0;
}
