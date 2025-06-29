# Research

> 这是我个人的学习记录......

导引：

​	一个稍微长期的训练流程，里面结合了容器网络、操作系统内核、以及网络控制平面的多方面知识。大致上的思路就是先熟悉容器网络，短期目标是把大规模的srlinux跑起来，并且让控制平面工作。下一步目标是尝试通过uds来实现用户态的数据平面。

## 熟悉容器网络

​	第一部分实验的目的是熟悉容器网络，我们的背景是在一个基于docker容器的虚拟网络中通过配置相关网络配置以达到整体网络连通的目的。在这个虚拟网络中，不同的容器代表着不同的网元（如交换机、路由器、host等），而我们需要通过ssh连接到各自负责的自治系统中，每个自治系统包含着一些网元容器，每位同学负责自己AS内部的网络连通（主要通过OSPF相关配置），以及AS间的网络连通（主要通过BGP相关配置）最终达到整个虚拟网络连通的效果。熟悉一些前置知识包括：
1.BGP与OSPF的基础知识（KO）
2.linux系统的网络配置命令iproute2/net-tools（KO）
3.**虚拟交换机**open vswitch的基础功能以及常用配置命令（?）
4.**虚拟路由器**frrouting的基础功能以及常用配置命令（?）
5.tcpdump与wireshark的基础使用（?）

### 安装并熟悉Docker

Docker中文入门(非常好的入门熟悉的教程)

https://www.ruanyifeng.com/blog/2018/02/docker-tutorial.html

Docker的微服务教程（如何搭建网站）

https://www.ruanyifeng.com/blog/2018/02/docker-wordpress-tutorial.html

Docker官方安装网站

https://docs.docker.com/engine/install/

把docker加入groupadd之后必须重启linux

开始是由于使用了clash代理的问题，导致没有办法pull镜像

> 一个很好的docker命令行练习的网站：https://kodekloud.com/learning-path/docker/

```shell
//练习一些常用命令
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)
docker rmi ubuntu
docker run -d --name webapp nginx:1.14-alpine
docker rmi $(docker images -aq)

```



#### **为什么 Clash 会影响 Docker？**

​	Clash 是一个支持 **代理规则** 的工具，会为你的系统流量提供代理支持。当你使用 Clash 时，它通常会把网络请求通过指定的代理服务器转发。对于 Docker 来说，如果没有正确配置代理，它可能无法通过 Clash 的代理访问外部网络（例如 Docker Hub），从而导致镜像拉取失败。

## BGP和OSPF

https://juejin.cn/post/7119493334968008734

先通过CS144熟悉路由相关的知识

笔记：https://soft-caution-b3f.notion.site/CS144-1506642bfec3808e88ceeeb863353316#1576642bfec380f794caff22d2647918

## iproute2/net-tools工具

https://qixiaoo.github.io/2021/05/13/IpRoute2-%E7%AE%80%E6%98%8E%E6%95%99%E7%A8%8B/

基本网络工具的使用

## 学习Frrouting和OVS的使用

https://blog.csdn.net/puhaiyang/article/details/140189690

简单入门教程(小组网实验)

![frr-ospf](https://i-blog.csdnimg.cn/direct/c4c77cef380f4d788623b66fd7427a05.png)

> 前期基本都是一些组网的实验吧......

#### 第一个小实验：linux容器网络连接基础实验

##### 实验目的

1. 学习如何使用 Linux Bridge 连接多个容器。
2. 掌握容器网络配置的基本方法。
3. 实现容器之间的网络互通（互相 `ping` 通）。

##### 环境：Linux系统

- **软件依赖**：Docker、`iproute2` 工具包。

- **容器镜像**：Ubuntu 的docker image（官方镜像即可）

##### 实验步骤

###### 1.准备：

- iproute2

```bash
# 检查系统是否安装iproute2，安装iproute2
ip -V   
# 下图为安装过的输出
```

```bash
# 安装 Ubuntu为例
sudo apt update
sudo apt install iproute2
```

- 拉取ubuntu的docker镜像

```bash
sudo docker pull ubuntu
```



###### 2. 创建并配置容器

- 创建容器

```bash
sudo docker run -itd --name container1 --privileged --network none ubuntu
sudo docker run -itd --name container2 --privileged --network none ubuntu
sudo docker run -itd --name container3 --privileged --network none ubuntu
```

- 获取容器pid，将容器网络命名空间链接到/var/run/netns

```bash
sudo docker inspect -f '{{.State.Pid}}' container1 # 假设返回的pid为12345
sudo ln -s /proc/12345/ns/net /var/run/netns/container1
# 2,3执行相同操作
```

在 Linux 系统中，网络命名空间（Network Namespace）是一种用于隔离网络资源的机制，使不同的进程拥有各自独立的网络设备、IP 地址、路由表等。这对于容器化技术（如 Docker）尤为重要，因为每个容器通常需要独立的网络环境。

当您使用 Docker 创建容器时，Docker 会为每个容器分配一个独立的网络命名空间。这些命名空间的信息可以在 `/proc/[PID]/ns/net` 路径中找到，其中 `[PID]` 是容器主进程的进程 ID。

然而，默认情况下，`ip netns` 命令仅识别位于 `/var/run/netns/` 目录下的网络命名空间。因此，为了使用 `ip netns` 命令管理 Docker 容器的网络命名空间，需要在 `/var/run/netns/` 目录下为每个容器创建指向其网络命名空间的符号链接。

###### 3.  创建linux bridge（相当于一个交换机）

```bash
sudo brctl addbr mybridge
# 需要启动并分配ip
sudo ip link set mybridge up
sudo ip addr add 192.168.1.1/24 dev mybridge
```

###### 4. 连接：为每个容器创建虚拟网卡并连接mybridge

通过veth pair实现，一端放入bridge，一端放入docker容器

`**veth pair**`（虚拟以太网对）是一种 Linux 内核提供的虚拟网络设备，由成对出现的两个虚拟网络接口组成。这两个接口彼此相连，数据从一端发送，会立即从另一端接收。这种特性使 `veth pair` 常被用于连接不同的网络命名空间（Network Namespace），例如连接两个容器的网络，以实现它们之间的通信。此外，`veth pair` 也常用于连接 Linux Bridge、Open vSwitch（OVS）等虚拟网络组件，构建复杂的虚拟网络拓扑。

需要注意的是，`veth pair` 是点对点的虚拟连接，如果需要将多个命名空间接入同一个二层网络，通常会结合使用 Linux Bridge 来实现。在这种情况下，多个 `veth pair` 的一端连接到各自的命名空间，另一端连接到同一个 Bridge，从而实现多个命名空间之间的互联。

```bash
# 为 container1 创建 veth pair
sudo ip link add veth1 type veth peer name veth1-br
sudo ip link set veth1 netns container1
sudo ip link set veth1-br up
sudo brctl addif mybridge veth1-br

# 在 container1 中配置网络
sudo ip netns exec container1 ip link set dev veth1 name eth0
sudo ip netns exec container1 ip link set eth0 up
sudo ip netns exec container1 ip addr add 192.168.1.2/24 dev eth0

```

这三行命令的作用是**在 `container1` 的网络命名空间中配置 `veth1` 设备，使其成为 `container1` 内部的 `eth0` 接口，并分配 IP 地址**。

```bash
sudo ip netns exec container1 ip link set dev veth1 name eth0
```

**解释**：

- `ip netns exec container1`：在 `container1` 的网络命名空间中执行后续命令。
- `ip link set dev veth1 name eth0`：将 `veth1` 设备的名称改为 `eth0`，这样 `container1` 内部就能识别这个网卡为 `eth0`。

**目的**：

- Docker 容器默认的网络接口是 `eth0`，但 `veth1` 的名字是我们自己创建的。为了让容器能够像普通网络设备一样使用 `veth1`，需要改名为 `eth0`。

```bash
sudo ip netns exec container1 ip link set eth0 up
```

**解释**：

- `ip netns exec container1`：在 `container1` 的网络命名空间中执行后续命令。
- `ip link set eth0 up`：激活 `eth0` 设备，使其开始工作。

**目的**：

- `ip link` 默认创建的网络设备是 `DOWN` 状态，必须手动 `up` 才能开始传输数据。

```bash
sudo ip netns exec container1 ip addr add 192.168.1.2/24 dev eth0
```

**解释**：

- `ip netns exec container1`：在 `container1` 的网络命名空间中执行后续命令。
- `ip addr add 192.168.1.2/24 dev eth0`：为 `eth0` 接口分配 IP 地址 `192.168.1.2`，子网掩码为 `255.255.255.0`（`/24`）。

**目的**：

- 只有分配了 IP 地址，容器才可以与同一网段的其他设备（如另一个容器）进行通信。

------

### **整体作用**

这三行命令的目的是**配置 `container1` 的 `eth0` 网络接口，使其能够通过 Open vSwitch 连接到其他容器，并实现 IP 通信**。

如果你还有 `container2`，你需要执行类似的命令，但 IP 地址应该是 `192.168.1.3`，这样 `container1` 和 `container2` 才能 `ping` 通。

###### 5. 测试连通性

```bash
# 进入container1，尝试ping container2、3
sudo docker exec -it container1 bash
ping 192.168.1.3
ping 192.168.1.4
```

###### 6. 清理实验环境

```bash
# 删除容器
sudo docker rm -f container1 container2 container3
# 删除linux bridge
sudo ip link set mybridge down
sudo brctl delbr mybridge
# 删除netns链接
sudo rm /var/run/netns/container1
sudo rm /var/run/netns/container2
sudo rm /var/run/netns/container3
```

##### 支线：简单搞一个有ping命令的docker镜像（关于DockerFile的学习）

> 这里进行docker的**基础命令练习**：https://kodekloud.com/courses/docker-for-the-absolute-beginner/

```bash
docker ps -a
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)
docker rmi udocker 
run -d --name webapp nginx:1.14-alpinebuntu
docker system prune -a
docker rm -f $(docker ps -aq)  # 强制删除所有容器
docker rmi -f $(docker images -aq)  # 强制删除所有镜像
```

#### 第二个小实验

> 使用一个开源的虚拟交换机Open vSwitch作为交换机连通容器。任务是：配置ovs环境、学习ovs常用操作、通过ovs连通两个host容器。
>
> 和第一个类似，目的就是为了熟悉ovs操作

ovs手册

```
ovs-vsctl: ovs-vswitchd management utility
usage: ovs-vsctl [OPTIONS] COMMAND [ARG...]

Open vSwitch commands:
  init                        initialize database, if not yet initialized
  show                        print overview of database contents
  emer-reset                  reset configuration to clean state

Bridge commands:
  add-br BRIDGE               create a new bridge named BRIDGE
  add-br BRIDGE PARENT VLAN   create new fake BRIDGE in PARENT on VLAN
  del-br BRIDGE               delete BRIDGE and all of its ports
  list-br                     print the names of all the bridges
  br-exists BRIDGE            exit 2 if BRIDGE does not exist
  br-to-vlan BRIDGE           print the VLAN which BRIDGE is on
  br-to-parent BRIDGE         print the parent of BRIDGE
  br-set-external-id BRIDGE KEY VALUE  set KEY on BRIDGE to VALUE
  br-set-external-id BRIDGE KEY  unset KEY on BRIDGE
  br-get-external-id BRIDGE KEY  print value of KEY on BRIDGE
  br-get-external-id BRIDGE  list key-value pairs on BRIDGE

Port commands (a bond is considered to be a single port):
  list-ports BRIDGE           print the names of all the ports on BRIDGE
  add-port BRIDGE PORT        add network device PORT to BRIDGE
  add-bond BRIDGE PORT IFACE...  add bonded port PORT in BRIDGE from IFACES
  del-port [BRIDGE] PORT      delete PORT (which may be bonded) from BRIDGE
  port-to-br PORT             print name of bridge that contains PORT

Interface commands (a bond consists of multiple interfaces):
  list-ifaces BRIDGE          print the names of all interfaces on BRIDGE
  iface-to-br IFACE           print name of bridge that contains IFACE

Controller commands:
  get-controller BRIDGE      print the controllers for BRIDGE
  del-controller BRIDGE      delete the controllers for BRIDGE
  [--inactivity-probe=MSECS]
  set-controller BRIDGE TARGET...  set the controllers for BRIDGE
  get-fail-mode BRIDGE       print the fail-mode for BRIDGE
  del-fail-mode BRIDGE       delete the fail-mode for BRIDGE
  set-fail-mode BRIDGE MODE  set the fail-mode for BRIDGE to MODE

Manager commands:
  get-manager                print the managers
  del-manager                delete the managers
  [--inactivity-probe=MSECS]
  set-manager TARGET...      set the list of managers to TARGET...

SSL commands:
  get-ssl                     print the SSL configuration
  del-ssl                     delete the SSL configuration
  set-ssl PRIV-KEY CERT CA-CERT  set the SSL configuration

Auto Attach commands:
  add-aa-mapping BRIDGE I-SID VLAN   add Auto Attach mapping to BRIDGE
  del-aa-mapping BRIDGE I-SID VLAN   delete Auto Attach mapping VLAN from BRIDGE
  get-aa-mapping BRIDGE              get Auto Attach mappings from BRIDGE

Switch commands:
  emer-reset                  reset switch to known good state

Database commands:
  list TBL [REC]              list RECord (or all records) in TBL
  find TBL CONDITION...       list records satisfying CONDITION in TBL
  get TBL REC COL[:KEY]       print values of COLumns in RECord in TBL
  set TBL REC COL[:KEY]=VALUE set COLumn values in RECord in TBL
  add TBL REC COL [KEY=]VALUE add (KEY=)VALUE to COLumn in RECord in TBL
  remove TBL REC COL [KEY=]VALUE  remove (KEY=)VALUE from COLumn
  clear TBL REC COL           clear values from COLumn in RECord in TBL
  create TBL COL[:KEY]=VALUE  create and initialize new record
  destroy TBL REC             delete RECord from TBL
  wait-until TBL REC [COL[:KEY]=VALUE]  wait until condition is true
Potentially unsafe database commands require --force option.
Database commands may reference a row in each table in the following ways:
  AutoAttach:
    by UUID
    via "auto_attach" of Bridge with matching "name"
  Bridge:
    by UUID
    by "name"
  CT_Timeout_Policy:
    by UUID
  CT_Zone:
    by UUID
  Controller:
    by UUID
    via "controller" of Bridge with matching "name"
  Datapath:
    by UUID
  Flow_Sample_Collector_Set:
    by UUID
    by "id"
  Flow_Table:
    by UUID
    by "name"
  IPFIX:
    by UUID
    via "ipfix" of Bridge with matching "name"
  Interface:
    by UUID
    by "name"
  Manager:
    by UUID
    by "target"
  Mirror:
    by UUID
    by "name"
  NetFlow:
    by UUID
    via "netflow" of Bridge with matching "name"
  Open_vSwitch:
    by UUID
    as "."
  Port:
    by UUID
    by "name"
  QoS:
    by UUID
    via "qos" of Port with matching "name"
  Queue:
    by UUID
  SSL:
    by UUID
    as "."
  sFlow:
    by UUID
    via "sflow" of Bridge with matching "name"

Options:
  --db=DATABASE               connect to DATABASE
                              (default: unix:/var/run/openvswitch/db.sock)
  --no-wait                   do not wait for ovs-vswitchd to reconfigure
  --retry                     keep trying to connect to server forever
  -t, --timeout=SECS          wait at most SECS seconds for ovs-vswitchd
  --dry-run                   do not commit changes to database
  --oneline                   print exactly one line of output per command

Output formatting options:
  -f, --format=FORMAT         set output formatting to FORMAT
                              ("table", "html", "csv", or "json")
  -d, --data=FORMAT           set table cell output formatting to
                              FORMAT ("string", "bare", or "json")
  --no-headings               omit table heading row
  --pretty                    pretty-print JSON in output
  --bare                      equivalent to "--format=list --data=bare --no-headings"

Logging options:
  -vSPEC, --verbose=SPEC   set logging levels
  -v, --verbose            set maximum verbosity level
  --log-file[=FILE]        enable logging to specified FILE
                           (default: /var/log/openvswitch/ovs-vsctl.log)
  --syslog-method=(libc|unix:file|udp:ip:port)
                           specify how to send messages to syslog daemon
  --syslog-target=HOST:PORT  also send syslog msgs to HOST:PORT via UDP
  --no-syslog             equivalent to --verbose=vsctl:syslog:warn

Active database connection methods:
  tcp:HOST:PORT           PORT at remote HOST
  ssl:HOST:PORT           SSL PORT at remote HOST
  unix:FILE               Unix domain socket named FILE
Passive database connection methods:
  ptcp:PORT[:IP]          listen to TCP PORT on IP
  pssl:PORT[:IP]          listen for SSL on PORT on IP
  punix:FILE              listen on Unix domain socket FILE
PKI configuration (required to use SSL):
  -p, --private-key=FILE  file with private key
  -c, --certificate=FILE  file with certificate for private key
  -C, --ca-cert=FILE      file with peer CA certificate
  --bootstrap-ca-cert=FILE  file with peer CA certificate to read or create
SSL options:
  --ssl-protocols=PROTOS  list of SSL protocols to enable
  --ssl-ciphers=CIPHERS   list of SSL ciphers to enable

Other options:
  -h, --help                  display this help message
  -V, --version               display version information

```

开始：

##### **1.用ovs创建bridge**

```shell
sudo ovs-vsctl add-br myovs #创建一个bridge
sudo ovs-vsctl show 		#查看已经创建的bridge

```

##### 2.创建docker容器（之前做的有ping的镜像）

```bash
mostima@mostima-OMEN-by-HP-Gaming-Laptop-16-wf0xxx:~$ sudo docker run -itd --name container1 --privileged  ubuntu-with-ping:latest 
7ab3813f48154cc3a88083644eb2293d1fa924d79c9e53de9465e34ef5280870
mostima@mostima-OMEN-by-HP-Gaming-Laptop-16-wf0xxx:~$ sudo docker run -itd --name container2 --privileged  ubuntu-with-ping:latest 
e4754b5d0ffea944c40421c9eab5fad91b0e54cfe7fffbaf12930bd1713c983f

```

##### 3.配置链接

##### 4.创建veth对

##### 5.配置网络

```bash
sudo ovs-ofctl show br0 #查看某个OVS的端口状态
```

#### 第三部分

> 前面这部分先放在前置总结的知识中......











