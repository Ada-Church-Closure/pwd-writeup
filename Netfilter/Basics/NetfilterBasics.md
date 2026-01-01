# Netfilter Basics

## 执行过程

做基本编译:

```sh
$ gcc -o simple_hook simple_hook.c -lnetfilter_queue 
```

设置基本规则:

```sh
$ sudo iptables -F OUTPUT 
$ sudo iptables -A OUTPUT -p tcp -d 1.1.1.1 -j NFQUEUE --queue-num 0
```

运行,我们监听一个端口:

> 这个数据包就**被捕获**了.

```sh
$ sudo ./simple_hook
Opening......
>>>NF got the packted ID: 1, the packet Size: 60 bytes
>>>NF got the packted ID: 2, the packet Size: 52 bytes
>>>NF got the packted ID: 3, the packet Size: 123 bytes
>>>NF got the packted ID: 4, the packet Size: 52 bytes
>>>NF got the packted ID: 5, the packet Size: 52 bytes
>>>NF got the packted ID: 6, the packet Size: 52 bytes
```

同时在另一边我们调用curl:

```sh
$ curl 1.1.1.1
<html>
<head><title>301 Moved Permanently</title></head>
<body>
<center><h1>301 Moved Permanently</h1></center>
<hr><center>cloudflare</center>
</body>
</html>
```

## 网络数据包流向

#### 5 个关键的 Hooks

> 当一个数据包进入或离开你的电脑时，它会按照特定的路径流动。请看下面这张图，它展示了这 5 个点的位置：

我们来拆解一下这 5 个点：

1. **PREROUTING (路由前)**：
   - 数据包刚从网卡进来，还没决定要去哪里。
   - *比喻：* 邮件刚进传达室，还没看收件人是谁。
2. **LOCAL_IN (本地输入)**：
   - 经过路由判断，发现这个包是**发给我自己**的（比如发给你的 Web 服务器）。
   - *比喻：* 确认是给我的信，送到我的办公桌上。
3. **FORWARD (转发)**：
   - 经过路由判断，发现这个包**不是给我的**，而是要我帮忙传给别人的（通常用于路由器）。
   - *比喻：* 发现是给隔壁部门的信，我只是个中转站。
4. **LOCAL_OUT (本地输出)**：
   - **我自己**产生的包，准备发出去（比如你运行 `curl google.com`）。
   - *比喻：* 我写好了一封信，刚贴好邮票，准备投递。
5. **POSTROUTING (路由后)**：
   - 所有准备离开网卡的包（无论是转发的还是自己发的），在真正离开前的最后一站。
   - *比喻：* 信件装车，驶离大楼。

## 基本API

**`nfq_open()`**:

- **作用**：初始化库。
- **比喻**：去相关部门注册，拿到“上岗证”。

**`nfq_create_queue(h, 0, &cb, NULL)`**:

- **作用**：创建队列句柄，绑定回调函数。
- **关键参数**：`0` 是队列号（对应 iptables）；`&cb` 是你的处理函数地址。
- **比喻**：打开 0 号窗口，告诉领导：“有事叫 `cb` 这个人来处理”。

**`nfq_set_mode(qh, NFQNL_COPY_PACKET, ...)`**:

- **作用**：决定你拿到的数据类型。
- **重点**：默认情况下内核为了省事，只给你发 Packet ID（元数据）。如果你想看 HTTP Header，**必须**调这个函数设置为 `COPY_PACKET`。

**`nfq_set_verdict(qh, id, verdict, ...)`**:

- **作用**：这是你必须调用的最后一个函数。
- **重点**：只要不调用这个，那个包就会一直卡在内核里（你的 curl 就会转圈圈直到超时）。
- **Verdict 类型**：
  - `NF_ACCEPT`: 放行。
  - `NF_DROP`: 丢弃。
  - `NF_QUEUE`: 扔给下一个队列。

> ​	我手写并且成功运行了,但是我对于一些核心概念还是不理解,1.h和qh都是什么2.他是怎么和scoket配合工作的,网络数据包流向是什么3.源代码中没有库函数的解释,如果想要查找这些函数的作用,在哪里看?
>
> ​	对于底层系统编程，**“看不懂变量类型”**和**“不知道数据怎么流转”**是再正常不过的疑惑。因为 `libnetfilter_queue` 本质上是在和 Linux 内核通信，它用了很多抽象的概念。
>
> 让我们把你笔记本上的这三个问题彻底拆解清楚。
>
> ------
>
> ### 问题 1：`h` 和 `qh` 到底是什么？
>
> 这两个是 C 语言编程中典型的**“句柄 (Handle)”**设计模式。
>
> 你可以把它们想象成**层级关系**：
>
> 1. **`h` (`struct nfq_handle\*`) —— 全局联络员**
>    - **定义**：它是你的程序和 Linux 内核 Netfilter 子系统建立的**总连接**。
>    - **作用**：当你调用 `nfq_open()` 时，内核给你发了这个句柄。它代表“我要开始使用 Netfilter 功能了”。
>    - **对应现实**：就像你拿着身份证去**自来水总公司**开了个户。`h` 就是你的**用户号**。如果不拿到这个 `h`，你连门都进不去。
> 2. **`qh` (`struct nfq_q_handle\*`) —— 特定管道管理员**
>    - **定义**：它是依托于 `h` 创建的、专门负责某一个**队列 (Queue)** 的句柄。
>    - **作用**：因为 `iptables` 允许创建多个队列（**Queue 0**, Queue 1, Queue 2...），你需要**告诉内核你现在要管理哪一个**。
>    - **对应现实**：你在自来水公司开户后，申请接通了**0号水管**。`qh` 就是**0号水管的阀门钥匙**。你用 `qh` 来设置这根具体水管的流速（`set_mode`）或者放行/关断（`set_verdict`）。
>
> **总结关系**：
>
> - 先有 `h` (总户头)，才能有 `qh` (具体业务)。
> - 一个程序可以有一个 `h`，但可以通过 `h` 创建多个 `qh` (比如同时处理 Queue 0 和 Queue 1)。
>
> ------
>
> ### 问题 2：它怎么和 Socket 配合？数据包流向是什么？
>
> 这是最容易混淆的地方。你一定要把**“你的程序的 Socket”**和**“curl 的 Socket”**区分开。
>
> #### 1. 两个不同的 Socket
>
> - **业务 Socket (curl)**：这是 `curl` 程序创建的 TCP Socket，用来发 HTTP 请求。它只管发数据，根本不知道你的程序存在。
> - **Netlink Socket (你的程序)**：
>   - 在你的代码里：`fd = nfq_fd(h);`
>   - 这个 `fd` 也是一个 Socket，但它不是用来发 HTTP 的，它是**Netlink 协议**的 Socket。
>   - **作用**：它是一条**“专线”**。内核把截获的数据包（元数据+Payload）打包，通过这个 `fd` 发给你的用户态程序；你做完决定后，把 Verdict（放行/丢弃）通过这个 `fd` 发回给内核。
>
> #### 2. 数据包的完整流向 (The Big Picture)
>
> 请在笔记上画下这个流程图，这非常重要：
>
> **场景：curl 1.1.1.1**
>
> 1. User Space (curl): 用户输入命令，curl 创建 TCP Socket，写入数据 "GET / HTTP/1.1"。
>
>    ↓ (系统调用 send/write)
>
> 2. Kernel Space (协议栈): 内核将数据封装成 TCP Segment -> IP Packet。
>
>    ↓ (准备发送到网卡)
>
> 3. **Netfilter (OUTPUT Hook)**: 包到达关卡。`iptables` 发现规则：`DST=1.1.1.1 -> NFQUEUE num 0`。
>
>    - 动作：内核把这个包“扣留”下来，打个包（Netlink Message），放入 Queue 0。
>
>      ↓ (通过 Netlink Socket)
>
> 4. **User Space (你的 simple_hook)**:
>
>    - 你的 `recv(fd, ...)` 醒了，收到了这个消息。
>
>    - 你的 `callback` 函数被调用。
>
>    - 你执行 nfq_set_verdict(..., NF_ACCEPT)。
>
>      ↓ (通过 Netlink Socket 发回指令)
>
> 5. **Kernel Space (Netfilter)**:
>
>    - 内核收到 `NF_ACCEPT` 指令。
>
>    - 把刚才扣留的包“释放”。
>
>      ↓ (继续流动)
>
> 6. **Hardware (网卡)**: 包被转换成电信号/光信号，发到互联网。
>
> **结论**：你的程序是一个**“旁路监工”**。你并不直接持有 `curl` 的 Socket，你是通过 Netlink 这个“对讲机”告诉内核该怎么处理那个包。
>
> ------
>
> ### 问题 3：去哪里查库函数的解释？
>
> Linux 系统编程最硬核的地方在于：通常没有漂亮的网页文档，**Man Page (手册页)** 和 **头文件** 就是最权威的文档。
>
> #### 方法 1：使用 `man` 命令 (最推荐)
>
> Linux 系统自带了说明书。在终端输入：
>
> Bash
>
> ```sh
> # 查看库的概览
> man 3 libnetfilter_queue
> 
> # 有些函数可能有单独的页面，或者都在主页面里
> ```
>
> *注意：如果不灵，可能需要安装文档包 `man-pages` 或 `libnetfilter-queue-doc`。*
>
> #### 方法 2：直接看头文件 (最硬核、最准确)
>
> 源代码中没解释，但**头文件**里通常有函数原型和简短注释。对于 C 语言库，头文件就是契约。
>
> 请在你的 Arch Linux 上直接查看：
>
> Bash
>
> ```sh
> # 这是最核心的头文件
> vim /usr/include/libnetfilter_queue/libnetfilter_queue.h
> ```
>
> 你会看到类似这样的定义：
>
> C
>
> ```C
> /* nfq_set_verdict - issue a verdict on a packet 
>  * qh: netfilter queue handle
>  * id: ID assigned to packet by the kernel
>  * verdict: NF_ACCEPT, NF_DROP, etc.
>  */
> int nfq_set_verdict(struct nfq_q_handle *qh, u_int32_t id, u_int32_t verdict, u_int32_t data_len, const unsigned char *buf);
> ```
>
> 这是最原本的真相。
>
> #### 方法 3：在线文档 (Doxygen)
>
> 如果你还没习惯看黑底白字的 man page，可以看 Netfilter 官网生成的在线文档：
>
> - 搜索关键词：`libnetfilter_queue documentation`
> - 地址：https://www.netfilter.org/projects/libnetfilter_queue/doxygen/html/
>
> 我的建议：
>
> 先尝试用 man 命令，这是 Linux 程序员的基本功。如果看不懂，再去搜在线文档。
>
> ------
>
> ### 给你的小作业 (验证理解)
>
> 为了确认你懂了“流向”，请回答这个问题：
>
> 如果在你的代码里，`callback` 函数里写了一个死循环 `while(1);`，当你运行程序并执行 `curl` 时：
>
> 1. `curl` 会立即报错吗？还是会卡住？
>
>    > 不会报错,但是会卡住,因为我的内核收不到来自netfilter的处理信息
>
> 2. 你的 `callback` 会被调用第二次吗？
>
>    > 应该不行?要按顺序处理--->单线程.
>
> 3. 这说明内核的 Queue 是同步的还是异步的？
>
>    > 应该只有一个Queue,同步的,是的,就是一个一个处理.





















