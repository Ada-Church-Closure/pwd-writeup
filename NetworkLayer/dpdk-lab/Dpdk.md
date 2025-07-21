# Dpdk入门

了解区别：

| 特性     |          Socket           |          libpcap           |                  DPDK                  |
| :------: | :---------------------------: | :----------------------------: | :----------------------------------------: |
| 所在层次 |  应用层 + 传输层（TCP/UDP）   |    数据链路层（抓包/监听）     |        数据链路层（收发包）绕过内核        |
| 接口形态 | 内核 API（如 `send`, `recv`） | 被动抓包 API（如 `pcap_loop`） |   用户态高速库（如 `rte_eth_rx_burst`）    |
| 性能     |    一般（syscall + 拷贝）     |   中等（零拷贝但需内核参与）   |     极高（zero-copy，轮询，NUMA优化）      |
| 用途     |      写**服务器/客户端**程序      |   写**抓包、检测、流量分析工具**   | 高性能数据面（防火墙、交换机、负载均衡等） |
| 示例项目 |     Web Server / Chat App     |    Snort / Wireshark / IDS     | l3fwd / l2fwd / 高速防火墙 / 数据平面协议  |

了解先阅读一下文档：https://doc.dpdk.org/guides/linux_gsg/intro.html

## Data Plane Development Kit

首先分配大页。

```sh
echo 2048 | sudo tee /proc/sys/vm/nr_hugepages
```

✅ 解释：

- `nr_hugepages` 是 Linux 内核参数，表示当前系统可用的 “2MB 大页内存块” 的数量。
- 这条命令设置系统分配 **2048 个 2MB 大页**，也就是约 **4GB 的物理内存**专用于 **DPDK** 或其他用户态高速程序使用。

❓为什么需要 **HugePages**？

- Linux 默认内存页是 4KB，管理成本高。
- DPDK 使用大页能提升缓存命中率、减少 TLB（页表）miss，大幅提升数据包处理性能。
- **DPDK 不使用内核网络栈，它直接分配大页内存自己管理数据包（用 rte_mbuf 表示**

接着把这部分进行挂载的处理。

```sh
sudo mkdir -p /mnt/huge
sudo mount -t hugetlbfs none /mnt/huge
```



✅ 解释：

- `hugetlbfs` 是 Linux 提供的 “**大页内存专用文件系统**”，DPDK 就是从这里分配大页内存。

- 你必须手动挂载这个文件系统，让 DPDK 的程序能访问这些大页。

- `/mnt/huge` 是推荐的挂载点路径（也可自定义）

  none:这是一个**虚拟**的，没有实际对应设备的**文件系统**。

  重点解释：

  - `-t hugetlbfs`：告诉 `mount` 使用的文件系统类型是 `hugetlbfs`（即 HugePage 专用的内存文件系统）。
  - `none`：这里指的是 **“伪设备名”**，**不是实际设备路径**。
  - `/mnt/huge`：是挂载点目录，也就是你要挂载到的路径。

  ------

  🧠 所以 `none` 是什么意思？

  - 它代表**“这个文件系统没有实际的设备对应（不是 /dev/sdX 这样的存储设备）”**。
  - 因为 `hugetlbfs` 是一种**基于内存的虚拟文件系统**，不需要底层设备支持，所以写 `none` 是一种惯例。

当我们分配结束之后，可以查看我们内存的关于HugePage的分配情况。

```sh
❯ cat /proc/meminfo | grep Huge                                               
AnonHugePages:    614400 kB
ShmemHugePages:        0 kB
FileHugePages:    307200 kB
HugePages_Total:    2048
HugePages_Free:     2048
HugePages_Rsvd:        0
HugePages_Surp:        0
Hugepagesize:       2048 kB
Hugetlb:         4194304 kB
```



DPDK编译和运行的环境变量：

```sh
echo 'export RTE_SDK=/usr/share/dpdk' >> ~/.bashrc
echo 'export RTE_TARGET=x86_64-native-linuxapp-gcc' >> ~/.bashrc
source ~/.bashrc
```



|  环境变量名  |                             用途                             |
| :----------: | :----------------------------------------------------------: |
|  `RTE_SDK`   | 指向 DPDK 的源码或安装目录（源码安装时为 `~/dpdk`，pacman 装好的是 `/usr/share/dpdk`） |
| `RTE_TARGET` | 编译目标平台：`x86_64` + `native` + `linuxapp` + `gcc`。即使用本机架构、Linux 应用程序模式、GCC 编译器 |

### 第一个dpdk程序：

```C
#include <stdio.h>
#include <stdint.h>
#include <inttypes.h>
#include <rte_eal.h>
#include <rte_ethdev.h>
#include <rte_mbuf.h>

#define NUM_MBUFS 8191
#define MBUF_CACHE_SIZE 250
#define BURST_SIZE 32

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

        for (int i = 0; i < nb_rx; i++) {
            struct rte_mbuf *mbuf = bufs[i];
            struct rte_ether_hdr *eth_hdr = rte_pktmbuf_mtod(mbuf, struct rte_ether_hdr *);
            printf("Got packet: SRC MAC = %02X:%02X:%02X:%02X:%02X:%02X\n",
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
```

> ​	由于我们不熟悉，接下来我们逐行解析这个C文件。

#### 1.首先是DPDK需要什么头文件：

```C
#include <rte_eal.h>      // 初始化 EAL（环境抽象层）
#include <rte_ethdev.h>   // 网卡设备相关 API
#include <rte_mbuf.h>     // 数据包缓冲区（mbuf）操作
```



#### 2.一些常量：

```C
#define NUM_MBUFS 8191
#define MBUF_CACHE_SIZE 250
#define BURST_SIZE 32
```

`NUM_MBUFS`：数据包缓冲池里分配多少个 **mbuf**（数据包结构体）。

`MBUF_CACHE_SIZE`：每个 **core** 上最多 cache 多少个 **mbuf**，提升性能。

`BURST_SIZE`：每次最多收多少个数据包（批处理收包）。



#### 3.初始化DPDK的环境：

```C
rte_eal_init(argc, argv);
```

初始化 DPDK，解析命令行参数

创建 CPU 核心线程、内存池、IOMMU、HugePages 映射等

这一步很重要，所有操作都建立在它之后

#### 4.检查是否有可用的端口：

```C
if (!rte_eth_dev_is_valid_port(port_id))
    rte_exit(EXIT_FAILURE, "Invalid port\n");
```



确保 `port_id = 0` 这块网卡存在并且已经绑定 DPDK 驱动（如 vfio-pci）



#### 5.创建一个内存池用来存放接收队列拿过来的数据包：



```C
struct rte_mempool *mbuf_pool = rte_pktmbuf_pool_create(...);
```

建一个名为 `"MBUF_POOL"` 的数据包内存池（mbuf 是 DPDK 管理的 packet 对象）

分配好的 `mbuf_pool` 会被用于接收队列收上来的数据包



#### 6.配置网卡的收发参数：

```C
rte_eth_dev_configure(port_id, 1, 1, &port_conf);
```

`1, 1`：开启 1 个接收队列（RX）和 1 个发送队列（TX）

`port_conf`：可设置一些网卡行为，这里我们传了个默认空结构体

#### 7.设置一个接收的队列：

```C
rte_eth_rx_queue_setup(port_id, 0, 128, ..., mbuf_pool);
```

把 `port 0` 的 `queue 0` 设置为：最多 128 个描述符，使用上面的 `mbuf_pool`

这是真正把「内存」和「网卡接收队列」连接起来

#### 8.接着启动网卡：

```C
rte_eth_dev_start(port_id);
```



启动网卡收发功能

到这里你已经完成了网卡绑定 + 内存池 + 接收队列 + 启动

#### 9.接受并且处理数据包，这是主循环：

```C
const uint16_t nb_rx = rte_eth_rx_burst(port_id, 0, bufs, BURST_SIZE);
```



从 port 0 的 RX queue 0 接收最多 `BURST_SIZE` 个包

存到 `bufs[]` 数组里，每个元素是一个 `struct rte_mbuf *`

#### 10.遍历每个数据包并且打印地址：

```C
struct rte_ether_hdr *eth_hdr = rte_pktmbuf_mtod(mbuf, struct rte_ether_hdr *);
```

把 `mbuf` 指向的数据部分，转为以太网头指针（Ethernet header）

然后打印 `eth_hdr->src_addr`，就是源 MAC 地址

#### 11.最后释放数据包，防止出现内存泄漏：

```C
rte_pktmbuf_free(mbuf);
```



> ​	要运行这个程序，我们要让一块网卡绑定到DPDK支持的驱动。

DPDK给定的工具在这个目录里：/usr/bin/dpdk-devbind.py

#### build

在你的C文件目录下面：

```sh
# 在 dpdk-lab 目录下                                               
meson setup build
ninja -C build
```

来构建这个项目，不用cMake之类的工具了。

然后我们用来来处理某个网卡，但是我不想用物理网卡，害怕出问题：

先用tcpdump抓一个包：

```sh
sudo tcpdump -i wlan0 -c 200 -w test.pcap
```



接着再绑定到一个虚拟网卡上面来运行程序：

```sh
sudo ./build/dpdk_rx_mac -l 0 -n 4 --vdev 'net_pcap0,rx_pcap=test.pcap' -- -p 0x1
```



出现这样的结果：

```sh
Got packet: SRC MAC = 9C:71:3A:F5:DC:91
Got packet: SRC MAC = 9C:71:3A:F5:DC:91
Got packet: SRC MAC = 9C:71:3A:F5:DC:91
Got packet: SRC MAC = 9C:71:3A:F5:DC:91
Got packet: SRC MAC = E4:0D:36:54:65:E1
Got packet: SRC MAC = 9C:71:3A:F5:DC:91
```

表明程序运行成功了。

> ​	那么我们的第一个样例就运行成功了。























































