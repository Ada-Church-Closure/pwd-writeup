# 多线程/数据包处理的库

1.包需要处理（**加解密，完整性验证，窗口验证**）的情况

2.一个单线程和多线程情况的**对比**

实际上一个lcore就是一个 用户态调度 的线程.

我们直接来看这个L2 forward的代码,就是在本目录下的代码.

![../_images/l2_fwd_vm2vm.svg](https://doc.dpdk.org/guides/_images/l2_fwd_vm2vm.svg)

可以用于两个vm之间的流量转发.

运行可用的参数:

```sh
./<build_dir>/examples/dpdk-l2fwd [EAL options] -- -p PORTMASK
                               [-P]
                               [-q NQ]
                               --[no-]mac-updating
                               [--portmap="(port, port)[,(port, port)]"]
```

例子:

To run the application in linux environment with 4 lcores, 16 ports and 8 RX queues per lcore and MAC address updating enabled, issue the command:

```sh
./<build_dir>/examples/dpdk-l2fwd -l 0-3 -- -q 8 -p ffff
```

To run the application in linux environment with 4 lcores, 4 ports, 8 RX queues per lcore, to forward RX traffic of ports 0 & 1 on ports 2 & 3 respectively and vice versa, issue the command:

```sh
./<build_dir>/examples/dpdk-l2fwd -l 0-3 -- -q 8 -p f --portmap="(0,2)(1,3)"
```

ds的翻译:

# 

## 16. L2 转发

- **源 MAC 地址** 被替换为 **TX_PORT 的 MAC 地址**
- **目的 MAC 地址** 被替换为 **02:00:00:00:00:TX_PORT_ID**

应用程序需要许多命令行选项 ：

```
./<build_dir>/examples/dpdk-l2fwd [EAL options] -- -p PORTMASK
                               [-P]
                               [-q NQ]
                               --[no-]mac-updating
                               [--portmap="(port, port)[,(port, port)]"]
```

其中：

- `-p PORTMASK`：要配置的端口的十六进制位掩码。
- `-P`：（可选）将所有端口设置为**混杂模式**，**以便无论 MAC 目的地址如何都接受数据包。**没有此选项，则仅接受 MAC 目的地址设置为端口以太网地址的数据包。
- `-q NQ`：**每个 lcore 的最大队列数**（默认为 1）。
- `--[no-]mac-updating`：启用或禁用 **MAC 地址更新**（默认启用）。
- `--portmap="(port,port)[,(port,port)]"`：确定**转发端口映射**。

要在 linux 环境中运行应用程序，使用 4 个 lcore、16 个端口、每个 lcore 8 个 RX 队列并启用 MAC 地址更新，请发出命令：

```sh
./<build_dir>/examples/dpdk-l2fwd -l 0-3 -- -q 8 -p ffff
```

要在 linux 环境中运行应用程序，使用 4 个 lcore、4 个端口、每个 lcore 8 个 RX 队列，将端口 0 和 1 的 RX 流量分别转发到端口 2 和 3，反之亦然，请发出命令：

```sh
./<build_dir>/examples/dpdk-l2fwd -l 0-3 -- -q 8 -p f --portmap="(0,2)(1,3)"
```

## 测试程序

创建虚拟网卡,来进行转发的测试:--->我们先创建了这样的虚拟网卡对.

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

现在比如说我们要让这个两个端口之间进行转发的操作:

```sh
sudo ./build/examples/dpdk-l2fwd -l 0-1 -n 4 --vdev=net_pcap0,iface=veth0 --vdev=net_pcap1,iface=veth1 -- -p 0x3 --no-mac-updating
```

现在我们触发一些流量:

```sh
ping -I veth0 192.168.1.2
```

这里会unrecheable因为这是在l2层上面.

结果就会打印一些状态:

```sh
Port statistics ====================================
Statistics for port 0 ------------------------------
Packets sent:                   658158
Packets received:               658158
Packets dropped:                     0
Statistics for port 1 ------------------------------
Packets sent:                   658158
Packets received:               658158
Packets dropped:                     0
Aggregate statistics ===============================
Total packets sent:            1316316
Total packets received:        1316316
Total packets dropped:               0
====================================================

Port statistics ====================================
Statistics for port 0 ------------------------------
Packets sent:                   674520
Packets received:               674520
Packets dropped:                     0
Statistics for port 1 ------------------------------
Packets sent:                   674520
Packets received:               674520
Packets dropped:                     0
Aggregate statistics ===============================
Total packets sent:            1349040
Total packets received:        1349040
Total packets dropped:               0
====================================================
```

接下来我们再来看加解密功能的l2转发应用.

## 14. 带加密功能的L2转发

> 单向的只能加密或者解密的操作.

- **目标端口** 由启用的端口掩码决定（例如端口掩码 `0xf` 时，端口0和1互转，端口2和3互转）。
- **MAC地址更新**（可选）： 源MAC替换为 **TX端口的MAC地址** 目的MAC替换为 **02:00:00:00:00:TX_PORT_ID**

#### **加解密的基本问题**

加密和认证:这里AES是对称加密.

1.加密 用AES-CBC算法和一个128bit密钥加密 每一个块加密依赖于前一个块的密文

2.认证--->Authentication 用SHA1-HMAC生成认证码 接收方用key重新计算HMAC保证数据的**完整性**

IV--->种子?初始化的向量



#### **命令行参数**

```C
./<build_dir>/examples/dpdk-l2fwd-crypto [EAL参数] -- [应用参数]
```

| 参数                    | 说明                                                         |
| ----------------------- | ------------------------------------------------------------ |
| `-p PORTMASK`           | 十六进制端口掩码（默认启用所有端口）                         |
| `-q NQ`                 | 每个lcore的队列数（默认1）                                   |
| `-s`                    | 单核心管理所有端口                                           |
| `-T PERIOD`             | 统计信息打印间隔（秒）                                       |
| `--cdev_type HW/SW/ANY` | 密码设备类型：硬件/软件/任意（默认ANY）                      |
| `--chain`               | 操作链：`CIPHER_HASH`（加密后哈希）、`HASH_CIPHER`、`CIPHER_ONLY`、`HASH_ONLY` 或 `AEAD`（默认 `CIPHER_HASH`） |
| `--cipher_algo`         | 加密算法（默认 `aes-cbc`）                                   |
| `--cipher_op`           | 加密操作：`ENCRYPT` 或 `DECRYPT`（默认加密）                 |
| `--cipher_key`          | 加密密钥（字节用 `:` 分隔，如 `00:01:02...`）                |
| `--auth_algo`           | 认证算法（默认 `sha1-hmac`）                                 |
| `--auth_op`             | 认证操作：`GENERATE` 或 `VERIFY`（默认生成）                 |
| `--aead_algo`           | AEAD算法（默认 `aes-gcm`）                                   |
| `--sessionless`         | 不使用密码会话                                               |
| `--cryptodev_mask`      | 密码设备掩码（默认所有设备）                                 |
| `--[no-]mac-updating`   | 启用/禁用MAC地址更新（默认启用）                             |

#### **示例命令**

```sh
./l2fwd-crypto -l 0-1 --vdev "crypto_aesni_mb0" --vdev "crypto_aesni_mb1" -- \
    -p 0x3 --chain CIPHER_HASH \
    --cipher_op ENCRYPT --cipher_algo aes-cbc \
    --cipher_key 00:01:02:03:04:05:06:07:08:09:0a:0b:0c:0d:0e:0f \
    --auth_op GENERATE --auth_algo aes-xcbc-mac \
    --auth_key 10:11:12:13:14:15:16:17:18:19:1a:1b:1c:1d:1e:1f
```

> **注意**：
>
> - 需确保密码设备支持指定操作（硬件设备需绑定DPDK驱动，虚拟设备需通过 `--vdev` 创建）。
> - 每个以太网端口需对应一个密码设备。
> - 所有密码设备使用相同的会话配置。

#### **密码设备初始化**

1. **检查设备能力**：

   ```sh
   cap = check_device_support_cipher_algo(options, &dev_info, cdev_id);
   if (cap == NULL) return -1; // 设备不支持算法则报错
   ```

2. **验证密钥/IV长度**：

   ```sh
   if (check_supported_size(key_len, cap->min, cap->max, cap->increment) != 0)
       return -1; // 长度不匹配则报错
   ```

我想处理成这样:

![image-20250913211208449](../../../../mio/static/img/image-20250913211208449.png)

当然这是一个单向加密的流程:

创建一些虚拟设备来运行这个程序(最简单的情况):

```sh
❯ sudo ./examples/dpdk-l2fwd-crypto -l 0-1 --vdev "net_pcap0,iface=veth0" --vdev "net_pcap1,iface=veth1" --vdev "crypto_openssl0" --vdev "crypto_openssl1" -- -p 0x3 --chain CIPHER_ONLY --cipher_op ENCRYPT --cipher_algo aes-cbc --cipher_key 00:01:02:03:04:05:06:07:08:09:0a:0b:0c:0d:0e:0f --cipher_iv 11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff:00
```

做了这样的事情,单纯把一个port收到的数据包加密然后转发,另一边是对称的.

![../_images/l2_fwd_encrypt_flow.svg](https://doc.dpdk.org/guides/_images/l2_fwd_encrypt_flow.svg)

加密放进crypto队列,然后出队列改mac地址转发.

运行问题,还是用类似的方法来触发一些流量:

```sh
Port statistics ====================================
Statistics for port 0 ------------------------------
Packets sent:                            30544
Packets received:                        30574
Packets dropped:                             0
Statistics for port 1 ------------------------------
Packets sent:                            30544
Packets received:                        30574
Packets dropped:                             0
Crypto statistics ==================================
Statistics for cryptodev 0 -------------------------
Packets enqueued:                        30544
Packets dequeued:                        30544
Packets errors:                              0
Statistics for cryptodev 1 -------------------------
Packets enqueued:                        30544
Packets dequeued:                        30544
Packets errors:                              0
Aggregate statistics ===============================
Total packets received:                  61148
Total packets enqueued:                  61088
Total packets dequeued:                  61088
Total packets sent:                      61088
Total packets dropped:                       0
Total packets crypto errors:                 0
====================================================

Port statistics ====================================
Statistics for port 0 ------------------------------
Packets sent:                            38781
Packets received:                        38813
Packets dropped:                             0
Statistics for port 1 ------------------------------
Packets sent:                            38781
Packets received:                        38813
Packets dropped:                             0
Crypto statistics ==================================
Statistics for cryptodev 0 -------------------------
Packets enqueued:                        38781
Packets dequeued:                        38781
Packets errors:                              0
Statistics for cryptodev 1 -------------------------
Packets enqueued:                        38781
Packets dequeued:                        38781
Packets errors:                              0
Aggregate statistics ===============================
Total packets received:                  77626
Total packets enqueued:                  77562
Total packets dequeued:                  77562
Total packets sent:                      77562
Total packets dropped:                       0
Total packets crypto errors:                 0
====================================================
```

只有两个网卡相互ping的时候会出现这样的情况,暂时无法理解.

应该是收到了一个数据包就开始疯狂的加密然后转发.

```py
from scapy.all import *
import time

# 配置接口和MAC地址
iface = "veth0"
dst_mac = "02:00:00:00:00:01"  # 目标MAC（需与DPDK配置一致）
src_mac = "02:00:00:00:00:00"  # 源MAC（避免与DPDK冲突）

# 构造自定义以太网帧
pkt = Ether(src=src_mac, dst=dst_mac)/Raw(load=b"TEST_PAYLOAD_1234")

# 发送并监听响应（需在另一终端抓包）
sendp(pkt, iface=iface, loop=1, inter=0.1, verbose=True)
```

直接构造ethernet frame来测试.













