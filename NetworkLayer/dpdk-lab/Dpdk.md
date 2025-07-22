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

然后我们来处理某个网卡，但是我不想用物理网卡，害怕出问题：

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

> ​	那么我们的第一个样例就运行成功了。

## Programmer's Guide

> ​	我们主要学习dpdk.org上面的基本概念一类的东西。

### Intro

架构方面的信息	开发环境	优化的指导

### Overview

> DPDK的架构。
>
> 简单，快速，完整的框架。
>
> 创建EAL（**Environment Abstraction Layer**）

​	DPDK 的主要目标是为数据平面应用中的快速数据包处理，提供一个简单且完整的框架。用户可以使用这套代码来理解其中使用的一些技术，用于原型开发，或者添加自定义的协议栈。基于 DPDK 的其他生态系统选项也已存在。

​	该框架通过构建 **环境抽象层（EAL, Environment Abstraction Layer）**，为特定环境创建一组库。这个环境可以是 Intel 架构的某种模式（32 位或 64 位）、Linux 用户态编译器或某个平台。环境的构建依赖于 meson 文件和配置文件。**一旦 EAL 库被构建完成，用户就可以将其链接到自己的应用程序中**。除了 EAL，DPDK 还提供了其他库，如 Hash（哈希库）、最长前缀匹配（LPM）库以及环形队列（ring）库等。还提供了一些示例程序，帮助用户了解如何使用 DPDK 的各种功能。

​	DPDK 采用一种 **“运行至完成”**（run to completion）的数据包处理模型，即所有资源都必须在调用数据平面应用之前就分配好。这些应用作为执行单元在逻辑处理核心上运行。该模型**不支持调度器**，所有设备都通过**轮询（polling）**方式访问。不使用中断的主要原因是中断处理会引入性能开销。

​	除了运行至完成模型，DPDK 还支持一种 **流水线模型（pipeline model）**，可以通过 ring（环形队列）在多个核心之间传递数据包或消息。这种方式允许以多个阶段处理工作，并可能使核心上的代码使用更高效。

**开发环境：**

​	The DPDK project installation requires Linux and the associated toolchain, such as one or more compilers, assembler, meson utility, editor and various libraries to create the DPDK components and libraries.

​	When creating applications for the Linux user space, the glibc library is used.

**EAL给我们提供了什么功能：**

- DPDK loading and launching
- Support for multi-process and multi-thread execution types
- Core affinity/assignment procedures
- System memory allocation/de-allocation
- Atomic/lock operations
- Time reference
- PCI bus access
- Trace and debug functions
- CPU feature identification
- Interrupt handling
- Alarm operations
- Memory management (malloc)



#### Core Components

核心组成和依赖关系的问题，核心库，核心函数,分别的功能是什么。

![../_images/architecture-overview.svg](https://doc.dpdk.org/guides/_images/architecture-overview.svg)

术语表：

- ACL

  Access Control List

- API

  Application Programming Interface

- ASLR

  Linux* kernel Address-Space Layout Randomization

- BSD

  Berkeley Software Distribution

- Clr

  Clear

- CIDR

  Classless Inter-Domain Routing

- Control Plane

  The control plane is concerned with the routing of packets and with providing a start or end point.

- Core

  A core may include several lcores or threads if the processor supports hyperthreading.

- Core Components

  A set of libraries provided by the DPDK, including eal, ring, mempool, mbuf, timers, and so on.

- CPU

  Central Processing Unit

- CRC

  Cyclic Redundancy Check

- Data Plane

  In contrast to the control plane, the data plane in a network architecture are the layers involved when forwarding packets. These layers must be highly optimized to achieve good performance.

- DIMM

  Dual In-line Memory Module

- Doxygen

  A documentation generator used in the DPDK to generate the API reference.

- DPDK

  Data Plane Development Kit

- DRAM

  Dynamic Random Access Memory

- EAL

  The Environment Abstraction Layer (EAL) provides a generic interface that hides the environment specifics from the applications and libraries. The services expected from the EAL are: development kit loading and launching, core affinity/ assignment procedures, system memory allocation/description, PCI bus access, inter-partition communication.

- FIFO

  First In First Out

- FPGA

  Field Programmable Gate Array

- GbE

  Gigabit Ethernet

- HW

  Hardware

- HPET

  High Precision Event Timer; a hardware timer that provides a precise time reference on x86 platforms.

- ID

  Identifier

- IOCTL

  Input/Output Control

- I/O

  Input/Output

- IP

  Internet Protocol

- IPv4

  Internet Protocol version 4

- IPv6

  Internet Protocol version 6

- lcore

  A logical execution unit of the processor, sometimes called a *hardware thread*.

- L1

  Layer 1

- L2

  Layer 2

- L3

  Layer 3

- L4

  Layer 4

- LAN

  Local Area Network

- LPM

  Longest Prefix Match

- main lcore

  The execution unit that executes the main() function and that launches other lcores.

- master lcore

  Deprecated name for *main lcore*. No longer used.

- mbuf

  An mbuf is a data structure used internally to carry messages (mainly network packets). The name is derived from BSD stacks. To understand the concepts of packet buffers or mbuf, refer to *TCP/IP Illustrated, Volume 2: The Implementation*.

- MESI

  Modified Exclusive Shared Invalid (CPU cache coherency protocol)

- MTU

  Maximum Transfer Unit

- NIC

  Network Interface Card

- OOO

  Out Of Order (execution of instructions within the CPU pipeline)

- NUMA

  Non-uniform Memory Access

- PCI

  Peripheral Connect Interface

- PHY

  An abbreviation for the physical layer of the OSI model.

- PIE

  Proportional Integral Controller Enhanced (RFC8033)

- pktmbuf

  An *mbuf* carrying a network packet.

- PMD

  Poll Mode Driver

- PMU

  Performance Monitoring Unit

- QoS

  Quality of Service

- RCU

  Read-Copy-Update algorithm, an alternative to simple rwlocks.

- Rd

  Read

- RED

  Random Early Detection

- RSS

  Receive Side Scaling

- RTE

  Run Time Environment. Provides a fast and simple framework for fast packet processing, in a lightweight environment as a Linux* application and using Poll Mode Drivers (PMDs) to increase speed.

- Rx

  Reception

- Slave lcore

  Deprecated name for *worker lcore*. No longer used.

- Socket

  For historical reasons, the term “socket” is used in the DPDK to refer to both physical sockets, as well as NUMA nodes. As a general rule, the term should be understood to mean “NUMA node” unless it is clear from context that it is referring to physical CPU sockets.

- SLA

  Service Level Agreement

- srTCM

  Single Rate Three Color Marking

- SRTD

  Scheduler Round Trip Delay

- SW

  Software

- Target

  In the DPDK, the target is a combination of architecture, machine, executive environment and toolchain. For example: i686-native-linux-gcc.

- TCP

  Transmission Control Protocol

- TC

  Traffic Class

- TLB

  Translation Lookaside Buffer

- TLS

  Thread Local Storage

- trTCM

  Two Rate Three Color Marking

- TSC

  Time Stamp Counter

- Tx

  Transmission

- TUN/TAP

  TUN and TAP are virtual network kernel devices.

- VLAN

  Virtual Local Area Network

- Wr

  Write

- Worker lcore

  Any *lcore* that is not the *main lcore*.

- WRED

  Weighted Random Early Detection

- WRR

  Weighted Round Robin

## Sample Applications

> ​	通过一些简单样例的学习来掌握DPDK。
>
> ​	简单，独立的程序。
>
> ​	每个试图表现不同的特性。 

### Compiling

> ​	编译的方法。git clone下来接着按照文档操作一遍。
>

首先环境配置：

```sh
cd ~  # 或你喜欢的工作目录，比如 ~/dev
git clone https://github.com/DPDK/dpdk.git
cd dpdk
meson setup build
cd build
meson configure -Dexamples=all
ninja
```

Go to DPDK build directory:

> ```
> cd dpdk/<build_dir>
> ```

Enable examples compilation:

> ```
> meson configure -Dexamples=all
> ```

Build:

> ```
> ninja
> ```

### Command Line Sample Application

> ​	命令行，这个程序就是展示交互API的使用方法。

There are three simple commands:

- add obj_name IP: Add a new object with an IP/IPv6 address associated to it.
- del obj_name: Delete the specified object.
- show obj_name: Show the IP associated with the specified object.

退出命令行 ，Ctrl + D

运行命令。

> ​	那么注意，要分配hugepage,并且用sudo分配和执行命令。

To run the application in a Linux environment, issue the following command:

```sh
$ ./<build_dir>/examples/dpdk-cmdline -l 0-3 -n 4
```

Refer to the *DPDK Getting Started Guide* for general information on running applications and the Environment Abstraction Layer (EAL) options.

```sh
❯ sudo ./examples/dpdk-cmdline -l 0-3 -n 4                                                              
EAL: Detected CPU lcores: 32
EAL: Detected NUMA nodes: 1
EAL: Detected static linkage of DPDK
EAL: Multi-process socket /var/run/dpdk/rte/mp_socket
EAL: Selected IOVA mode 'VA'
example> help
Demo example of command line interface in RTE

This is a readline-like interface that can be used to
debug your RTE application. It supports some features
of GNU readline like completion, cut/paste, and some
other special bindings.

This demo shows how rte_cmdline library can be
extended to handle a list of objects. There are
3 commands:
- add obj_name IP
- del obj_name
- show obj_name
```











































































