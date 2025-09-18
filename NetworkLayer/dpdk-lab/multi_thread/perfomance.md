# 对比

嗯，明白了，我帮你梳理一下思路。你现在的问题核心是：**如何用 DPDK 搭实验对比单线程 vs 多线程性能，并且在处理流程里加上“加解密”的模拟负载**。

> 学长,我又看了一些基础的概念和多进程,然后又仔细看了l2fwd转发和带加密转发,就是直接用这个加密队列来处理数据包.然后我还有问题,这些程序里面应该都是认为一个lcore就是一个线程,一般就在一个端口上面不停的遍历的处理rx tx队列,都算是多线程程序,那么单线程和多线程的对比该怎么做.有哪些代码可以针对性看的 
>
> 比如说，多个线程情况可以是一个线程负责包的收发，其他线程负责解密等相对重的工作；或者每个线程都参与收发和解密的情况。单线程当然只能自己同时完成收发和解密。你可以自己做实验对比一下这2种多线程情况相比单线程的加速比。 
>
> 明白了，就是单线程会让一个lcore处理一个数据包转发过程中的所有行为但是多线程是分工 我试试 
>
> 这两种情况的加速比你可以对比一下，然后调整解密工作在整体工作中的比重，推测一下加速比变化的原因 
>
> 好 解密工作的比重是调整在上面工作的线程的数量么？ 
>
> 不是，比如包内需要加解密字段的长度 好 你可以自己构造整个包，然后控制这个长度，这个比重也可以通过调整包其他部分的解析来调整 

1.构建可控制的数据包.(L2层的数据包,工具链)

2.单线程的流程/多线程的流程怎么构建

3.计时怎么实现 基准怎么选取

4.怎么加入加解密的流程



### 1. 你要关注的 DPDK 部分

别急着把整个 DPDK 框架啃下来，针对你要做的实验，可以先聚焦几个点：

- **EAL 初始化**：`rte_eal_init()`，负责把 lcore 和内存池搞起来。
- **mbuf 机制**：`struct rte_mbuf`，就是 DPDK 的数据包载体，你加“加解密负载”就是在处理这个结构里的数据区。
- **收发 API**：
  - `rte_eth_rx_burst()` —— 从端口的 RX 队列取包
  - `rte_eth_tx_burst()` —— 把包送到端口的 TX 队列
- **多线程调度**：DPDK 的“线程”就是 `lcore`，一般用 `rte_eal_remote_launch(func, arg, lcore_id)` 把一个函数绑定到某个 lcore 上。

👉 如果你只盯着 l2fwd 示例代码，这几个地方一看就懂。

------

### 2. 单线程 vs 多线程的实验设计

博士给你提示了两种情况，其实就是 **并行模型**的对比：

1. **单线程模型**
    一个 lcore：取包 → 加解密（模拟）→ 发包。
   - 对应的代码逻辑就是在 `rte_eth_rx_burst()` 和 `rte_eth_tx_burst()` 之间插一段 CPU 密集操作。
2. **多线程流水线模型**
   - lcore0：专门收包，放到一个队列
   - lcore1：从队列取包，加解密 → 放回另一个队列
   - lcore2：负责发包
   - 用 `rte_ring` 在不同线程之间传递 `mbuf`。
3. **多线程并行模型**
   - 每个 lcore 各自执行完整流程（收包→加解密→发包）。
   - 类似 l2fwd 原版代码就是这种。

------

### 3. 如何模拟“加解密开销”

你暂时不需要真正写 AES 或 IPsec，实验的关键是 CPU 占用。做法有两个：

- **简单粗暴**：在包处理的地方写个循环（比如遍历 `mbuf->data_len` 里的每个字节 XOR 一下）。
- **可调负载**：让你可以通过设置“包里面需要处理的长度”来增加或减少开销。比如只处理前 64B、128B、512B，看性能怎么变。

------

### 4. 推荐的代码路径

- **最小实验**：从 `examples/l2fwd` 出发，把 `main_loop` 改造：
  - 在 `rte_eth_rx_burst()` 取包后，加个 `dummy_encrypt(mbuf)`。
  - 再 `rte_eth_tx_burst()` 发出去。
- **多线程流水线**：参考 `examples/multi_process/simple_mp` 或者 `examples/packet_ordering` 里怎么用 `rte_ring` 把包丢给其他 lcore。

------

### 5. 实验步骤

1. 跑通 **单线程版本**：在 l2fwd 基础上加 dummy 加解密。
2. 实现 **并行多线程版本**：多个 lcore 各自做完整流程。
3. 实现 **流水线版本**：用 ring 把收/加密/发分给不同 lcore。
4. 用 pktgen 或 testpmd 发送流量，统计 throughput 和 latency。
5. 调整 dummy 加解密的“处理长度”，画图看加速比随开销变化的趋势。

------

👉 我建议你下一步不要急着搞 tap/veth 这些细节，而是先挑一份最小的代码框架（比如 l2fwd），写一个 **dummy 加解密函数**进去，确认能跑通单线程实验。

要不要我帮你在 l2fwd 的 `main_loop` 里画一下 **在哪里插 dummy_encrypt**？



## 虚拟网卡

先创建虚拟网卡:

```sh
# 创建一对 veth设备，命名为 veth0 和 veth1
sudo ip link add veth0 type veth peer name veth1

# 配置IP地址（可选，用于辅助测试）
sudo ip addr add 192.168.1.1/24 dev veth0
sudo ip addr add 192.168.1.2/24 dev veth1

# 启动设备
sudo ip link set veth0 up
sudo ip link set veth1 up

# 查看设备状态及MAC地址
ip link show veth0
ip link show veth1
```

熟悉可用的参数:

```sh
./<build_dir>/examples/dpdk-l2fwd [EAL options] -- -p PORTMASK
                               [-P]
                               [-q NQ]
                               --[no-]mac-updating
                               [--portmap="(port, port)[,(port, port)]"]
```

选取一些参数运行程序:

```sh
sudo ./build/examples/dpdk-l2fwd -l 0-1 -n 4 --vdev=net_pcap0,iface=veth0 --vdev=net_pcap1,iface=veth1 -- -p 0x3 --no-mac-updating
```

## 怎么控制frame

### 使用scapy工具

简单测试:

```py
from scapy.all import Ether, sendp
# 发送一个以太网帧
pkt = Ether(dst="da:c9:6a:67:8a:d6", src="02:00:00:00:00:01")/b'HELLO-DPDK'
sendp(pkt, iface="veth0", count=1, verbose=1)

```

先更改,停止无限转发.--->怎么该逻辑,使用标志位?

在私有字段上做手脚,创建一个私有字段,大小是8bytes:

```C
l2fwd_pktmbuf_pool = rte_pktmbuf_pool_create("mbuf_pool", nb_mbufs,
		MEMPOOL_CACHE_SIZE, sizeof(uint64_t), RTE_MBUF_DEFAULT_BUF_SIZE,
		rte_socket_id());
```

然后在收包这里的逻辑处理:

```C
for (j = 0; j < nb_rx; j++) {
				m = pkts_burst[j];

				uint64_t* mark = rte_mbuf_to_priv(m);
				if(*mark == 1){
					rte_pktmbuf_free(m);
					continue;
				}

				*mark = 1;
				// prefetch--->预取数据到CPU缓存
				rte_prefetch0(rte_pktmbuf_mtod(m, void *));
				// 模拟对于这个数据包进行加密的处理
				dummy_encrypt(m, 256);
				// 核心转发的逻辑,把接收到的数据包进行转发
				l2fwd_simple_forward(m, portid);
			}
```

还是有一些问题,感觉数字对不上号,但是问题不大.

大量发送并且控制frame的长度:

```py
#!/usr/bin/env python3
from scapy.all import Ether, sendp
import sys
import time
import random

iface = sys.argv[1] if len(sys.argv)>1 else "veth0"
dst = sys.argv[2] if len(sys.argv)>2 else "02:00:00:00:00:02"  # 目标 MAC
count = int(sys.argv[3]) if len(sys.argv)>3 else 1000
payload_len = int(sys.argv[4]) if len(sys.argv)>4 else 128
interval = float(sys.argv[5]) if len(sys.argv)>5 else 0.0  # 秒，0 表示尽可能快

for i in range(count):
    payload = bytes([random.getrandbits(8) for _ in range(payload_len)])
    pkt = Ether(dst=dst)/payload
    sendp(pkt, iface=iface, verbose=False)
    if interval>0:
        time.sleep(interval)
print("done")
```

直接发送:

```py
sudo python3 send_eth.py veth0 02:00:00:00:00:02 10000 256 0.0
# 在 veth0 发送 1000 个以太帧，每个 256B，尽可能快
```

高速测试:

```py
from scapy.all import sendpfast, Ether
pkt = Ether(dst="02:00:00:00:00:02")/b'A'*128
sendpfast([pkt]*1000, iface="veth0", pps=10000)   # 每秒 10000 pkt
```

尝试在另一边监测收到的frame:

```sh
sudo tcpdump -i veth1 -e -n -xx       # -e 显示以太头，-xx 显示原始内容
```

以字符串的形式显示:

```sh
sudo tcpdump -i veth1 -A
```

比如我们之前发送了一个frame,tcpdump veth1的接口查看:

```sh
sudo tcpdump -i veth1 -A
tcpdump: verbose output suppressed, use -v[v]... for full protocol decode
listening on veth1, link-type EN10MB (Ethernet), snapshot length 262144 bytes
19:53:45.804127 Loopback, skipCount 17736 (invalid)
HELLO-DPDK
```

到这里还没有加密等等操作,我们才解决了发包控制的问题,可以控制长度和数量并且不会死循环.

### testpmd做测试

先运行这个命令行程序:

```sh
sudo ./build/app/dpdk-testpmd -l 0-3 -n 4 -- -i \
  --forward-mode=macswap \
  --port-topology=chained \
  --burst=32
```









## 单线程处理

我们先不关心具体的加解密的逻辑,先考虑实现一个lcore处理两个端口上的收发和加解密,先用位运算模拟一下.

先调整一下参数,让一个lcore可以处理多个port上的收发问题,并且进行加密和解密.

```sh
sudo ./build/multi_thread/l2fwd/l2fwd -l 0 -n 4 --vdev=net_pcap0,iface=veth0 --vdev=net_pcap1,iface=veth1 -- -p 0x3 -q 2
```

只要再加上真实的加密逻辑,我们就可以认为这样的一个线程处理了两个端口上的收包,发包和加密,解密的过程.

两个lcore处理两个端口:

```sh
sudo ./build/multi_thread/l2fwd/l2fwd -l 0-1 -n 4 --vdev=net_pcap0,iface=veth0 --vdev=net_pcap1,iface=veth1 -- -p 0x3
```

> A.时间的计算是否合理,为什么这里用一个线程比两个线程有时候更快?
>
> 1.采取更多数量的包实验.
>
> 2.dpdk库里面有没有更好的工具?
>
> B.好像只给第一个包进行了模拟加密,原因?
>
> 我用数据包私有内存来判断,它直接被free,但是数据残留,所以,这两个问题是一起解决的.都进行"加解密"
>
> 暂时利用发送后更改etherType,收到后验证etherType解决了这个问题,OK
>
> 具体加解密流程复杂,怎么实现?
>
> C.单线程实现之后,怎么分离线程工作的逻辑,单独拿一部分来收发,另一部分加解密工作,命令行参数是怎样的?
>
> 

现在还没有放入真实的加密逻辑,先对比一下一个线程处理两个端口和两个线程分别处理两个端口的情况.

1000个包作为基准,双lcore:

```sh
Forwarded 1000 frames in 19.51784 seconds
The speed is 51.23518 frame/s... 
```

一个lcore:

```sh
Forwarded 1000 frames in 18.30551 seconds
The speed is 54.62837 frame/s... 
```

并没有明显的性能差异.







很好，你做了改动并跑起来了 — 现在能稳定看到无限“回环”现象。不要慌，我把原因、原理和两种可靠的修复方案（含可直接替换的代码片段）都说清楚并给出调试步骤。你可以直接把代码粘进来试验。

------

## 为什么还会出现无限回环（本质原因）

你做的目标是「把已经转发过的包识别并丢弃」，以避免在两口之间无限转发。之前我们讨论过两种标记方式：

- 标记 **mbuf 的 private 区或 mbuf 字段**（你之前尝试的）：**不可行** 来检测线缆/虚拟网卡上的回环包。原因：**发出去的包和后来被 rx 的包是不同的 mbuf 实例**（驱动/PMD 会把 wire 上来的数据填进新分配的 mbuf）。也就是说给 tx 时的 mbuf 打标记，并不能在接收时看到（除非你能在物理包里也写入标记）。
- 标记 **包的内容（以太头）**：这才是可靠的方式，因为包内容随帧在链路上传输，接收端能看到。
   我上次给的思路是“比较 `eth->src` 与端口MAC”，理论上可行 **但在你实际运行的环境里并没有生效**，原因可能包括：
  - `l2fwd_mac_updating()` 在发送时把 `eth->src` 改成某个值，但那个值在链路上被对端或网桥/driver 改写（例如 af_packet/tap/veth 在某些配置/驱动下会干预 MAC），导致接收端看到的 `eth->src` 并不是你写入的值 → 判断不命中 → 包被再次转发 → 循环。
  - 也可能是比较的逻辑/字节序有误（但这比较少见）。

结论：**标记必须用一种在链路上稳定保留的修改**。常见实验方法有：

- 改 EtherType（最简单可靠，适合实验）
- 添加/修改 VLAN 标签（需要更复杂的处理）
- 在 L2 payload 写自定义标识（但会修改协议负载）

对于做实验和避免回环，**改 EtherType 是最简单快速的办法**。

------

## 解决方案（给出两种可选实现，A 为推荐）

### 方案 A（推荐、快速）：把转发时的以太类型改成特殊值 `0x88B5`（或你喜欢的未被用的 EtherType）；接收端看到该 EtherType 就丢弃

优点：非常可靠，因为 EtherType 是链路层字段，几乎不会被驱动自动改写；实现改动少且容易观察。

**修改要点**

1. 在 `l2fwd_simple_forward()` 里，**在调用 `l2fwd_mac_updating()` 之后**，把 `eth->ether_type = rte_cpu_to_be_16(0x88B5)`（注意网络字节序）。
2. 在接收循环中，马上读取以太头 `eth`，如果 `ntohs(eth->ether_type) == 0x88B5`（或用 `rte_be_to_cpu_16`）就丢弃（`rte_pktmbuf_free(m); continue;`）。
3. （可选）如果你希望转发出去的包被接收端看到原来的 EtherType（还原），那需要在原始包里保存原始 type 并在适当时机恢复 —— 但你只想“只转发一次”，不需要恢复。

**接收循环伪代码（替换原来的检测/处理段）**:

```c
#include <rte_ether.h>
#include <rte_byteorder.h>

#define MARK_ETHER_TYPE 0x88B5

for (j = 0; j < nb_rx; j++) {
    m = pkts_burst[j];
    struct rte_ether_hdr *eth = rte_pktmbuf_mtod(m, struct rte_ether_hdr *);

    /* 如果已经被标记为已转发（EtherType==MARK），则丢弃 */
    if (rte_be_to_cpu_16(eth->ether_type) == MARK_ETHER_TYPE) {
        rte_pktmbuf_free(m);
        continue;
    }

    /* 第一次记录时间等逻辑（保持你原有逻辑，但用独立变量名） */
    ...
    rte_prefetch0(rte_pktmbuf_mtod(m, void *));
    dummy_encrypt(m, 256);

    /* 转发前在以太头设置标记类型 */
    /* l2fwd_simple_forward() 里会调用 l2fwd_mac_updating(m, dst)；那之后我们覆盖 ether_type */
    l2fwd_simple_forward(m, portid);

    /* 注意：l2fwd_simple_forward 已经把包交给 tx_buffer，且 tx_buffer 不会马上 free m（m 被传递）。
       如果 l2fwd_simple_forward 修改 ether_type，那个修改要在放入 tx_buffer 之前完成。
       因此更稳妥做法是在 l2fwd_simple_forward 内部做 ether_type 的写入（参见下面）。 */
}
```

**在 `l2fwd_simple_forward()` 内加入写 ether_type：**

```c
static void l2fwd_simple_forward(struct rte_mbuf *m, unsigned portid)
{
    unsigned dst_port;
    int sent;
    struct rte_eth_dev_tx_buffer *buffer;
    struct rte_ether_hdr *eth = rte_pktmbuf_mtod(m, struct rte_ether_hdr *);

    dst_port = l2fwd_dst_ports[portid];

    if (mac_updating)
        l2fwd_mac_updating(m, dst_port);

    /* 在这里写入标记 EtherType（网络字节序） */
    eth->ether_type = rte_cpu_to_be_16(MARK_ETHER_TYPE);

    buffer = tx_buffer[dst_port];
    sent = rte_eth_tx_buffer(dst_port, 0, buffer, m);
    if (sent)
        port_statistics[dst_port].tx += sent;
}
```

**如何用 tcpdump / scapy 验证**

- 启动程序后，用 tcpdump 在 veth 接口查看 EtherType:

  ```bash
  sudo tcpdump -i veth1 -e -n -xx ether proto 0x88b5
  ```

- 用 scapy 发送普通 IPv4 以太帧（Ether/ IP payload），观察转发出的帧是否出现 EtherType 0x88b5（被标记），同时被接收端不会再次转发（即可停止回环）。

------

### 方案 B（更“协议友好”）：使用 VLAN 标记或添加额外 L2 标识并恢复

如果你必须保持 EtherType（例如不能破坏 IP 数据），可以插入 802.1Q VLAN tag（在转发前插入一个 VLAN tag，接收时检测并丢弃），但实现会更复杂（要移动 payload、更新报头长度、且不同 PMD 行为不同）。我不建议用于快速实验。

------

## 诊断步骤（如果你想理解为什么原来 `eth->src` 方法失败）

在接收处打印收到帧的 MAC/ethertype，看看真正的链路上字段是什么。临时代码（谨慎打印小量）：

```c
struct rte_ether_addr src = eth->src_addr;
struct rte_ether_addr dst = eth->dst_addr;
printf("RX port %u src=%02x:%02x:%02x:%02x:%02x:%02x dst=%02x:%02x:%02x:%02x:%02x:%02x type=0x%04x\n",
       portid,
       src.addr_bytes[0], src.addr_bytes[1], src.addr_bytes[2],
       src.addr_bytes[3], src.addr_bytes[4], src.addr_bytes[5],
       dst.addr_bytes[0], dst.addr_bytes[1], dst.addr_bytes[2],
       dst.addr_bytes[3], dst.addr_bytes[4], dst.addr_bytes[5],
       rte_be_to_cpu_16(eth->ether_type));
```

（只打印很少包以免影响性能）
 如果你看到 `src` 根本不是你设置的值，那么说明链路/driver 修改了 MAC，证明 `eth->src` 检测不可依赖。

------

## 为什么之前你看到的 `Encrypted successfully!!!` 还在不断出现

那条输出来自 `dummy_encrypt(m, ... )`。无限 loop 的现象说明每次收到的包都被当成“未转发”并继续被处理、并再次转发，然后再次被接收并再次处理 —— 标记逻辑没有生效（标记没被链路保留或比较不命中）。所以 `dummy_encrypt()` 不停被调用。

把标记换成 EtherType 后，这个循环会被切断（收到标记包就丢弃），`dummy_encrypt()` 的调用次数也会变得正常（每个入站包一次）。

------

## 给你直接可用的补丁（最小改动版）

把下面两段分别替换到你的文件中（**确保包含合适的 include：** `#include <rte_byteorder.h>` 和 `#define MARK_ETHER_TYPE 0x88B5`）：

1. 在文件顶部（includes 后）加入：

```c
#include <rte_byteorder.h>
#define MARK_ETHER_TYPE 0x88B5
```

1. 在 `l2fwd_simple_forward()` 内，把 `l2fwd_mac_updating` 调用后的 ether_type 覆盖（完整函数示例）：

```c
static void
l2fwd_simple_forward(struct rte_mbuf *m, unsigned portid)
{
    unsigned dst_port;
    int sent;
    struct rte_eth_dev_tx_buffer *buffer;
    struct rte_ether_hdr *eth = rte_pktmbuf_mtod(m, struct rte_ether_hdr *);

    dst_port = l2fwd_dst_ports[portid];

    if (mac_updating)
        l2fwd_mac_updating(m, dst_port);

    /* 标记这个帧，表示已经被转发一次（使用一个不会被内核改变的 EtherType） */
    eth->ether_type = rte_cpu_to_be_16(MARK_ETHER_TYPE);

    buffer = tx_buffer[dst_port];
    sent = rte_eth_tx_buffer(dst_port, 0, buffer, m);
    if (sent)
        port_statistics[dst_port].tx += sent;
}
```

1. 在接收循环（`for (j = 0; j < nb_rx; j++)`）开头加入检测并丢弃标记包：

```c
m = pkts_burst[j];
struct rte_ether_hdr *eth = rte_pktmbuf_mtod(m, struct rte_ether_hdr *);

/* 如果已标记为我们转发过（EtherType==MARK），则丢弃 */
if (rte_be_to_cpu_16(eth->ether_type) == MARK_ETHER_TYPE) {
    rte_pktmbuf_free(m);
    continue;
}

/* 其余处理保持不变 */
```

（同时请根据之前建议修正 `end_cycles` / `finish_flag` 的混用问题）

------

## 如何快速验证（推荐顺序）

1. 编译并运行 l2fwd（带你的两个 veth）：

   ```bash
   sudo ./multi_thread/l2fwd/l2fwd -l 0-1 -n 4 \
       --vdev=net_af_packet0,iface=veth0 \
       --vdev=net_af_packet1,iface=veth1 \
       -- -p 0x3
   ```

2. 先在一侧抓包看标记（tcpdump 只看该 ether type）：

   ```bash
   sudo tcpdump -i veth1 -e -n -xx ether proto 0x88b5
   ```

3. 用 scapy 发一个普通 Ethernet/IP 包（不带 0x88b5）：

   ```python
   from scapy.all import Ether, IP, UDP, sendp
   pkt = Ether(dst="DA:C9:6A:67:8A:D6", src="02:00:00:00:00:01")/IP(dst="1.2.3.4")/UDP(dport=1234)/b"PAY"
   sendp(pkt, iface="veth0", count=10)
   ```

4. 观察：

   - 第一次发出的包到达对端并被 l2fwd 处理并转发（此时 l2fwd 会把 ether_type 改成 0x88b5，然后发出）
   - tcpdump 应该能看到以太类型 0x88b5 的帧出现在接口上（说明标记写进去了）
   - 接收端收到 0x88b5 的包会丢弃（不会再次转发），因此无限回环应当停止
   - `dummy_encrypt()` 的输出应与发包速率一致（而不应不断打印无限多次）

------

## 额外建议 / 后续改进

- 如果你需要真实业务（不能破坏 EtherType），可以把标记写入 L2 payload 的某些可重用的位置，或使用 VLAN tag（实现更复杂，但更“协议友好”）。
- 用 `tcpdump -e -n -xx` 在 veth 看原始 bytes，确认哪个字段被改变（有助于诊断驱动是否改写 MAC）。
- 为了做吞吐测量，把 `printf` 都删掉或改成每 N 个包汇总一次，避免 I/O 干扰测量。
- 如果你愿意，我可以把完整的补丁文件（包含 `finish_flag` 修复）发给你，你直接替换文件并重编译试跑。

------

想先把我上面 A 方案代码直接贴给你（完整的、可直接替换的片段）并修好 `end_flag`/计时变量吗？我可以把完整函数体给你，你直接替换。不过你也可以先把上面三处小改动试一遍，然后把运行日志（特别是 tcpdump 输出里看到的 ether type / mac）贴上来，我帮你进一步确认。



