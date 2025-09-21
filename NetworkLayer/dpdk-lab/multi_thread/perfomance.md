# 对比

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
sudo ./l2fwd -l 0-1 -n 4 --vdev=net_pcap0,iface=veth0 --vdev=net_pcap1,iface=veth1 -- -p 0x3 --no-mac-updating
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

并没有明显的性能差异.(这个时候实现有问题,而且速度太慢)

## 加解密的问题

接下来我们要搞清楚的是,单线程加解密,或者加解密本身是怎样的流程.

先使用简单的加解密的逻辑,看看有没有变慢:

接下来我们会拿5000个包做测试.

之前的payload是128字节,我们改成1024字节尝试一下:

> py构造数据包是否有速度的限制?用tcpreplay来放包.--->是这个问题,感觉之前的对比没有什么意义了.

那么现在我们引入流水线来处理,收发包的逻辑和加解密的逻辑分开.

## 多线程流水线处理

那么我们之前的实现的代码就是单线程处理的.

> 怎么把I/O和加解密解耦?

要使用rte_ring来解决.

## 结果

我们进行的简单对比:

### 情况

A.一个程序仅使用一个lcore,处理两个端口的全部收发和加解密.

B.另一个使用两个lcore,使用rte_ring,其中一个lcore负责处理两个端口上面的收发以及信息的打印,一个lcore仅负责处理ring队列中的加解密.

C.不解耦,2个lcore分别处理两个端口,参与收发和加解密.

### 测试

用Pktgen-DPDK来做产生流量进行测试.

先调整两个网卡的mtu大小:

```sh
sudo ip link set veth0 mtu 9000                                                     
sudo ip link set veth1 mtu 9000
```

在程序中从接收到第一个数据包开始计时,到100000之后结束,并且计算.

#### 128bytes:

A:

```sh
Forwarded 100000 frames in 33.94502 seconds
The speed is 2945.94048 frame/s... 
```

B:

```sh
Forwarded 100000 frames in 26.60046 seconds
The speed is 3759.33415 frame/s... 
```

C:

```sh
Forwarded 100000 frames in 21.54361 seconds
The speed is 4641.74657 frame/s... 
```

B/A = 1.276

C/A = 1.575

加速比:0.810

#### 256bytes

A:

```sh
Forwarded 100000 frames in 26.92276 seconds
The speed is 3714.32934 frame/s... 
```

B:

```sh
Forwarded 100000 frames in 25.43705 seconds
The speed is 3931.27402 frame/s... 
```

C:

```sh
Forwarded 100000 frames in 27.93653 seconds
The speed is 3579.54210 frame/s... 
```

B/A = 1.058

C/A = 0.963

加速比:1.098

#### 512bytes

A:

```sh
Forwarded 100000 frames in 45.01419 seconds
The speed is 2221.52155 frame/s... 
```

B:

```sh
Forwarded 100000 frames in 35.54370 seconds
The speed is 2813.43835 frame/s... 
```

C:

```sh
Forwarded 100000 frames in 37.24130 seconds
The speed is 2685.19067 frame/s... 
```

B/A = 1.266

C/A = 1.208

加速比:1.055

#### 1024bytes

A:

```sh
Forwarded 100000 frames in 83.54150 seconds
The speed is 1197.00993 frame/s... 
```

B:

```sh
Forwarded 100000 frames in 57.05477 seconds
The speed is 1752.70196 frame/s... 
```

C:

```sh
Forwarded 100000 frames in 67.54421 seconds
The speed is 1480.51172 frame/s... 
```

B/A = 1.463

C/A = 1.236

加速比:1.18

#### 2048bytes

A:

```sh
Forwarded 100000 frames in 104.46021 seconds
The speed is 957.30229 frame/s... 
```

B:

```sh

```

C:

```sh
Forwarded 100000 frames in 104.95009 seconds
The speed is 952.83385 frame/s... 
```

B/A = 1.058

C/A = 0.963

加速比:1.098







