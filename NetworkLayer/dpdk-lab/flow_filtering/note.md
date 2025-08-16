# flow规则代码

## 理解一些基本概念和工具

就是某种模式匹配 + 动作

我们要使得代码工作:

```sh
sudo ./examples/dpdk-flow_filtering \
  -l 0-1 -n 4 \
  --vdev=net_tap0 \
  -- --non-template

EAL: Detected CPU lcores: 32
EAL: Detected NUMA nodes: 1
EAL: Detected static linkage of DPDK
EAL: Multi-process socket /var/run/dpdk/rte/mp_socket
EAL: Selected IOVA mode 'VA'
:: initializing port: 0
:: promiscuous mode enabled
:: initializing port: 0 done
Flow created!!
src=46:81:74:0E:01:56 - dst=33:33:00:00:00:16 - queue=0x4
src=46:81:74:0E:01:56 - dst=33:33:FF:0E:01:56 - queue=0x3
src=46:81:74:0E:01:56 - dst=33:33:00:00:00:16 - queue=0x4
src=46:81:74:0E:01:56 - dst=33:33:00:00:00:16 - queue=0x0
src=46:81:74:0E:01:56 - dst=33:33:00:00:00:02 - queue=0x2
src=46:81:74:0E:01:56 - dst=33:33:00:00:00:16 - queue=0x0
src=46:81:74:0E:01:56 - dst=33:33:00:00:00:02 - queue=0x2
^C

Signal 2 received, preparing to exit...
```

用虚拟网卡net_tap0.

什么是tap?

**TAP** 是 **“网络虚拟接口”**，全称 **Terminal Access Point**。

它是 **一种软件虚拟网卡**，存在于 Linux 内核中。

特点：

1. **二层以太网接口**，处理 **以太帧**（Ethernet frame）。
2. 可以被用户态程序直接读写（比如 DPDK、QEMU、Open vSwitch）。
3. 用于 **虚拟网络和测试环境**，不依赖真实物理网卡。

在 DPDK 里，`net_tap0` 就是通过 TAP PMD（Poll Mode Driver）访问的虚拟网卡。

还有一些类似的虚拟网卡:

| 类型        | 描述                          | 典型用例                 |
| ----------- | ----------------------------- | ------------------------ |
| **TAP**     | 二层以太网接口，处理以太帧    | DPDK 测试、虚拟机网卡    |
| **TUN**     | 三层网络接口，只处理 IP 包    | VPN（OpenVPN）、隧道测试 |
| **veth**    | 成对的虚拟以太网线            | 容器间通信、Linux 网桥   |
| **macvlan** | 在物理网卡上创建多个 MAC 地址 | 容器多网卡或 IP 隔离     |
| **dummy**   | 虚拟接口，没有硬件            | 测试路由、桥接、ARP实验  |



程序在运行之前会自动创建一个tap虚拟网卡,我们在这个网卡上进行操作.
## 工作的原理

出现了库没有更新的问题:

检查版本:

```sh
❯ pkg-config --modversion libdpdk
24.11.2
```

检查库在哪里:

```sh
❯ pacman -Qs dpdk
local/dpdk 24.11.2-1
    A set of libraries and drivers for fast packet processing
```

我们要从源码构建并且覆盖系统的库:

```sh
meson setup build --prefix=/usr -Dexamples=all
ninja -C build
sudo ninja -C build install
sudo ldconfig
```

> ​	这里可能会遇到内存瓶颈,如果你使用的是16G的,32G应该不会有问题.
>

```sh
❯ pkg-config --modversion libdpdk
25.07.0-rc3
```

这样应该就不会有问题了.





























































