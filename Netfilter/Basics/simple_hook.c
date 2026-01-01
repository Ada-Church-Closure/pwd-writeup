#include <libnetfilter_queue/linux_nfnetlink_queue.h>
#include <libnfnetlink/linux_nfnetlink.h>
#include <stdio.h>
#include <sys/socket.h>
#include <unistd.h>
#include <netinet/in.h>
#include <linux/types.h>
#include <linux/netfilter.h> // 包含NF_ACCEPT NF_DROP--->结论常量
#include <libnetfilter_queue/libnetfilter_queue.h>

/**
  定义一个回调的callback函数,每当我们的网络队列中有这样一个packet,我们就会自动调用这个函数处理
*/
static int callback(struct nfq_q_handle *qh, struct nfgenmsg *nfmsg, struct nfq_data* nfa, void* data) {
    // 1.获取包的唯一id
    struct nfqnl_msg_packet_hdr *ph = nfq_get_msg_packet_hdr(nfa);
    uint32_t id = 0;
    if (ph) {
        // 网络序转成字节序
        id = ntohl(ph->packet_id);
    }

    // 2.See how big the payload is?
    // Change the direction of the pointer......
    unsigned char* payload_data;
    int len = nfq_get_payload(nfa, &payload_data);

    printf(">>>NF got the packted ID: %u, the packet Size: %d bytes\n", id, len);

    // Verdict the packet
    return nfq_set_verdict(qh, id, NF_ACCEPT, 0, NULL);

}

int main(void) {
    struct nfq_handle *h;
    struct nfq_q_handle *qh;
    int fd;
    int rv;
    char buf[4096] __attribute__ ((aligned));

    printf("Opening......\n");
    
    // 1.打开库的句柄
    h = nfq_open();
    if (!h) {
        fprintf(stderr, "Error during nfq_open()\n");
        return -1;
    }

    // 2.重新绑定IPV4协议族
    nfq_unbind_pf(h, AF_INET);
    if (nfq_bind_pf(h, AF_INET) < 0) {
        fprintf(stderr, "Error rebingding AF_INET\n");
        return -1;
    }

    // 3.创建一个队列(0),利用callback接口去处理
    qh = nfq_create_queue(h, 0, &callback, NULL);
    if (!qh) {
        fprintf(stderr, "Error during nfq_create_queue\n");
        return -1;
    }

    // 4.设置拷贝数据包的模式--->PACKET(整个包的大小)
    if (nfq_set_mode(qh, NFQNL_COPY_PACKET, 0xFFFF) < 0) {
        fprintf(stderr, "Error during nfq_set_mode()\n");
        return -1;
    }

    // 5.拿到文件描述符,跟读普通文件一样读取网络包
    fd = nfq_fd(h);

    // 6.等待数据包并且处理
    while ((rv = recv(fd, buf, sizeof(buf), 0)) && rv >= 0) {
        nfq_handle_packet(h, buf, rv);
    }

    printf("Handle Over\n");
    nfq_destroy_queue(qh);
    nfq_close(h);
    
    return 0;
}
