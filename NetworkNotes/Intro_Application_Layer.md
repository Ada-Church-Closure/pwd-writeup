# 计算机网络——自顶向下方法

> 本笔记是来自于《中科大郑烇、杨坚全套《计算机网络（自顶向下方法 第7版，James F.Kurose，Keith W.Ross）》课程》的总结，因为有先一步了解计算机网络的需要，本文使用的几乎所有图片都来自与此书的英文版本。
>
> 相关计算机网络学习的好的资源也会放在这里（自己查一下，不放链接了）：
>
> 1.小林coding：图解计算机网络。
>
> 2.著名的CS144,建议做lab，并且lab分为两个版本，原来的Sponge和现在的minnow，你可以做第二个。
>
> 3.https://www.educoder.net/paths/zecl9i6m 这是一个比较好的可以用来实践的平台，入门相对简单，组网实验也是图形化的Gns3(整体真的偏简单，适合入门，但是文档写的**相当一般**)。
>
> 在我学习期间，我会做以上两个实验（最好能做完吧）。
>
> 4.如果你对于网络安全感兴趣：https://app.cybrary.it/ ，这个网站上的**免费实验**也会让你有一个很好的了解。
>
> 5.*是我认为的重点内容，不是额外内容。
>
> 6.为了防止篇幅过长，（一）主要是关于应用层和传输层的内容，（二）是其余的内容。
>
> 7.主要代码实现都用Python，只是因为语法简单，不容易陷入大量的细节问题。
>
> 8.关于wireshark的使用，你可以看3中的小实验，也可以看4中的实验，进一步深入的话就看书（分析艺术）或者文档。

## 1.计算机网络和因特网

## 1.1什么是Internet？

网络，计算机网络，互联网

节点：主机节点，交换节点（路由器，交换机）

链路：主机到交换，接入链路；交换到交换：骨干链路

协议（Protocol）：**对等层的实体**在通信的过程中应当遵守的规则的集合。

端系统（end system/host）：主机，OS，APP，proc

主机进程间通信，工作在进程（应用层）以下的称为**基础设施**（为了向上层提供服务）。

## 1.2网络边缘

边缘（edge）：主机，应用程序。

核心（core）：互联的路由器，网络的网络。---》大的交换设备（switch跟开关一样，瞬间打通）

链接处（access）：接入的系统，链接边缘和核心。

交互模式：CS模式（服务器可扩展性差） / peer to peer 对等模式（迅雷）

通信方式：1.面向连接的通信方式（TCP协议，UDP协议）

## 1.3网络核心

端系统之下的交换机和路由所组成的网状网络。

### 交换方式：

#### 1.分组交换（packet switch）

1.占用全部带宽。

2.用packet的形式传送数据。

> [!WARNING]
>
> 注意：发送的同时也在接收。

3.存储转发时延

在每个路由器进行存储 （**全部存储---》为了不一直占用全部带宽，存储期间可以给其他路使用，这会导致端到端的时延，因为必须要等存储了之后才可以发送（有N条链路的情况）：**
$$
d端到端 = NL/R
$$
）+ 转发的过程。

4.排队时延和分组丢失

router有输出缓存和输出队列---》缓存满了---》丢包

能够支持的用户数量更多，应对突发性。

a.数据报网络：面向连接（TCP，路由不维护连接的状态，体现end to end的思想）

b.虚电路：有连接（确实建立了连接但是要维护，不体现end to end的思想）

#### 2.电路交换

不共享（要维护mapping），保证性能，但是资源有浪费（占用了资源，computer建立连接有突发性），不能并发，可靠性不高。

带宽分解：多路复用的形式。（时分，波分(光纤通信)，频分）

不适合computer

## 1.4接入网和物理媒体

modem

光纤---同轴电缆

无线网络：1.WLAN2.无限广域网络

媒体(第0层)：发射到接收之间的要通过的介质。

导引性媒体：双绞铜线，光缆（光信号），电缆。

非导引性媒体：LAN（WiFI），wide-area（蜂窝网络，5G网络）,卫星。

## 1.5Internet结构和ISP

ISP（Internet Service Provider--->网络服务的供应商）；端系统通过接入ISP进入互联网。

网络的网络，ISP之间的相互连接（Global ISP建立合约，peering link合作，对等连接条件之下没有资金流动）。

ICP（那就是互联网公司）：提供业务，内容。（google， baidu）全球部署DC（Data Center）来更好的提供服务，用专线连在一起，并且要部署在离ISP比较近的地方。

## 1.6分组交换网中的时延，丢失，吞吐量

### 分组时延的类型

#### 1.节点处理时延

检查错误，分往哪个方向 数据包处理过程中消耗的时间.

#### 2.排队时延

在router的buffer中排队的时间

#### 3.传输时延(transmition)

把数据全部打出去所需要的时间

#### 4.传播时延(propagation)

在物理介质上传播的时间（电磁波的速度传播）

#### 排队时延和丢包

流量强度：
$$
I = \frac{La}{R}
$$
a:每秒到达router的pkt的数量

L:每个pkt的字节数

R:传输速率

> **设计系统时，流量强度不能大于1！**--->不然肯定会丢包.

#### 端到端时延

ICMP（互联网报文控制协议）数据报设置TTL（Time to Live）过一个router减1,那么减为0的时候向原来的主机发送消息。

工具：Traceroute相当于是每到一个router就向原来的分组发送一个控制消息。

检查一下：后面就是到每个路由器的时间

***可能是内部的网络，为了保证隐私性

```bash
mostima@mostima-OMEN-by-HP-Gaming-Laptop-16-wf0xxx:~$ traceroute stanford.edu
traceroute to stanford.edu (171.67.215.200), 30 hops max, 60 byte packets
 1  _gateway (10.173.255.254)  2.338 ms  4.583 ms  5.232 ms
 2  10.6.11.70 (10.6.11.70)  1.677 ms  1.979 ms  1.967 ms
 3  10.55.192.1 (10.55.192.1)  3.255 ms  4.523 ms  4.513 ms
 4  111.19.5.197 (111.19.5.197)  3.919 ms  3.908 ms 120.192.227.113 (120.192.227.113)  4.314 ms
 5  120.192.202.9 (120.192.202.9)  3.752 ms 120.192.202.5 (120.192.202.5)  4.990 ms  16.137 ms
 6  221.183.63.21 (221.183.63.21)  4.986 ms 221.183.63.17 (221.183.63.17)  13.923 ms 221.183.63.21 (221.183.63.21)  3.576 ms
 7  221.183.137.89 (221.183.137.89)  37.311 ms 221.183.41.49 (221.183.41.49)  44.156 ms 221.183.137.1 (221.183.137.1)  29.443 ms
 8  221.183.167.30 (221.183.167.30)  58.761 ms 221.183.166.214 (221.183.166.214)  58.763 ms 221.183.167.26 (221.183.167.26)  58.758 ms
 9  * 221.183.92.206 (221.183.92.206)  58.746 ms  58.715 ms
10  221.183.92.198 (221.183.92.198)  58.697 ms 221.183.92.190 (221.183.92.190)  58.714 ms  58.708 ms
11  * * 223.120.14.253 (223.120.14.253)  285.786 ms
12  223.120.11.58 (223.120.11.58)  237.840 ms  235.645 ms 223.120.10.226 (223.120.10.226)  278.914 ms
13  port-channel6.core2.lon5.he.net (216.66.90.72)  283.376 ms  277.464 ms  277.835 ms
14  port-channel6.core1.bos2.he.net (184.104.194.207)  274.228 ms  274.225 ms  276.115 ms
15  * port-channel10.core2.chi1.he.net (184.104.188.148)  283.571 ms *
16  * * *
17  * * *
18  port-channel5.core6.fmt2.he.net (184.105.222.5)  287.845 ms * *
19  100ge0-74.core2.pao1.he.net (184.104.188.166)  288.180 ms * *
20  stanford-university.e0-62.core2.pao1.he.net (184.105.177.238)  297.145 ms  298.102 ms  307.545 ms
21  campus-ial-nets-b-vl1102.SUNet (171.66.255.196)  299.222 ms campus-ial-nets-a-vl1018.SUNet (171.64.255.228)  307.742 ms campus-ial-nets-a-vl1002.SUNet (171.64.255.196)  307.708 ms
22  * * *
23  web.stanford.edu (171.67.215.200)  290.001 ms  306.817 ms  306.780 ms

```

##### ICMP Ping的简单实现

要对于IP和ICMP的header的组成比较熟悉，后面我们会在网络层提到这些构造。

```python
#ICMPPing.py

import socket
import os
import struct
import time
import select

ICMP_ECHO_REQUEST = 8

#生成校验和
def checksum(str):
    csum = 0
    countTo = (len(str) / 2) * 2
    count = 0
    while count < countTo:
        thisVal = str[count + 1] * 256 + str[count]
        csum = csum + thisVal
        csum = csum & 0xffffffff
        count = count + 2
    if countTo < len(str):
        csum = csum + str[len(str) - 1].decode()
        csum = csum & 0xffffffff
    csum = (csum >> 16) + (csum & 0xffff)
    csum = csum + (csum >> 16)
    answer = ~csum
    answer = answer & 0xffff
    answer = answer >> 8 | (answer << 8 & 0xff00)
    return answer


#接收一次Ping的返回消息
def receiveOnePing(mySocket, ID, sequence, destAddr, timeout):
    timeLeft = timeout

    while 1:
        startedSelect = time.time()
        whatReady = select.select([mySocket], [], [], timeLeft)
        howLongInSelect = (time.time() - startedSelect)
        if whatReady[0] == []:  # Timeout
            return None

        timeReceived = time.time()
        recPacket, addr = mySocket.recvfrom(1024)
        
        header = recPacket[20:28]
        type, code, checksum, packetID, sequence = struct.unpack('!bbHHh', header)
        if type == 0 and packetID == ID:  # type should be 0
            byte_in_double = len(recPacket) - 28
            timeSent = struct.unpack("!d", recPacket[28:28 + struct.calcsize("d")])[0]
            delay = timeReceived - timeSent
            ttl = recPacket[8]
            return (delay, ttl, byte_in_double)
        
        timeLeft = timeLeft - howLongInSelect
        if timeLeft <= 0:
            return None

#发送一次Ping数据包
def sendOnePing(mySocket, ID, sequence, destAddr):
    # 头部构成： type (8), code (8), checksum (16), id (16), sequence (16)

    myChecksum = 0
    # Make a dummy header with a 0 checksum.
    # struct -- Interpret strings as packed binary data
    header = struct.pack("!bbHHh", ICMP_ECHO_REQUEST, 0, myChecksum, ID, sequence)
    data = struct.pack("!d", time.time())
    # 计算头部和数据的校验和
    myChecksum = checksum(header + data)

    header = struct.pack("!bbHHh", ICMP_ECHO_REQUEST, 0, myChecksum, ID, sequence)
    packet = header + data

    mySocket.sendto(packet, (destAddr, 1))  # AF_INET address must be tuple, not str
    # Both LISTS and TUPLES consist of a number of objects
    # which can be referenced by their position number within the object

#向指定地址发送Ping消息
def doOnePing(destAddr, ID, sequence, timeout):
    icmp = socket.getprotobyname("icmp")

    # 创建原始套接字
    mySocket = socket.socket(socket.AF_INET, socket.SOCK_RAW, icmp)
 
    sendOnePing(mySocket, ID, sequence, destAddr)
    delay = receiveOnePing(mySocket, ID, sequence, destAddr, timeout)

    mySocket.close()
    return delay

#主函数Ping
def ping(host, timeout=1):
    
    # timeout=1指: 如果1秒内没从服务器返回，客户端认为Ping或Pong丢失。
    dest = socket.gethostbyname(host)
    print("Pinging " + dest + " using Python:")
    print("")
    
    #每秒向服务器发送一次Ping请求
    myID = os.getpid() & 0xFFFF  # 返回进程ID
    loss = 0
    for i in range(4):
        result = doOnePing(dest, myID, i, timeout)
        if not result:
            print("Request timed out.")
            loss += 1
        else:
            delay = int(result[0]*1000)
            ttl = result[1]
            bytes = result[2]
            print("Received from " + dest + ": byte(s)=" + str(bytes) + " delay=" + str(delay) + "ms TTL=" + str(ttl))
        time.sleep(1)  # one second
    print("Packet: sent = " + str(4) + " received = " + str(4-loss) + " lost = " + str(loss))

    return

ping("127.0.0.1")
```



#### 吞吐量

两个主机之间有很多跳，其中会出现瓶颈带宽的情况，当有多个数据流从这条高速的传输链路通过时，这个链路也有可能成为一个瓶颈。

## 1.7协议层次和服务模型

**分层的思想**，层间调用。

交换PDU利用下层所提供的服务以及层间的接口，这一目的也是为了更好的向上层提供服务。

服务是本层功能的一个子集，是能被上层调用的部分，同时也包括了本层和对等层实体之间形成的新的服务（要不然本层的存在就没有意义了）。

**SAP**：**服务访问点**，区分数据传输给哪个用户（比如TCP向上提供多种服务的时候利用了Sockets（套接字））。

**原语**：Socktet API 上层要调用下层的什么服务。

**服务类型**：

面向连接的服务：建立连接之前先进行一些准备，接着建立连接进行通信。

无连接的服务：比如UDP连接的服务，通信之前不用准备。

**协议和服务的关系：本层协议要利用下层的服务来实现，实现本层协议的目的是为了向上层提供更好的服务。**

**DU：数据单元**

上层给下层传输的叫SDU（服务数据单元），本层在前面加一些控制信息（Header）形成PDI（协议数据单元）。

SDU太大---》分解处理

SDU太小---》组合处理

每一层的PDU都有不同的称呼方法。

### 分层

![image-20250323171459104](../../mio/static/img/image-20250323171459104.png)

> 学习之前没有区分清楚过Link和Physical之间的关系。
>
> 在7层协议中，表示和会话（建立，维持。。。）都要在**应用层**实现。
>
> 网卡集成了physical和link层

**Physical**：获取以太网帧（frame）转换成物理信号（bit信号）传输到邻近的网络元素。

**Link**：**相邻两点之间**传输以帧(frame)为单位的数据，哪些01数据表明帧的开始和帧的结束。P2P

**Network**：提供源主机到目标主机之间的端到端的传输，单位是分组或者数据报（datagram），并且不可靠。E2E---》转发（局部的向哪个方向转发），路由（全局，找路，有很多的路由选择协议）

**Transport**：1.在E2E的基础上完成进程到进程的区分。2.IP层的服务可能不可靠，TCP可以提供可靠的服务，单位是报文段（segment）。

**Application**:单位是报文（message）。

![image-20250323175218468](../../mio/static/img/image-20250323175218468.png)

### 封装和解封装

每遇到一个设备，向上解封装，向下封装，有几层就进行几次。

![image-20250323174714022](../../mio/static/img/image-20250323174714022.png)

> The Internet and all that it enables is a vast new frontier, full of amazing challenges. There
> is room for great innovation. Don’t be constrained by today’s technology. Reach out and
> imagine what could be and then make it happen. ----Leonard Kleinrock

## 2.应用层（Application Layer）

## 2.0总述

原理 应用实例 TCP UDP套接字编程

## 2.1应用层原理

#### 应用程序体系结构 CS P2P

![image-20250324225445342](../../mio/static/img/image-20250324225445342.png)

CS：服务器一直在运行，所有的Client都要访问这个服务器（可扩展性差，并且有性能问题）。

P2P：每个节点既是Server也是Client，但是没有一直运行的IP。

混合系统----》（目录查询是集中式的，但是文件分发是P2P）正反馈的效应，用户越多，能提供的资源也就越多。（迅雷等）

#### 进程通信

找进程：标识和寻址。

利用层间SAP，调用下层的API。

解决上述的问题：

##### 进程寻址（addressing）：

IP（主机地址） + TCP/UDP（使用哪个传输层协议进行通信） + **端口（Port）号**（在应用层工作的哪个进程。TCP和UDP使用方式不同，有各自的端口号）。

那么就是一个端节点（**End** **Point**）就是一个 **IP** + **Port**来唯一确定，从而进行通信的操作。

##### 传输层提供的服务

层间接口要带有的信息：IP + PORT（源主机） / DATA / IP + PORT（目标主机）

**Socket API**：是一个整数，用来减少层间传播的信息量，并且便于管理（这是本地意义下的一个标识）：
$$
Socket Number = f(sIp,sPort,dIp,dPort);
$$
代表这样的一个四元组，可以简单理解为 ip + port number，就是建立了一个映射（这个socket就是映射到两个主机之间的会话的关系），每次进行查找，这对于网络层也是透明的。

> 注意：TCP和UDP的socket不一样，TCP是四元组，UDP仅包含本机的IP和PortNumber。

**UDP** **Socket**: 本机Socket + data + **对方的IP + PortNumber**

**TCP** **Socket**： 本机Socket + data

传输层提供的服务：

丢失 延迟 吞吐 安全性

#### 常见应用的要求：

![image-20250325193608484](../../mio/static/img/image-20250325193608484.png)



常见应用和他们使用的协议：

![image-20250325194328714](../../mio/static/img/image-20250325194328714.png)

安全TCP，app运作在ssl之上，也就是https，此时增强TCP的安全性。

利用ssl库，和应用进程一起运作。

## 2.2 Web And Http

> 要想更好的理解协议，要么手写一遍，要么就多用wireshark分析分析。

Web页包含基本的html文件：base html。

URL对于网页的每个对象进行引用。

### HTTP：

CS工作模式：

![image-20250325200351208](../../mio/static/img/image-20250325200351208.png)

运作在TCP之上，80端口。

##### **非持久性HTTP** http/1.0

![image-20250325202218145](../../mio/static/img/image-20250325202218145.png)

​	针对于请求的对象每次都要建立一次连接，每次请求完了之后都要关闭请求，每次都要承受两倍的RTT（但是TCP可以并发请求）（Round-Trip Time：1.发起TCP连接2.发起对象文件的请求）

##### **持久性HTTP** http/1.1

一个连接之上不停传输文件，不断开连接。

分为两种：1.流水线并行。2.上一次请求的对象回来之后，才进行下一次对象的请求。

#### 1.http报文格式

##### a.http请求报文

```http
GET /somedir/page.html HTTP/1.1
Host: www.someschool.edu
Connection: close
User-agent: Mozilla/5.0
Accept-language: fr
```

**第一行**：请求行 GET/POST（上载数据---》提交表单）/HEAD/PUT/DELETE /.../../.../../... HTTP/1.1

常用的请求方法：

![预览大图](../../mio/static/img/Y0pDVkdKSnMvcktwN0RMbW9TZTVpQT09.png)

其中用得最多的方法是 get 方法和 post 方法，二者的区别：

```txt
get 直接在浏览器输入，post 需要工具发送请求；
get 用 url 或者 cookie 传参，post 将数据放在 body 中；
get 的 URL 有长度限制，post 数据可以非常大；
post 比 get 安全，因为 URL 看不到数据；
get 用来获取数据，post 用来发送数据。
```

**其余**：首部行

以下就是一个请求的通用格式：

![image-20250325204230702](../../mio/static/img/image-20250325204230702.png)

##### b.http状态报文

```http
HTTP/1.1 200 OK
Connection: close
Date: Tue, 18 Aug 2015 15:44:04 GMT
Server: Apache/2.2.3 (CentOS)
Last-Modified: Tue, 18 Aug 2015 15:11:03 GMT
Content-Length: 6821
Content-Type: text/html
(data data data data data ...)
```

第一行：status line

后六行： header line

之后传输数据：entity body

**这里,TCP不维护报文的边界，所以有Content-Length，应用进程要自己区分boundary。**

> 到这里为止，建议做CS144的**lab0**,对于理解会很有帮助。

以下就是一个响应报文的格式：

![image-20250327151631891](../../mio/static/img/image-20250327151631891.png)

http响应的状态码：

![预览大图](../../mio/static/img/T3BRMTd2S0l5OGpnWUIxS0c2WjFxZz09.png)

#### 2.http协议缓存

类似于我们之后会提到的web cache

当第一次做请求的时候：

![预览大图](../../mio/static/img/WHdRMmJJZ1ROTjRlWm1icnMrSXhmQT09.png)

直接向资源服务器做请求，获取结果之后，我们根据这个头信息决定是把它存放在内存还是硬盘。

当我们再次请求的时候：

![预览大图](../../mio/static/img/OGp6d25DYkxhNU5sa3hsekhqT2NQZz09.png)

浏览器要根据http头部信息来决定从哪里加载页面：

![预览大图](../../mio/static/img/THIzYncrUmx3YWVJOWRQa0Z1SDhodz09.png)

##### HTTP报文中与缓存有关的字段

HTTP 状态码（status code）

​	**200** 请求成功，浏览器会把响应回来的信息显示在浏览器端；
​	**304** 第一次访问一个资源后，浏览器会将该资源缓存到本地；第二次再访问该资源时，如果该资源没有发生改变或失效，那么服务器响应给浏览器 304 状态码，告诉浏览器使用本地缓存的资源。

​	**HTTP 响应时，如何判断是该返回 200 还是 304 呢？**与之相关的字段是： 
**Last-Modified**： 表示这个响应资源的最后修改时间。web 服务器在响应请求时，告诉浏览器资源的最后修改时间。
**If-Modified-Since**： 当资源过期时（使用 Cache-Control 标识的 max-age），发现资源具有 Last-Modified 声明，则再次向 WEB 服务器请求时，带上 If-Modified-Since，表示请求时间。WEB 服务器收到请求后发现有 If-Modified-Since 则与被请求资源的最后修改时间进行比对。若最后修改时间较新，说明资源有被改动过，则响应资源内容（写在响应消息包体内），HTTP 200 ；若最后修改时间较旧，说明资源无新修改，则响应 HTTP 304 (无需包体，节省流量)，告知浏览器继续使用缓存（就是之后会讲到的CDN）。

​	如果你想观察到缓存的协议：

1.启动浏览器，确保浏览器的缓存被清除。在 Firefox 下执行此操作，请选择“工具” - > “清除最近历史记录”，然后检查缓存框；
2.启动 Wireshark 数据包嗅探，在浏览器中输入某一 URL ，浏览器应显示一个 HTML 文件；
3.再次快速地将相同的 URL 输入到浏览器中（或者只需在浏览器中点击刷新按钮）；
4.停止 Wireshark 数据包捕获，并在 display-filter-specification 窗口中输入“http”，以便只捕获 HTTP 消息，并在数据包列表窗口中显示。

#### 3.http怎么传输一个长文件

> 1.HTTP 协议中对长文件的分段传输机制。
> 2.HTTP 报文中与报文分段相关的字段。

之后我们也会提到下面的内容：	

​	在以太网中，最大传输单元（MTU）为 1500 个字节，在一个 IP 包中，去除 IP 包头的 20 个字节，可以传输的最大数据长度为 1480 个字节。在 TCP 包中，去除 20 个 TCP 包头，可以传输的最大数据段为 1460 个字节。因此，当数据超过最大数据长度时，将对该数据进行分片处理，在 IP 包头中会看到有多个片在传输，但标识号是相同的，表示是同一个数据包。
​	HTML 文件相当长时，例如： 4861 字节太大，一个 TCP 数据包不能容纳。因此，单个 HTTP 响应消息由 TCP 分成几个部分，每个部分包含在单独的 TCP 报文段中，如下图：长度为 4861 的报文被分为长度分别为 1440，1440，1440，541 的 4 个 TCP 段，编号分别为 8715，8716，8718，8719 。

![预览大图](../../mio/static/img/c0UwTDd3SG5tdzJFWTRIR3JmcFpDUT09.png)

#### 4.http对于嵌入对象网页的处理

稍微了解一点html等描述语言的。

标签作为一个占位符号，链接到指定的资源。

##### **嵌入对象的连接方式**

在客户端请求有嵌入对象的网页时，除开要对请求的页面文件进行响应之外，还需要对网页中嵌入的每个对象进行发起请求。对嵌入对象的请求可以采用串行方式或并行方式。

###### **串行方式**

假设一个包含了 3 个嵌入图片的 WEB 页面，浏览器需要发起 4 个 HTTP GET 请求来显示此页面：1 个用于顶层的 HTML 页面，3 个用于嵌入的图片。浏览器可以先完整地请求原始的 HTML 页面，然后请求第一个嵌入对象，然后请求第二个嵌入对象等，以这种简单的方式对每个嵌入对象串行处理。如图所示：

![预览大图](../../mio/static/img/RGZFNHNMRzcyVUtSWUZ5azM1a3FUQT09.png)

我们先请求base html这就跟一张白纸一样，接着再请求上面相关的资源。

###### 并行方式

串行方式的处理效率非常低。HTTP 允许客户端打开多条连接，并行地执行多个 HTTP GET 请求，并行加载了 4 幅嵌入式图片，每个事务都有自己的 TCP 连接。如图所示：

![预览大图](../../mio/static/img/RkQvVE9VZW1vMUVUTml4SW5VZW8zQT09.png)

​	并行连接的时间线，比单条连接快很多。首先要装载的是封闭的 HTML 页面，然后并行处理其他的 3 个事务，每个事务都有自己的连接。图片中的装载是并行的，连接的时延也是重叠的。

​	从理论上说，并行连接的速度可能会更快。但实际上不一定总是更快的。客户端的网络带宽不足时，大部分的时间可能都是用来传送数据的。在这种情况下，一个连接到速度较快服务器上的 HTTP 事务就会很容易地耗尽所有可用的 Modem 带宽。如果并行加载，每个对象可能会去竞争有限的带宽，每个对象都会以较慢的速度按比例加载，这样带来的性能提升就很小，甚至没有提升。

​	另外，打开大量连接会消耗很多内存资源，从而引发自身的性能问题。复杂的 WEB 页面可能会有数十或数百个内嵌对象。客户端可能可以打开数百个连接，但服务器通常要同时处理很多其他用户的请求，所以很少有 WEB 服务器希望出现这样的情况。这会造成服务器性能的严重下降，对高负荷的代理来说也同样如此。

​	实际上，浏览器使用并行连接时，会将并行连接的总数限制在一个较小的值。服务器可以随意关闭来自特定客户端的超量连接。

###### 持久连接 http/1.1

​	WEB 客户端经常会打开到同一个站点的连接。一个 WEB 页面上的大部分内嵌图片通常都来自同一个 WEB 站点，而且相当一部分指向对象的超链接通常都指向同一个站点。因此，初始化了对某服务器 HTTP 请求的应用程序很可能会在不久的将来对那台服务器发起更多的请求，这种性质被称为站点本地服务（site locality）。

​	HTTP/1.1 允许 HTTP 设备在事务处理结束之后将 TCP 连接保持在打开状态，以便为未来的 HTTP 请求重用现存的连接。在事务处理结束之后，仍然保持在打开状态的 TCP 连接被称为持久连接。非持久连接会在每个事务结束之后关闭。持久连接会在不同事务之间保持打开状态，直到客户端或服务器决定将其关闭为止。

​	重用已对目标服务器打开的空闲持久连接，就可以避开缓慢的链接建立阶段。而且已经打开的链接还可以避免慢启动的拥塞适应阶段，以便更快速地进行数据传输。

###### keep-alive 与 persistent 连接

​	持久连接与并行连接配合使用可能是最高效的方式。现在，很多 WEB 应用程序都会打开少量的并行连接，其中的每一个都是持久连接。持久连接有两种类型：比较老的 HTTP/1.0+"keep-alive" 连接，以及现代的 HTTP/1.1“persistent” 连接。

**keep-alive**

![预览大图](../../mio/static/img/TWIvM3lUTWgzOUpKaDZTYXhETVB6QT09.png)

​	上图对串行连接实现 4 个 HTTP 事务的时间与在一条持久连接上实现同样事务所需的时间线进行了比较，由于去除了进行连接和关闭连接的开销，所以时间线有所缩减。

​	很多 HTTP/1.0 浏览器和服务器支持 keep-alive 连接。但由于受到一些互操作性设计的困扰， HTTP/1.1 逐渐停止了对 keep-alive 连接的支持，用持久连接（persistent connection）的改进型设计取代了它。持久连接的目的与 keep-alive 了解的目的相同，但工作机制更优。

​	与 HTTP/1.0 的 keep-alive 连接不同，HTTP/1.1 持久连接在默认情况下是激活的。除非特别指明，否则 HTTP/1.1 假定所有连接都是持久的。要在事务处理结束之后将连接关闭，HTTP/1.1 应用程序必须向报文中显示地添加一个 Connection：close 首部。

​	HTTP/1.1 客户端假定在收到响应后，除非响应中包含了 Connection：close 首部，不然 HTTP/1.1 连接就仍维持在打开状态。但是，客户端和服务器仍然可以随时关闭空闲的连接。不发送 Connection：close ，并不意味着服务器承诺永远将连接保持在打开状态。

#### 5.http认证

HTTP 中有如下常用认证方式：

Basic 认证
Digest 认证
SSL Client 认证
表单认证

##### 1.HTTP 基本认证 Basic Authentication

> 注意，BASE64只进行了简单的编码，这并不安全，所以我们才需要https。

​	当一个客户端向 HTTP 服务器进行数据请求时，如果客户端未被认证（401），则 HTTP 服务器将通过基本认证过程对客户端的用户名及密码进行验证，以决定用户是否合法。

​	客户端在接收到 HTTP 服务器的身份认证要求后，会提示用户输入用户名及密码，然后将用户名及密码以**BASE64**编码，编码后的字符串将附加于请求信息中。 如当用户名为anjuta，密码为：123456时，客户端将用户名和密码用“：”合并，并将合并后的字符串用BASE64编码，并于每次请求数据时，将密文附加于请求头（Request Header）中。HTTP 服务器在每次收到请求包后，根据协议取得客户端附加的用户信息（BASE64加密的用户名和密码），解开请求包，对用户名及密码进行验证，如果用户名及密码正确，则根据客户端请求，返回客户端所需要的数据；否则，返回错误代码或重新要求客户端提供用户名及密码。

**BASIC** **认证的步骤**：

1.客户端访问一个受 HTTP 基本认证保护的资源；
2.服务器返回 401 状态，要求客户端提供用户名和密码进行认证。（验证失败的时候，响应头会加上 WWW-Authenticate: Basic realm="请求域" ），如下所示：

```http
401 Unauthorized
WWW-Authenticate： Basic realm="WallyWorld"
```

3.客户端将输入的用户名密码用Base64进行编码后，采用非加密的明文方式传送给服务器。

```http
Authorization: Basic xxxxxxxxxx.
```

4.服务器将 Authorization 头中的用户名密码解码并取出，进行验证，如果认证成功，则返回相应的资源；如果认证失败，则仍返回 401 状态，要求重新进行认证。

##### 2.HTTP 摘要认证 Digest Authentication

​	该认证是 HTTP1.1 提出的基本认证的替代方法，不包含密码的明文传递。
摘要认证使用随机数 + MD5 加密哈希函数来对用户名、密码进行加密，在上述第二步时，服务器返回随机字符串 nonnce 之后，客户端发送摘要MD5（HA1:nonce:HA2）。
​	其中HA1=MD5(username:realm:password),HA2=MD5(method:digestURI)。

##### 3.HTTP 开放认证 OAuth Authentication

​	开放认证允许用户提供一个令牌，而不是用户名和密码来访问它们存放在特定服务器的数据，每一个令牌授权一个特定的第三方系统。

##### 4.HTTP（令牌认证） Token Authentication

​	令牌认证是指当用户第一次登陆时，服务器生成一个 token 并返回给客户端，之后的每次访问客户端都会带上该 token，无需再次带上用户名和密码。

#### Cookie：用户和服务器之间的交互

用来维护一个BS之间通信信息的存储状态，比如购物网站维护购物车或者每次不用验证登陆信息的操作（但是这样的同步时间长了会使Server了解用户的更多信息，构成隐私性问题）：

![image-20250327152705679](../../mio/static/img/image-20250327152705679.png)

第一次请求建立连接的时候会Set-cookie建立cookie文件，接着每次请求的时候都会带上这个cookie值。

简单举例子，你可以查看并且清除Cookies:

![image-20250327153851661](../../mio/static/img/image-20250327153851661.png)

#### Web Cache：代理服务器

> 还是经典的Cache理念，Proxy服务器缓存是原服务器的一个子集（也不一定，这取决于缓存的策略，但是和我们的一台计算机中的缓存策略应该有所不同），有利于减少Server压力，并且客户端访问迅速。

![image-20250327154620738](../../mio/static/img/image-20250327154620738.png)

接入链路压力太大，在局域网内部接入一个缓存的节点：

这样就有两点好处：

1.本地访问如果Cache Hit那么访问速度会相当快。

2.就算没有的部分，也会极大降低接入链路的流量强度。

![image-20250327192759303](../../mio/static/img/image-20250327192759303.png)

**解决缓存一致性问题**：

Conditional Get方法：

1.首先Cache向Server请求资源时会获得这样的报文，其中加载了这个数据最新修改时间

```http
HTTP/1.1 200 OK
Date: Sat, 3 Oct 2015 15:39:29
Server: Apache/1.3.0 (Unix)
Last-Modified: Wed, 9 Sep 2015 09:23:24
Content-Type: image/gif
(data data data data data ...)
```

2.每当有主机向Cache请求资源的时候，Cache都会向Server发送一个header line，检查自从上一次之后资源有没有修改：

```http
GET /fruit/kiwi.gif HTTP/1.1
Host: www.exotiquecuisine.com
If-modified-since: Wed, 9 Sep 2015 09:23:24
```

3.如果没有修改，返回一个header line,表明资源可用，不用更新：

```http
HTTP/1.1 304 Not Modified
Date: Sat, 10 Oct 2015 15:39:29
Server: Apache/1.3.0 (Unix)
(empty entity body)
```

### 2.3 FTP

1.双通道通信：数据传输 指令传输

2.有状态连接。

### 2.4 EMail

如图，三个组成部分：1.邮箱代理程序（就是邮箱软件，比如QQ邮箱） 2.邮箱服务器3.SMTP(前面两步都是推，最后一步是拉)

![image-20250327201059138](../../mio/static/img/image-20250327201059138.png)

一个基本的建立连接的过程如下：

![image-20250327202508922](../../mio/static/img/image-20250327202508922.png)

1.用户到邮箱服务器。

2.邮箱服务器之间SMTP服务器建立连接并且发送内容。

3.pop3协议把传到服务器的邮件拿下来。

SMTP服务器之间交换报文的格式：

```html
220 hamburger.edu
HELO crepes.fr
250 Hello crepes.fr, pleased to meet you
MAIL FROM: <alice@crepes.fr>
250 alice@crepes.fr ... Sender ok
RCPT TO: <bob@hamburger.edu>
250 bob@hamburger.edu ... Recipient ok
DATA
354 Enter mail, end with ”.” on a line by itself
Do you like ketchup?
How about pickles?
.
250 Message accepted for delivery
QUIT
221 hamburger.edu closing connection
```

TODO：进一步了解EMail并且做相关实验。

### 2.5 *DNS(Domain Name System)

> 本节课光听课不行，要认真看书，简单一句话：**提供了用域名拿到IP地址的功能**。

可读性

维护和解析

核心功能 在应用层实现

运作在53号端口 **UDP**



作用：别名---》正规名 ---》 域名查询IP地址（mapping）  可以进行**负载均衡， 主机别名，邮件服务器别名**     

**Name** **Space**（命名空间）

和一颗树一样不断向下划分，最上面是顶级域（edu com ......），上层维护了下层的一部分信息。

#### **DNS服务器层次结构**

一个分布式数据库的实现

主要有以下三种DNS服务器：

**root**

**top**

**authoritative**

![image-20250401152910878](../../mio/static/img/image-20250401152910878.png)

name space划分为多个zone

权威DNS Server---》TTL无限大

#### 工作机理

我们每个主机有DNS客户端，向DNS服务器请求信息并且收到域名对应的IP地址，短期之内会进行缓存保证性能。

客户会先从上述的根服务器之一向下进行查找的操作，然后到顶级服务器，最后到权威服务器之一获取IP地址。

本地DNS服务器做查询代理：

##### 迭代查询和递归查询：

![image-20250402163424031](../../mio/static/img/image-20250402163424031.png)

![image-20250402163447672](../../mio/static/img/image-20250402163447672.png)

可以观察到：上图的查询是迭代的，都是本地DNS在不断的访问服务器，而下图是递归的，会带来性能的问题，所以本地主机访问本地DNS服务器是递归的（本地DNS缓存了这条记录，下次访问时可以立即返回IP地址），而本地DNS服务器向外部查询则是迭代的，提升性能。

#### DNS记录和报文

> DNS到底传送了什么样的信息？？？

资源记录(四元组)：
$$
（Name, Value, Type, TTL）
$$
**Type**:

A:权威服务器，精确对应了域名和一个IP地址。

NS：如何找到一个权威的服务器(TLD服务器，但是同时还会包含一条A记录)。

比如当我们注册一个域的时候，在某个TLD com服务器输入：

(networkutopia.com, dns1.networkutopia.com, NS)
(dns1.networkutopia.com, 212.212.212.1, A)

以上两条就能保证你的域能被访问。

通过保存这样的记录的方式，我们就可以通过上层DNS服务器找到下层DNS服务器的信息。

格式：

![image-20250402165149898](../../mio/static/img/image-20250402165149898.png)

```shell
mostima@mostima-OMEN-by-HP-Gaming-Laptop-16-wf0xxx:~$ nslookup 
> baidu.com
Server:		127.0.0.53
Address:	127.0.0.53#53

Non-authoritative answer:
Name:	baidu.com
Address: 39.156.66.10
Name:	baidu.com
Address: 110.242.68.66
> 
```

用nslookup直接查询DNS记录（这是一个非权威的）

> 注意：缓存技术同时也缓解了DDoS攻击的严重性。
>
> 还可以进行中间人攻击，但是也是很难做到的，除非你连接了攻击人的热点之类的......

### 2.6 P2P应用

> 解决server负载过重的问题，这是一类的应用，加入的节点，是Client，也是Server。

### 与传统的 C/S（Client-Server）模型对比

| 特点     | C/S 模型               | P2P 模型         |
| -------- | ---------------------- | ---------------- |
| 结构     | 客户端连接中心服务器   | 每个节点互连     |
| 数据流向 | 客户端 ←→ 服务器       | 节点 ←→ 节点     |
| 单点故障 | 存在（服务器挂了全挂） | 无中心，容错性高 |
| 扩展性   | 扩展成本高             | 节点越多越强大   |

CS应用的举例：

![image-20250406204634159](../../mio/static/img/image-20250406204634159.png)

当Client多的时候，Server的上载带宽就成了瓶颈。



![image-20250406205014280](../../mio/static/img/image-20250406205014280.png)

NF是Server上传了N个大小为F的文件，Us是上传的最大带宽，这是Server的瓶颈（当Client非常多的时候）。

F/d 则是Client的瓶颈。

下图是P2P的模式（文件分发的例子）：

![image-20250406205702624](../../mio/static/img/image-20250406205702624.png)

服务器至少上传一份文件。

**问题**：

1.资源定位

2.节点管理



##### 非结构化P2P

**BitTorrent**（混合式的）

客户端获取一个集中化目录，查看那些peers有这个资源，并且进行请求，并且还会上报自己拥有的资源。

集中化目录服务器的问题：

​	单点故障 性能问题 版权侵犯问题

**BitMap**交换信息，所有节点就能知道其他peer的拥有资源的情况（全部bit都是0：吸血鬼节点，开始先随便获取几个）。

> 谁对我好我对谁好（tit-for-tat）。

接着**最稀缺优先**策略，优先请求最稀缺的block。

**我优先向谁提供服务**：谁能够以最高的速率向我提供资源，我就优先给谁提供服务（疏通：**unchoked**）。

开始两个周期选取优先的提供服务（每次都会进行维护），再来一个周期随机选取一个节点提供服务（可能会出现更好的节点）。

**比如一个torrent文件（我们常说的种子文件）**

有一个tracker服务器维护这个文件的peer的相关信息。

一个 `.torrent` 文件本质上是一个结构化的数据文件，内容大致包括：

| 字段                  | 作用                                        |
| --------------------- | ------------------------------------------- |
| **文件名**            | 你要下载的文件或文件夹的名字                |
| **文件大小**          | 被分割成的块（piece）的数量和每块大小       |
| **piece哈希值**       | 每个块的 SHA-1 校验码，确保数据完整性       |
| **tracker服务器地址** | 告诉你的客户端去哪里找其他正在下载/上传的人 |
| （可选）其他信息      | 创建时间、创建者、备注说明等                |

**Gnutella**

完全分布式：建立一个Overlay（可能就是8-10个的小覆盖网络）。

给所有邻居发送泛洪（flooding）查询，设置一个TTL进行有限flooding。

ping pong机制

**KaZaA**

混合机制。

搜索，分布式文件命名系统。

每个peer要么是组员，要么是组长，组长跟踪每个组员的持有资源的情况，并且和每个组长之间建立连接。

**什么是DHT P2P？**

DHT P2P 是 **分布式哈希表（Distributed Hash Table）** 与 **点对点网络（Peer-to-Peer, P2P）** 相结合的一种技术，用于在**无需中心服务器的情况下查找资源和节点**。它广泛应用于像 **BitTorrent** 这样的去中心化系统中。

有一个环形链表，每个节点保存一部分拥有的资源，根据infohash的值进行逐跳的寻找。

### 2.7 CDN（Content Distribution Network）

> 怎么解决大量的视频流量的问题，还是**Cache魅力时刻**。

视频压缩编码要求，冗余度。

> 关于这部分，如果你很感兴趣，可以去看看ytb上影视飓风被下架的视频。

**流化服务** Dash技术（经HTTP的动态适应性流）

全球部署Cache服务器群，就近给用户提供服务，用户这边是域名解析的重定向。

内容加速服务---》应用层，在边缘网络提供这样的服务。

问题：

1.从哪个CDN获取服务？

2.阻塞怎么办？

3.部署哪些内容？

#### 核心操作过程：

![image-20250408153220516](../../mio/static/img/image-20250408153220516.png)

1.向NC请求网页。

2.向LDNS发送DNS请求。

3.LDNS把这个请求中继到NC的权威服务器，**此时权威服务器不返回IP，而返回KingCDN的主机名，这样就转移了目标**。

4.此时LDNS又去请求KingCDN的权威服务器，这样就可以拿到IP地址，并且建立TCP连接，并且进行这个视频的传输。

### 2.8 TCP和UDP套接字编程

> 掌握socket编程是这里的基础，基本都是在做socketAPI的调用。

TCP：可靠字节流的服务。

UDP：不可靠的服务。

#### 过程：

下面的英文单词一般都是socket api的原语。

1.Server先运行，创建一个socket(welcome socket（这是唯一的，不处理数据，仅仅用来监听），和本地的IP和端口号捆绑bind)，阻塞式等待接收新的用户的连接（创建一个connect socket在welcome socket上等待用户建立连接accept）。

如下图，建立一个TCP连接在server端要两个socket

![image-20250409162006377](../../mio/static/img/image-20250409162006377.png)

2.客户端创建socket（默认和本地的IP和端口号进行bind），和Server建立连接请求（connect）。

3.Server接收到请求，解除accept处的阻塞，返回一个新的端口号。

4.Client 的connect返回有效值，TCP正式建立连接。

5.处理字节流的逻辑。

6.关闭进程，删除socket表项。

如下图是整个过程：

![image-20250409162056412](../../mio/static/img/image-20250409162056412.png)

#### 代码实现

如果你用的是Java（我想大多数人会用的），就去看看这个博客：

https://liaoxuefeng.com/books/java/network/tcp/index.html

关于网络编程讲解的很清晰。

我们用py，为了更简单的理解，而不会一开始陷入语言语法的复杂性中：

Client：

```python
import socket

# 创建 socket 对象
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# 连接服务器
client_socket.connect(('127.0.0.1', 8888))

# 发送数据
client_socket.send("你好，服务器！".encode('utf-8'))

# 接收回复
data = client_socket.recv(1024).decode('utf-8')
print(f"收到服务器回复：{data}")

# 关闭连接
client_socket.close()
```

Server（模拟一个请求连接，并且从server中读取相关的文件并且返回给Client）:

```python
#import socket module
from socket import *

serverSocket = socket(AF_INET, SOCK_STREAM) 
#Prepare a sever socket 
serverSocket.bind(("127.0.0.1",6789))
serverSocket.listen(1)

#while True:
print('开始WEB服务...')
try:
    	#利用监听套接字创建一个连接套接字
		connectionSocket, addr = serverSocket.accept()
		message = connectionSocket.recv(1024) # 获取客户发送的报文
        
        #读取文件内容
        filename = message.split()[1]       
		f = open(filename[1:])
		outputdata = f.read();
		
        #向套接字发送头部信息
		header = ' HTTP/1.1 200 OK\nConnection: close\nContent-Type: text/html\nContent-Length: %d\n\n' % (len(outputdata))
		connectionSocket.send(header.encode())

		#S发送请求文件的内容
		for i in range(0, len(outputdata)):
			connectionSocket.send(outputdata[i].encode())
		
        #关闭连接
        connectionSocket.close()
except IOError:             #异常处理
		#发送文件未找到的消息
		header = ' HTTP/1.1 404 not Found'
       	connectionSocket.send(header.encode())
		#关闭连接
		connectionSocket.close()
#关闭套接字
serverSocket.close()
```

这样能建立一个简单的连接。

可以用tcpdump抓包分析，在socket编程中，三次握手对于我们来说是透明的。

#### UDP（Ping程序）

不连接，不握手（和IP都叫数据报），明确指明对方的IP和端口。

我们用py实现一个UDP 简单ping程序的服务器和客户端

**Client：**

```python
from socket import *
import time

serverName = '127.0.0.1' # 服务器地址，本例中使用本机地址
serverPort = 12000 # 服务器指定的端口
clientSocket = socket(AF_INET, SOCK_DGRAM) # 创建UDP套接字，使用IPv4协议
clientSocket.settimeout(1) # 设置套接字超时值1秒

for i in range(0, 9):
	sendTime = time.time()
	message = ('Ping %d %s' % (i+1, sendTime)).encode()     # 生成数据报，编码为bytes以便发送
	
    try:
    	
    	# 将信息发送到服务器，这和我们理解的UDP过程是相同的，在发送的时候才会把目标的信息放进去
        clientSocket.sendto(message, (serverName, serverPort))
		# 从服务器接收信息，同时也能得到服务器地址
        modifiedMessage, serverAddress = clientSocket.recvfrom(1024)
    
    	rtt = time.time() - sendTime # 计算往返时间
		print('Sequence %d: Reply from %s    RTT = %.3fs' % (i+1, serverName, rtt)) # 显示信息
	except Exception as e:
        print('Sequence %d: Request timed out.' % (i+1))
		
clientSocket.close() # 关闭套接字
```

**Server:**

```python
from socket import *
import random

# 创建UDP套接字
serverSocket = socket(AF_INET, SOCK_DGRAM)
# 绑定本机IP地址和端口号
serverSocket.bind(('', 12000))

num=0
while True:
 
    # 接收客户端消息
    message, address = serverSocket.recvfrom(1024)
    # 将数据包消息转换为大写
    message = message.upper()
        
    num=num+1
    if num>=8:
        break
	#模拟一个丢包的事件
    if num % 3 == 1:
        continue
    
    #将消息传回给客户端
    serverSocket.sendto(message, address)
```

## 面试题目

### 输入网址--->网页显示 过程描述?

1.解析URL 来生成要发给server的请求信息.

​	2.在发送之前,我们要查找这个域名对应的真实IP地址--->DNS查询

> ​	这里就是递归查询或者迭代查询的过程,我们会先向本地的DNS服务器发送请求.
>
> ​	实际不会每次都查询,有多层的缓存,比如浏览器本身的缓存,OS的缓存,hosts文件等,本地DNS服务器也会进行缓存.

3.接下来我们需要**协议栈**的帮助,先寻找TCP协议的帮助.

> TCP其实就是一个软件,它对于这些数据做分片的处理,在原来的http头部之前添加了TCP头部字段.

4.接下来是IP协议,IP协议要给这个**数据加上一个IP的头部**,其中最重要的是**源IP和目标IP(刚才用DNS解析出来的ip)**.

> ​	这里有很多网卡,要根据路由表的规则还选择源IP.
>
> ```sh
> ❯ ip route show
> default via 10.173.255.254 dev wlan0 proto dhcp src 10.173.20.39 metric 20600 
> 10.173.0.0/16 dev wlan0 proto kernel scope link src 10.173.20.39 metric 600 
> ```
>
> 这里是dhcp动态分配的ip.
>
> 其实应该是进行**最长前缀匹配**,都不匹配就走默认网关.

5.数据链路层,在之前的IP头部再加上MACheader.

> 就是自己的MAC地址(厂家写进ROM内部的) + 对方MAC地址.
>
> 怎么找对方的MAC地址?
>
> ​	ARP广播,查询此时的源IP应该往哪个MAC地址进行发送,获取,并且会存放在一个ARP缓存的内部.
>
> ```sh
> ❯ ip neigh show
> 10.173.20.91 dev wlan0 lladdr 9c:71:3a:f5:dc:91 STALE 
> 10.173.255.254 dev wlan0 lladdr 9c:71:3a:f5:dc:91 REACHABLE 
> ```
>
> 查询这样的记录.
>
> 加上MACheader之后,MAC报文就生成了,这就是向下封装的过程.

6.网卡处理

> 我们会把这个数据包放在网卡的缓存中,网卡会加上一个起始帧的分节符,最后加上FCS进行校验.
>
> 把**数字序列转换为电信号**,然后发送出去.

7.Switch 交换机

> 把网线处收到的电信号转换成数字信号,然后根据地址表进行转发.
>
> 注意:Switch的端口是没有mac地址的.

8.Router 路由器

> 这是三层的,工作在IP层上面.
>
> 路由器的端口有MAC也有IP地址.
>
> ​	路由器收到这个数据包之后,先检查MAC是否匹配,不匹配就直接丢弃,匹配了就先去掉MAC,然后根据路由表来重置MAC地址.在整个过程中,MAC的地址会不断更改,为了实现两点之间的发送.
>
>  NAT网络有可能会导致IP的变化,子网穿透这样的技术.

9.到达,**层层协议进行解封装并且检查是否匹配,最终浏览器会进行渲染的操作**.









