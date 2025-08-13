# Dpdk入门

> 可以看一下github上有一个中文的指导仓库.

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

​	每当我们要跑代码的时候,利用usertools下的工具先对于大页内存进行设置,阅读一下help,一般按照以下的步骤来进行:(先分配2G,接着再进行挂载的操作)

​	我使用的是内存16GB的笔记本,当分配失败的时候(没有分配到合适的内存数量),应该是内存碎片化导致的,重新开机并且第一时间分配这部分大页内存.

```sh
❯ sudo python3 dpdk-hugepages.py --reserve 2G --pagesize 2M                                    
❯ sudo python3 dpdk-hugepages.py --show
Node  Pages  Size  Total  
0      1024   2Mb    2Gb  
No huge page filesystems mounted

❯ sudo python3 dpdk-hugepages.py --mount

❯ sudo python3 dpdk-hugepages.py --show
Node  Pages  Size  Total  
0      1024   2Mb    2Gb  

Huge page filesystems mounted at: /dev/hugepages
```



### 1.Compiling HelloWorld

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

比如要编译helloworld项目:

```
meson configure -Dexamples=helloworld
```

然后:

```
ninja
```

接着:(注意权限sudo)

```sh
sudo ./<build_dir>/examples/dpdk-helloworld -l 0-3
```

正确运行有:

```sh
❯ sudo ./examples/dpdk-helloworld -l 0-3
EAL: Detected CPU lcores: 32
EAL: Detected NUMA nodes: 1
EAL: Detected static linkage of DPDK
EAL: Multi-process socket /var/run/dpdk/rte/mp_socket
EAL: Selected IOVA mode 'VA'
hello from core 1
hello from core 2
hello from core 3
hello from core 0
```

代表我们成功了.

我们来分析一下这个程序:(第一个也是最简单的)

```c
#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <errno.h>
#include <sys/queue.h>

#include <rte_memory.h>
#include <rte_launch.h>
#include <rte_eal.h>
#include <rte_per_lcore.h>
#include <rte_lcore.h>
#include <rte_debug.h>

/* Launch a function on lcore. 8< */
static int
lcore_hello(__rte_unused void *arg)
{
	unsigned lcore_id;
	lcore_id = rte_lcore_id();
	printf("hello from core %u\n", lcore_id);
	return 0;
}
/* >8 End of launching function on lcore. */

/* Initialization of Environment Abstraction Layer (EAL). 8< */
int
main(int argc, char **argv)
{
	int ret;
	unsigned lcore_id;

	ret = rte_eal_init(argc, argv);
	if (ret < 0)
		rte_panic("Cannot init EAL\n");
	/* >8 End of initialization of Environment Abstraction Layer */

	/* Launches the function on each lcore. 8< */
	RTE_LCORE_FOREACH_WORKER(lcore_id) {
		/* Simpler equivalent. 8< */
		rte_eal_remote_launch(lcore_hello, NULL, lcore_id);
		/* >8 End of simpler equivalent. */
	}

	/* call it on main lcore too */
	lcore_hello(NULL);
	/* >8 End of launching the function on each lcore. */

	rte_eal_mp_wait_lcore();

	/* clean up the EAL */
	rte_eal_cleanup();

	return 0;
}

```

1.初始化EAL环境:

```C
int
main(int argc, char **argv)
{
	int ret;
	unsigned lcore_id;

	ret = rte_eal_init(argc, argv);
	if (ret < 0)
		rte_panic("Cannot init EAL\n");
```

这就是我们手动提供的两个参数,-l 0-3,制定CPU核心的编号.

当环境初始化了之后,我们要在每个CPU核心上运行下面这个方法:

```C
static int
lcore_hello(__rte_unused void *arg)
{
	unsigned lcore_id;
	lcore_id = rte_lcore_id();
	printf("hello from core %u\n", lcore_id);
	return 0;
}
```

下面是在每个核心上跑的**function**:

```C
RTE_LCORE_FOREACH_WORKER(lcore_id) {
	/* Simpler equivalent. 8< */
	rte_eal_remote_launch(lcore_hello, NULL, lcore_id);
	/* >8 End of simpler equivalent. */
}

/* call it on main lcore too */
lcore_hello(NULL);
```

或者是直接这样:

```C
rte_eal_remote_launch(lcore_hello, NULL, lcore_id);
```





### 2.Command Line Sample Application

> ​	命令行，这个程序就是展示交互API的使用方法。

There are three simple commands:

- add obj_name IP: Add a new object with an IP/IPv6 address associated to it.
- del obj_name: Delete the specified object.
- show obj_name: Show the IP associated with the specified object.

退出命令行 ，Ctrl + D

运行命令。

> ​	那么注意，要分配hugepage,并且用sudo分配和执行命令。

To run the application in a Linux environment, issue the following command:

-l是指定CPU核心的编号

-n指的是内存通道

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

基本用法就是:

```sh
example> add man 127.0.0.1
Object man added, ip=127.0.0.1
example> show
Bad arguments
example> show man
Object man, ip=127.0.0.1
example> del man
Object man removed, ip=127.0.0.1
example> show man
Bad arguments
```

### 3.Ethtool Sample Application

数据平面的开发包.这里我们要和一个网卡进行绑定.

先用tcpdump抓一个包：

```sh
sudo tcpdump -i wlan0 -c 200 -w test.pcap
```



接着再绑定到一个虚拟网卡上面来运行程序：

```sh
sudo ./build/dpdk_rx_mac -l 0 -n 4 --vdev 'net_pcap0,rx_pcap=test.pcap' -- -p 0x1
```

和我们上面的入门示例是一样的.

```sh
❯ sudo ./examples/dpdk-ethtool -l 0-3 -n 4 --vdev  'net_pcap0,rx_pcap=test.pcap' -- -p 0x1    
EAL: Detected CPU lcores: 32
EAL: Detected NUMA nodes: 1
EAL: Detected static linkage of DPDK
EAL: Multi-process socket /var/run/dpdk/rte/mp_socket
EAL: Selected IOVA mode 'VA'
Number of NICs: 1
Init port 0..
EthApp> drvinfo
firmware version get error: (Operation not supported)
Port 0 driver: net_pcap (ver: DPDK 25.07.0-rc3)
firmware-version: 
bus-info: net_pcap0s
```

这些功能都设置成了一个单独的库,之后可以用来调用.

### 4.Basic Forwarding Sample Application

> ​	这是最基本的应用,是后面的基础.	
>
> ​	转发的应用,数据平面处理的核心机制.这是在两个网卡之间循环发送数据包.
>
> ​	光是跑起来就要设置半天环境了.

​	首先我们要理解PMD:(这是一种机制)

​	**PMD** 是 **Poll Mode Driver（轮询模式驱动）** 的缩写，它是 DPDK 高性能数据包处理的核心组件之一.

- **轮询（Polling）**：PMD 通过主动轮询网卡的接收/发送队列（RX/TX Queue）获取数据包，**完全绕过内核中断机制**，避免上下文切换开销。
- **零拷贝（Zero-Copy）**：数据包直接从网卡 DMA 到用户态内存（`rte_mbuf`），无需内核参与。
- **批处理（Burst）**：每次轮询处理多个数据包（如 32/64 个），最大化 CPU 缓存利用率。

在 DPDK 中，PMD 分为两类：

**(1) 物理网卡 PMD**

- 针对特定硬件的驱动（如 `ixgbe`、`i40e`、`mlx5`）。
- 示例：Intel X710 网卡使用 `i40e`PMD，Mellanox ConnectX-6 使用 `mlx5`PMD。

**(2) 虚拟设备 PMD**

- 用于虚拟化或模拟场景：
  - **virtio-user**：用户态 virtio 网卡（如你的 `net_virtio_user0`）。
  - **vhost**：与 KVM 虚拟机通信的驱动。
  - **tap**：连接 Linux 内核 TAP 设备。

然后是PMD的**基本工作步骤**:

1. **初始化阶段**：
   - 加载 PMD 驱动（如 `rte_eth_ixgbe_pmd`）。
   - 配置网卡的 RX/TX 队列和内存池（`rte_mempool`）。
2. **数据包接收（RX）**：
   - PMD 轮询网卡接收队列，将数据包存入 `rte_mbuf`。
   - 调用 `rte_eth_rx_burst()`批量获取数据包。
3. **数据包发送（TX）**：
   - 应用填充 `rte_mbuf`后，调用 `rte_eth_tx_burst()`批量发送。
   - PMD 将数据包推送到网卡发送队列。

> ​	以下是关于网卡操作的记录.
>

我们使用usertool中的dpdk-pmdinfo.py,来观察关于网卡的信息.

```sh
usage: dpdk-pmdinfo.py [-h] [-p] [-v] ELF_FILE [ELF_FILE ...]

Utility to dump PMD_INFO_STRING support from DPDK binaries.

This script prints JSON output to be interpreted by other tools. Here are some
examples with jq:

Get the complete info for a given driver:

  dpdk-pmdinfo.py dpdk-testpmd | \
  jq '.[] | select(.name == "cnxk_nix_inl")'

Get only the required kernel modules for a given driver:

  dpdk-pmdinfo.py dpdk-testpmd | \
  jq '.[] | select(.name == "net_i40e").kmod'

Get only the required kernel modules for a given device:

  dpdk-pmdinfo.py dpdk-testpmd | \
  jq '.[] | select(.pci_ids[]? | .vendor == "15b3" and .device == "1013").kmod'

positional arguments:
  ELF_FILE              DPDK application binary or dynamic library.

options:
  -h, --help            show this help message and exit
  -p, --search-plugins  In addition of ELF_FILEs and their linked dynamic libraries, also scan the DPDK
                        plugins path.
  -v, --verbose         Display warnings due to linked libraries not found or ELF/JSON parsing errors in
                        these libraries. Use twice to show debug messages.

```

1.查看可用网卡PCI的地址:

```sh
❯ sudo python3 dpdk-devbind.py --status

Network devices using kernel driver
===================================
0000:00:14.3 'Raptor Lake-S PCH CNVi WiFi 7a70' if=wlan0 drv=iwlwifi unused=wl *Active*
0000:43:00.0 'RTL8111/8168/8211/8411 PCI Express Gigabit Ethernet Controller 8168' if=eno1 drv=r8169 unused= 

No 'Baseband' devices detected
==============================

No 'Crypto' devices detected
============================

No 'DMA' devices detected
=========================

No 'Eventdev' devices detected
==============================

No 'Mempool' devices detected
=============================

No 'Compress' devices detected
==============================

No 'Misc (rawdev)' devices detected
===================================

No 'Regex' devices detected
===========================

No 'ML' devices detected
========================
```

#### 内存问题

> ​	我们明确几个关于内存的概念.

产看内存分配的情况:

```sh
❯ grep Huge /proc/meminfo
AnonHugePages:    673792 kB
ShmemHugePages:        0 kB
FileHugePages:    352256 kB
HugePages_Total:    1024
HugePages_Free:     1024
HugePages_Rsvd:        0
HugePages_Surp:        0
Hugepagesize:       2048 kB
Hugetlb:         2097152 kB
```

要运行dpdk程序,我们要分配这样的大页内存.

NUMA内存 Non-Uniform Memory Access

我们查看NUMA节点的分布:

```sh
❯ numactl -H
available: 1 nodes (0)
node 0 cpus: 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
node 0 size: 15675 MB
node 0 free: 4637 MB
node distances:
node     0 
   0:   10 
```

我想创建两个网卡,但是遇到了很多不理解的问题.

首先我们创建一个后端网卡:

```sh
sudo ./build/app/dpdk-testpmd -l 0-1 \
    --vdev="net_vhost0,iface=/tmp/socket0" \
    --no-pci \
    -- --forward-mode=io
```

这个整了半天弄不明白,这个虚拟网卡的命令行太复杂了.

#### 网卡问题

首先我们理解什么是pcap文件:

> ​	完整的网络数据包文件.

​	**PCAP（Packet Capture）文件**是抓包工具（如 `tcpdump`、`Wireshark`）保存网络数据包的标准格式。里面包含了网络中传输的每一个完整数据包的内容（包括 MAC 层、IP 层、传输层等的头部信息和负载），用于后续分析网络行为、排查问题等。

- 文件扩展名：`.pcap`
- 常见工具：`tcpdump -w out.pcap`（抓取网络数据并保存为 pcap）
- 可以被 DPDK 用来“虚拟回放”流量



```sh
sudo tcpdump -i lo -c 1 -w /tmp/in0.pcap
sudo tcpdump -i lo -c 1 -w /tmp/in1.pcap
```

我们先创建具有正确格式的pcap包.

接着执行:

```sh
sudo ./examples/dpdk-skeleton -l 1 \
  --vdev=net_pcap0,rx_pcap=/tmp/in0.pcap,tx_pcap=/tmp/out0.pcap \
  --vdev=net_pcap1,rx_pcap=/tmp/in1.pcap,tx_pcap=/tmp/out1.pcap
```

--vdev=net_pcap0,...创建一个虚拟网卡,读取后面的pcap文件模拟行为

rx读取数据包模拟接收.

tx写入数据包模拟发送.

注意当我们需要虚拟网卡的时候这样做.

- `--vdev <device arguments>`

  Add a virtual device using the format:

  ```
  <driver><id>[,key=val, ...]
  ```

  For example:

  ```
  --vdev 'net_pcap0,rx_pcap=input.pcap,tx_pcap=output.pcap'
  ```

我精心注释了这个代码,来更好的理解内容.

#### code 代码理解

```C
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
* @function 初始化网卡端口port,比较有移植性
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
    // 根据我们给定的参数进行初始化
    int ret = rte_eal_init(argc, argv);
    if(ret < 0){
        rte_exit(EXIT_FAILURE, "Error with the EAL initialization!!!\n");
    }

    // rte_init会消耗掉前面的参数,这里要进行更新
    // ???
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
* NUM_MBUFS * nb_ports：总 mbuf 数量（根据端口数扩大）
* MBUF_CACHE_SIZE：每个 lcore 缓存多少 mbuf
* 0：私有数据空间大小（我们不需要）
* RTE_MBUF_DEFAULT_BUF_SIZE：默认数据包大小（2048）
* rte_socket_id()：NUMA 优化，分配内存在当前 socket 上
*/
    mbuf_pool = rte_pktmbuf_pool_create("MBUF_POOL", NUM_MBUFS * nb_ports, MBUF_CACHE_SIZE, 0, RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());

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
```

> ​	抄代码并且单独运行也是很好的方法,但是还有很多的细节要理解,要理解并且熟练.
>























