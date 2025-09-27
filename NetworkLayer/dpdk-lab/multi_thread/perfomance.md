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

简单测试连通性,和是否被加密了.

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

加入简单的加密逻辑,

先运行这个命令行程序:

```sh
sudo ./build/app/dpdk-testpmd -l 0-3 -n 4 -- -i \
  --forward-mode=macswap \
  --port-topology=chained \
  --burst=32
```

## 单线程处理

我们先不关心具体的加解密的逻辑,先考虑实现一个lcore处理两个端口上的收发和加解密,先用位运算模拟一下.

先调整一下参数,让一个**lcore**可以处理多个**port**上的收发问题,并且进行加密和解密.

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

用**dpdk-testpmd**来做产生流量进行测试.我们对于一个网口进行流量发送,然后查看效果.

```sh
sudo dpdk-testpmd -l 2-3 -n 4 \
      --vdev=net_pcap0,iface=veth0 \
      --file-prefix=testpmd \
      --proc-type=auto \
      -- --forward-mode=txonly \
      --txd=1024 --rxd=1024 \
      --port-topology=loop \
      --nb-ports=1 \
      -i
```

然后进行单向发送的配置:

```sh
  testpmd> stop
  testpmd> port stop all
  testpmd> port config all txq 1
  testpmd> port config all rxq 1
  testpmd> set fwd txonly
  testpmd> set txpkts 64
  testpmd> port start all
  testpmd> show config fwd    # 确认配置
  testpmd> start tx_first
```

这样的配置应该没有问题:

```sh
txonly packet forwarding - ports=1 - cores=1 - streams=1 - NUMA support enabled, MP allocation mode: native
Logical Core 3 (socket 0) forwards packets on 1 streams:
  RX P=0/Q=0 (socket 0) -> TX P=0/Q=0 (socket 0) peer=02:00:00:00:00:00
```

以5000000个数据包做基准,虽然我不知道会不会有性能的限制.

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

简单测试连通性,和是否被加密了.

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

加入简单的加密逻辑,

先运行这个命令行程序:

```sh
 sudo ./build/app/dpdk-testpmd -l 0-3 -n 4 -- -i \
   --forward-mode=macswap \
   --port-topology=chained \
   --burst=32
```

## 单线程处理

我们先不关心具体的加解密的逻辑,先考虑实现一个lcore处理两个端口上的收发和加解密,先用位运算模拟一下.

先调整一下参数,让一个**lcore**可以处理多个**port**上的收发问题,并且进行加密和解密.

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

用**dpdk-testpmd**来做产生流量进行测试.我们对于一个网口进行流量发送,然后查看效果.

```sh
 sudo dpdk-testpmd -l 2-3 -n 4 \
       --vdev=net_pcap0,iface=veth0 \
       --file-prefix=testpmd \
       --proc-type=auto \
       -- --forward-mode=txonly \
       --txd=1024 --rxd=1024 \
       --port-topology=loop \
       --nb-ports=1 \
       -i
```

然后进行单向发送的配置:

```sh
   testpmd> stop
   testpmd> port stop all
   testpmd> port config all txq 1
   testpmd> port config all rxq 1
   testpmd> set fwd txonly
   testpmd> set txpkts 64
   testpmd> port start all
   testpmd> show config fwd    # 确认配置
   testpmd> start tx_first
```

这样的配置应该没有问题:

```sh
 txonly packet forwarding - ports=1 - cores=1 - streams=1 - NUMA support enabled, MP allocation mode: native
 Logical Core 3 (socket 0) forwards packets on 1 streams:
   RX P=0/Q=0 (socket 0) -> TX P=0/Q=0 (socket 0) peer=02:00:00:00:00:00
```

以5000000个数据包做基准,虽然我不知道会不会有性能的限制.

#### 64bytes:

A:

```sh
Forwarded 5000000 frames in 11.95635 seconds
The speed is 418187.79948 frame/s... 
Forwarded 5000000 frames in 12.13336 seconds
The speed is 412086.97799 frame/s... 
Forwarded 5000000 frames in 12.22878 seconds
The speed is 408871.57481 frame/s... 
Forwarded 5000000 frames in 12.84697 seconds
The speed is 389196.90463 frame/s... 
```

B:

```sh
Forwarded 5000000 frames in 9.17793 seconds
The speed is 544785.29259 frame/s... 
Forwarded 5000000 frames in 9.46229 seconds
The speed is 528413.04580 frame/s... 
Forwarded 5000000 frames in 9.73131 seconds
The speed is 513805.29044 frame/s... 
Forwarded 5000000 frames in 9.76250 seconds
The speed is 512164.07599 frame/s... 
Forwarded 5000000 frames in 9.62936 seconds
The speed is 519245.39231 frame/s... 
```

C:

```sh
Forwarded 5000000 frames in 10.22622 seconds
The speed is 488939.43963 frame/s... 
Forwarded 5000000 frames in 10.09045 seconds
The speed is 495517.88604 frame/s... 
Forwarded 5000000 frames in 10.02363 seconds
The speed is 498821.21987 frame/s... 
Forwarded 5000000 frames in 10.28604 seconds
The speed is 486095.54340 frame/s... 
```

B/A = 1.29

C/A = 1.21

**加速比:** 1.066

#### 128bytes:

A:

```sh
Forwarded 5000000 frames in 12.38187 seconds
The speed is 403816.27546 frame/s... 
Forwarded 5000000 frames in 12.56814 seconds
The speed is 397831.19390 frame/s... 
Forwarded 5000000 frames in 12.21329 seconds
The speed is 409390.08236 frame/s... 
```

B:

```sh
Forwarded 5000000 frames in 9.89604 seconds
The speed is 505252.50110 frame/s... 
Forwarded 5000000 frames in 9.83576 seconds
The speed is 508349.21622 frame/s... 
Forwarded 5000000 frames in 9.92876 seconds
The speed is 503587.47182 frame/s... 
```

C:

```sh
Forwarded 5000000 frames in 10.39675 seconds
The speed is 480919.31557 frame/s... 
Forwarded 5000000 frames in 10.38278 seconds
The speed is 481566.49558 frame/s... 
Forwarded 5000000 frames in 10.20946 seconds
The speed is 489741.63841 frame/s... 
```

B/A = 1.25

C/A = 1.20

**加速比:1.041**

#### 256bytes

A:

```sh
Forwarded 5000000 frames in 13.21581 seconds
The speed is 378334.88000 frame/s... 
Forwarded 5000000 frames in 13.28452 seconds
The speed is 376377.89512 frame/s... 
Forwarded 5000000 frames in 13.24504 seconds
The speed is 377499.72792 frame/s... 
```

B:

```sh
Forwarded 5000000 frames in 10.36578 seconds
The speed is 482356.26903 frame/s... 
Forwarded 5000000 frames in 10.56124 seconds
The speed is 473429.27159 frame/s... 
Forwarded 5000000 frames in 10.64875 seconds
The speed is 469538.73774 frame/s... 
Forwarded 5000000 frames in 10.60009 seconds
The speed is 471694.03006 frame/s... 
```

C:

```sh
Forwarded 5000000 frames in 10.67588 seconds
The speed is 468345.42051 frame/s... 
Forwarded 5000000 frames in 10.72200 seconds
The speed is 466330.83614 frame/s... 
Forwarded 5000000 frames in 10.82696 seconds
The speed is 461810.19716 frame/s... 
```

B/A = 1.26

C/A = 1.23

**加速比:1.024**

#### 512bytes

A:

```sh
Forwarded 5000000 frames in 15.33425 seconds
The speed is 326067.56745 frame/s... 
Forwarded 5000000 frames in 14.96587 seconds
The speed is 334093.55690 frame/s... 
Forwarded 5000000 frames in 15.53538 seconds
The speed is 321845.95492 frame/s... 
```

B:

```sh
Forwarded 5000000 frames in 11.94801 seconds
The speed is 418479.76014 frame/s... 
Forwarded 5000000 frames in 11.96507 seconds
The speed is 417883.05495 frame/s... 
Forwarded 5000000 frames in 11.78509 seconds
The speed is 424265.04845 frame/s... 
```

C:

```sh
Forwarded 5000000 frames in 11.91589 seconds
The speed is 419607.71766 frame/s... 
Forwarded 5000000 frames in 11.60845 seconds
The speed is 430720.70034 frame/s... 
Forwarded 5000000 frames in 11.63096 seconds
The speed is 429887.13172 frame/s... 
```

B/A = 1.28

C/A = 1.30

**加速比:0.984**

#### 1024bytes

A:

```sh
Forwarded 5000000 frames in 18.11797 seconds
The speed is 275969.12640 frame/s... 
Forwarded 5000000 frames in 18.32365 seconds
The speed is 272871.35994 frame/s... 
Forwarded 5000000 frames in 18.39165 seconds
The speed is 271862.46932 frame/s... 
```

B:

```sh
Forwarded 5000000 frames in 14.82811 seconds
The speed is 337197.31746 frame/s... 
Forwarded 5000000 frames in 14.77073 seconds
The speed is 338507.20084 frame/s... 
Forwarded 5000000 frames in 14.63262 seconds
The speed is 341702.19098 frame/s... 
Forwarded 5000000 frames in 14.62498 seconds
The speed is 341880.82364 frame/s... 
```

C:

```sh
Forwarded 5000000 frames in 13.28649 seconds
The speed is 376322.06287 frame/s... 
Forwarded 5000000 frames in 13.32213 seconds
The speed is 375315.41661 frame/s... 
Forwarded 5000000 frames in 13.61788 seconds
The speed is 367164.35028 frame/s... 
```

B/A = 1.24

C/A = 1.36

**加速比:0.9117**

|   size    |  rate  |
| :-------: | :----: |
|  64bytes  | 1.066  |
| 128bytes  | 1.041  |
| 256bytes  | 1.024  |
| 512bytes  | 0.984  |
| 1024bytes | 0.9117 |

### 猜测

因为之前并没有做过性能测试相关的工作,所以肯定有很多不严谨和不对的地方,仅从实验结果的数据来看,加速比是随着frame大小的上升而降低的.

原因是什么?

猜测:I/O和加解密的工作肯定都会变多,但是I/O带来的开销更大,导致I/O和加解密解耦的工作方式的效率下降.

但是我这里的多线程并不明显,因为只有两个线程,一个来负责收发,一个负责加解密.

### 分析

可以看到当payload的长度很大的时候,解耦这样看似效率会更高的操作加速的效果却更差,我们来使用vtune来分析一下.--->我们都进行60s数据的采集

```sh
sudo /opt/intel/oneapi/vtune/2025.5/bin64/vtune \
    -collect hotspots \
    -target-duration-type=veryshort \
    -cpu-mask=0-1 \
    --duration 60 \
    --app-working-dir=/home/ada/pwd-writeup/NetworkLayer/dpdk-lab/multi_thread/l2fwd-multi-thread \
    -- /home/ada/pwd-writeup/NetworkLayer/dpdk-lab/multi_thread/l2fwd-multi-thread/l2fwd \
        -l 0-1 -n 4 \
        --vdev=net_pcap0,iface=veth0 \
        --vdev=net_pcap1,iface=veth1 \
        -- -p 0x3 --no-mac-updating
```

#### 64bytes:

```sh
Top Hotspots
Function              Module                   CPU Time  % of CPU Time(%)
--------------------  -----------------------  --------  ----------------
EVP_CipherInit_ex     libcrypto.so.3            42.060s             42.0%
eth_pcap_tx           librte_net_pcap.so.25.2   34.370s             34.3%
crypto_loop           l2fwd                      4.170s              4.2%
EVP_CIPHER_CTX_reset  libcrypto.so.3             3.460s              3.5%
eth_pcap_rx           librte_net_pcap.so.25.2    3.110s              3.1%
[Others]              N/A                       12.920s             12.9%           
```

#### 128bytes

```sh
Top Hotspots
Function           Module                   CPU Time  % of CPU Time(%)
-----------------  -----------------------  --------  ----------------
EVP_CipherInit_ex  libcrypto.so.3            40.260s             40.1%
eth_pcap_tx        librte_net_pcap.so.25.2   34.920s             34.8%
crypto_loop        l2fwd                      4.810s              4.8%
EVP_EncryptUpdate  libcrypto.so.3             3.520s              3.5%
eth_pcap_rx        librte_net_pcap.so.25.2    3.150s              3.1%
[Others]           N/A                       13.810s             13.7%
```

#### 256bytes

```sh
Top Hotspots
Function           Module                   CPU Time  % of CPU Time(%)
-----------------  -----------------------  --------  ----------------
EVP_CipherInit_ex  libcrypto.so.3            37.779s             37.5%
eth_pcap_tx        librte_net_pcap.so.25.2   33.700s             33.5%
EVP_EncryptUpdate  libcrypto.so.3             5.600s              5.6%
crypto_loop        l2fwd                      4.420s              4.4%
eth_pcap_rx        librte_net_pcap.so.25.2    3.920s              3.9%
[Others]           N/A                       15.301s             15.2%
```

#### 512bytes

```sh
Top Hotspots
Function           Module                   CPU Time  % of CPU Time(%)
-----------------  -----------------------  --------  ----------------
EVP_CipherInit_ex  libcrypto.so.3            33.720s             33.5%
eth_pcap_tx        librte_net_pcap.so.25.2   31.600s             31.4%
EVP_EncryptUpdate  libcrypto.so.3            10.100s             10.0%
eth_pcap_rx        librte_net_pcap.so.25.2    4.880s              4.8%
l2fwd_main_loop    l2fwd                      3.750s              3.7%
[Others]           N/A                       16.670s             16.6%
```

#### 1024bytes

```sh
Top Hotspots
Function           Module                   CPU Time  % of CPU Time(%)
-----------------  -----------------------  --------  ----------------
EVP_CipherInit_ex  libcrypto.so.3            28.380s             27.2%
eth_pcap_tx        librte_net_pcap.so.25.2   28.350s             27.2%
EVP_EncryptUpdate  libcrypto.so.3            16.640s             16.0%
eth_pcap_rx        librte_net_pcap.so.25.2   11.400s             10.9%
l2fwd_main_loop    l2fwd                      3.850s              3.7%
[Others]           N/A                       15.670s             15.0%
```

同样我们还要分析一下**不解耦I/O和加密**的多线程程序区别在哪?

#### 64bytes:

```sh
Top Hotspots
Function             Module                   CPU Time  % of CPU Time(%)
-------------------  -----------------------  --------  ----------------
eth_pcap_tx          librte_net_pcap.so.25.2   54.920s             55.1%
EVP_CipherInit_ex    libcrypto.so.3            26.430s             26.5%
EVP_CIPHER_CTX_free  libcrypto.so.3             4.060s              4.1%
eth_pcap_rx          librte_net_pcap.so.25.2    2.940s              3.0%
EVP_EncryptUpdate    libcrypto.so.3             2.430s              2.4%
[Others]             N/A                        8.810s              8.8%
```

#### 128bytes

```sh
Top Hotspots
Function             Module                   CPU Time  % of CPU Time(%)
-------------------  -----------------------  --------  ----------------
eth_pcap_tx          librte_net_pcap.so.25.2   53.860s             53.9%
EVP_CipherInit_ex    libcrypto.so.3            26.100s             26.1%
EVP_EncryptUpdate    libcrypto.so.3             3.850s              3.9%
EVP_CIPHER_CTX_free  libcrypto.so.3             3.520s              3.5%
eth_pcap_rx          librte_net_pcap.so.25.2    3.300s              3.3%
[Others]             N/A                        9.250s              9.3%
```

#### 256bytes

```sh
Top Hotspots
Function             Module                   CPU Time  % of CPU Time(%)
-------------------  -----------------------  --------  ----------------
eth_pcap_tx          librte_net_pcap.so.25.2   53.430s             53.2%
EVP_CipherInit_ex    libcrypto.so.3            24.190s             24.1%
EVP_EncryptUpdate    libcrypto.so.3             6.640s              6.6%
EVP_CIPHER_CTX_free  libcrypto.so.3             3.350s              3.3%
eth_pcap_rx          librte_net_pcap.so.25.2    3.270s              3.3%
[Others]             N/A                        9.610s              9.6%
```

#### 512bytes

```sh
Top Hotspots
Function             Module                   CPU Time  % of CPU Time(%)
-------------------  -----------------------  --------  ----------------
eth_pcap_tx          librte_net_pcap.so.25.2   50.740s             49.9%
EVP_CipherInit_ex    libcrypto.so.3            21.840s             21.5%
EVP_EncryptUpdate    libcrypto.so.3            11.420s             11.2%
eth_pcap_rx          librte_net_pcap.so.25.2    3.690s              3.6%
EVP_CIPHER_CTX_free  libcrypto.so.3             3.480s              3.4%
[Others]             N/A                       10.540s             10.4%
```

#### 1024bytes

```sh
Top Hotspots
Function           Module                   CPU Time  % of CPU Time(%)
-----------------  -----------------------  --------  ----------------
eth_pcap_tx        librte_net_pcap.so.25.2   45.330s             43.5%
EVP_EncryptUpdate  libcrypto.so.3            20.770s             19.9%
EVP_CipherInit_ex  libcrypto.so.3            19.710s             18.9%
real_encrypt       l2fwd                      4.030s              3.9%
eth_pcap_rx        librte_net_pcap.so.25.2    3.980s              3.8%
[Others]           N/A                       10.340s              9.9%
```

























