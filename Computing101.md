# Computing101

> 计算机基础。



## Assembly

我已经学习过学校课程的Intel格式和CSAPP中的AT&T格式，有一些基础，简单过一下。

```sh
mov rax, 42
syscall
```

系统调用,中断向量表。

设置exit code reg rdi中。

**理解整个过程，从.s文件到可执行目标文件，接着是链接。**

```console
.intel_syntax noprefix
```

前缀声明语法类型。

**as**生成object file,01二进制码。

```sh
as -o asm.o asm.s
```



**ld**进行🔗（**link editor**）

关于linking，我们在linkerLab中讨论过，但是不深刻。

```sh
ld -o exe asm.o
```

生成可执行文件。



设置label，对于linker是可见的：

```asm
.intel_syntax noprefix
.global _start
_start:
mov rdi, 42
mov rax, 60
syscall
```



简单的mov指令 寻址方法等

## Software Introspection

软件内省

关于trace gdb debug的基本使用

strace跟踪函数的调用栈，参数，返回值。

```sh
(gdb) starti
```

从第一条指令开始执行。

## Computer Memory

关于缓存机制，我们在cacheLab中已经有了较为深入的理解。

关于**指针，多重指针，引用，解引用,数组**的实质问题,理解汇编的机制就很简单。

各种寻址的方式。

理解地址关系就很简单。

## System Calls

中断向量表调用系统函数。

write system call number is 1.

**文件描述符：**

- **FD 0:** Standard *Input* is the channel  through which the process takes input. For example, your shell uses  Standard Input to read the commands that you input.（进程接受输入，也就是read函数）

- **FD 1:** Standard *Output* is the channel  through which processes output normal data, such as the flag when it is  printed to you in previous challenges or the output of utilities such as `ls`.（进程输出，也就是写入）

- **FD 2:** Standard *Error* is the channel  through which processes output error details. For example, if you  mistype a command, the shell will output, over standard error, that this command does not exist.（错误状态）

  PS：对于**write** **call**，第一个参数是FD，比如是写入标准输入，设置为1,ERROR，那就设置成2.

```
write(file_descriptor, memory_address, number_of_characters_to_write)
call：write(1, 1337000, 10);
```

调用：设置参数和向量号即可。

链式调用，那么只要再更改再加即可。

简单的调用：

```asm
.intel_syntax noprefix
.global _start
_start:
mov rdi, 0
mov rsi, 1337000
mov rdx, 8
mov rax, 0
syscall
mov rdi, 1
mov rsi, 1337000
mov rdx, 8
mov rax, 1
syscall
mov rdi, 42
mov rax, 60
syscall
```

## Assembly Level Up

> 接下来我们更进一步的熟悉汇编语言，为之后的学习打基础。

要生成ELF文件进行进一步的测试。

就是一些关于寄存器的基本操作。

**div除法指令**



- ```asm
  rax = rdx:rax / reg
  rdx = remainder
  ```

  rdx为高64bit，rax为低64bit，这样的处理方式。**最后rdx存放余数。**

​	

```plein text
MSB                                    LSB
+----------------------------------------+
|                   rax                  |
+--------------------+-------------------+
                     |        eax        |
                     +---------+---------+
                               |   ax    |
                               +----+----+
                               | ah | al |
                               +----+----+
```







独立访问一个reg的不同位置的bit。



那么如果被除数是2的某些幂，那么可以直接对于寄存器进行某种操作。



位运算。



shl shr

bit逻辑运算。



```
and`, `or`, `not`, `xor
```



汇编位运算技巧，有一种boomlab的感觉。



关于内存的操作。

加法 **显示指定操作数**的大小：

```asm
add qword ptr [0x404000], 0x1337
```





little endian

低位在低字节，高位在高字节。



立即数超过32bit写入内存就需要寄存器的参与,先把大常数放到reg中，接着写入内存。



stack栈内存的基本操作。



接着是函数调用，段间跳转的问题，我们在bomlab中讨论的很详细了。

absolute jump 直接跳转到寄存器内的某个地址。



相对跳转 nop占位 用

```asm
.rept 0x51
nop
.endr
```





生成重复汇编。

conditional jump条件跳转的问题，根据ZF标志位。

实现switch语句，跳转表,条件number最好小并且连续。



怎么实现循环？简单计算一个数组的平均值，注意要给rdx置0,不然会认为被除数是由rdx:rax组成的128bit的数字。

```asm
.intel_syntax noprefix
.global _start
_start:
xor rax, rax
mov rdx, 0x0
loop:
cmp rdx, rsi
jae endcase
add rax, [rdi + 8 * rdx]
inc rdx
jmp loop
endcase:
xor rdx, rdx
div rsi
```



| 64位  | 32位   | 16位   | 8位低  | 8位高  | 用途（传统）               |
| ----- | ------ | ------ | ------ | ------ | -------------------------- |
| `RAX` | `EAX`  | `AX`   | `AL`   | `AH`   | 累加器 Accumulator         |
| `RBX` | `EBX`  | `BX`   | `BL`   | `BH`   | 基址寄存器 Base            |
| `RCX` | `ECX`  | `CX`   | `CL`   | `CH`   | 计数器 Counter             |
| `RDX` | `EDX`  | `DX`   | `DL`   | `DH`   | 数据寄存器 Data            |
| `RSI` | `ESI`  | `SI`   | `SIL`  | *(无)* | 源索引 Source Index        |
| `RDI` | `EDI`  | `DI`   | `DIL`  | *(无)* | 目的索引 Destination Index |
| `RBP` | `EBP`  | `BP`   | `BPL`  | *(无)* | 栈基址 Base Pointer        |
| `RSP` | `ESP`  | `SP`   | `SPL`  | *(无)* | 栈指针 Stack Pointer       |
| `R8`  | `R8D`  | `R8W`  | `R8B`  | *(无)* | 扩展寄存器                 |
| `R9`  | `R9D`  | `R9W`  | `R9B`  | *(无)* | 扩展寄存器                 |
| `R10` | `R10D` | `R10W` | `R10B` | *(无)* | 扩展寄存器                 |
| `R11` | `R11D` | `R11W` | `R11B` | *(无)* | 扩展寄存器                 |
| `R12` | `R12D` | `R12W` | `R12B` | *(无)* | 扩展寄存器                 |
| `R13` | `R13D` | `R13W` | `R13B` | *(无)* | 扩展寄存器                 |
| `R14` | `R14D` | `R14W` | `R14B` | *(无)* | 扩展寄存器                 |
| `R15` | `R15D` | `R15W` | `R15B` | *(无)* | 扩展寄存器                 |



统计连续的不为0的个数：

```asm
.intel_syntax noprefix
.global _start
_start:
xor rax, rax
cmp rdi, 0
je endcase
loop:
mov bl, [rdi + rax]
cmp bl, 0
je endcase
inc rax
jmp loop
endcase:
```

注意比较的操作数大小的问题：

| 名称           | 缩写    | 位数 | 示例         |
| -------------- | ------- | ---- | ------------ |
| 字节           | `byte`  | 8    | `al`, `bl`   |
| 字（字长）     | `word`  | 16   | `ax`, `bx`   |
| 双字           | `dword` | 32   | `eax`, `ebx` |
| 四字（四倍字） | `qword` | 64   | `rax`, `rbx` |



```asm
cmp byte ptr [rdi], 0x5a
```





关于函数的调用的问题，第一次稍微写长一点的asm，有点组织不好：

```asm
.intel_syntax noprefix
.global _start
str_lower:
xor rdx, rdx
cmp rdi, 0x0
je str_lower_end
loop_1:
cmp [rdi], 0x00
je str_lower_end
cmp [rdi], 0x5a
ja endcase
call_foo:
push rdi
mov rax, 0x403000
mov rdi, [rdi]
call rax
pop rdi
mov [rdi], rax
inc rdx
endcase:
inc rdi
jmp loop_1
str_lower_end:
mov rax, rdx
ret
_start:
call str_lower
```



注意传递多个参数的寄存器顺序：

| 参数编号 | 寄存器 | 说明       |
| -------- | ------ | ---------- |
| 第1个    | `rdi`  | 第一个参数 |
| 第2个    | `rsi`  | 第二个参数 |
| 第3个    | `rdx`  | 第三个参数 |
| 第4个    | `rcx`  | 第四个参数 |
| 第5个    | `r8`   | 第五个参数 |
| 第6个    | `r9`   | 第六个参数 |



我们接下来继续研究带栈帧（处理callee的局部变量）的函数的用法：

**rbp**作为当前的**base**，临时栈帧的基址。

接下来是一个统计数量的题目：



```asm
.intel_syntax noprefix
.global _start

# most_common_byte(rdi=src_addr, rsi=size)
most_common_byte:
    # 函数栈帧设置
    push rbp
    mov rbp, rsp
    sub rsp, 512          # 分配 256 * 2 bytes 空间作为统计数组

    xor rdx, rdx          # rdx = i = 0
count_loop:
    cmp rdx, rsi
    jge count_done

    mov al, byte ptr [rdi + rdx]    # al = src[i]
    movzx rcx, al                   # rcx = unsigned(src[i])
    shl rcx, 1                      # rcx *= 2 （计算偏移）
	
	neg rcx
    movzx bx, word ptr [rbp + rcx]  # bx = counter[src[i]]
    neg rcx
    inc bx
    neg rcx
    mov word ptr [rbp + rcx], bx    # counter[src[i]] += 1
	neg rcx
    inc rdx
    jmp count_loop

count_done:
    mov rdx, 1              # b = 1
    xor ecx, ecx            # max_freq = 0
    xor eax, eax            # max_freq_byte = 0

find_loop:
    cmp rdx, 0x100
    ja  find_done

    mov r8, rdx
    shl r8, 1                       # r8 = b * 2
    neg r8
    movzx bx, word ptr [rbp + r8]  # bx = counter[b]
	neg r8
    cmp bx, cx
    jle skip_update

    mov cx, bx             # max_freq = counter[b]
    mov al, dl             # max_freq_byte = b

skip_update:
    inc rdx
    jmp find_loop

find_done:
    mov rsp, rbp           # 恢复栈空间
    pop rbp
    ret

# 测试入口
_start:
    # 此处省略调用设置和 syscall，你可根据平台自行编写测试
    call most_common_byte
```





> > [!NOTE]
> >
> > ​	可能是因为语法还是什么之类的原因，这里的寻址**不能使用**"-"减号来获取栈帧中的地址，要先使用neg取反接着"+"来做实现。

## Debugging Refresher

```sh
$host scp -i key -r hacker@pwn.college:/challenge
```



我们用上面的指令拿在本地做比较流畅。

接下来是我们关于debugger的一些简单的使用，在bomblab中也是提到过很多次了。

其实这层实验也算是一个bomblab之类的东西。



```
run

continue

info registers

p $rdi

p/x $rdi

x/<n><u><f> <address> // 检查内存的内容

x/8i $rip			  // 可以带格式，检查8条指令

break

finish

nexti

stepi

display				 // 一直监控某个变量
```



搞清楚read()调用时候的参数：**ssize_t read(int fd, void *buf, size_t count);**



那么调用的时候地址就存放在 $rsi寄存器内部。

​	**level4**:正确的使用display和断点之类的工具就可以解决，循环不断的在栈上一个相同的位置去设置一些值的大小，这个之后就稍微有一些难度了。

​	gdb脚本的编写和使用：

比如写一个脚本：x.gdb	用这个参数启动：-x <PATH_TO_SCRIPT>

~/.gdbinit---》**就是一个初始化的脚本**，可以写入一些通用的操作

一个脚本的例子：

```gdb
start // 开始程序run，并且在main函数的位置设置断点
break *main+42
commands
  silent
  set $local_variable = *(unsigned long long*)($rbp-0x32)
  printf "Current value: %llx\n", $local_variable
  continue
end
continue
```

​	注意：这里的command就是对于断点进行编程---》当在*main + 42的位置停止的时候，我们会进行从command-end之间的所有指令的操作。silent清除常规输出，打印变量的值。

​	注意两个continue的区别。

​	那么比如针对level4,我们编写一个脚本。

```gdb
set $address = 0

break *main + 704
commands
    silent
    set $address = $rsi
    continue
end

break *main + 757
commands
    silent
    printf "Value at $rsi = 0x%llx\n", *(unsigned long long*)$address
    continue
end

run
continue
```

这就是我们写的脚本，简单来说debug的效率会上升。就是实现了一个自动化的流程一样。

​	**level5**:关于set关键字

​	You can modify the state of your target program with the `set` command. For example, you can use `set $rdi = 0` to zero out $rdi. You can use `set *((uint64_t *) $rsp) = 0x1234` to set the first value on the stack to 0x1234. You can use `set *((uint16_t *) 0x31337000) = 0x1337` to set 2 bytes at 0x31337000 to 0x1337.

​	这里我们的任务还是一样，但是我们要实现和这个程序交互的纯自动化，比如它让我们连续输入1000个stack上面改变的值，我们就不能手动复制粘贴了。

注意我们是怎么跳过scanf函数的：

```asm
set $address = 0

break *main + 570
commands
    silent
    set $address = $rsi
    continue
end

break *main + 620
commands
    silent
    if($address != 0)
        set *(unsigned long long*)$rsi = *(unsigned long long*)$address
    
    set $rip = *main + 630
    continue
end

start
```

scanf函数只是从键盘读入value,并且把这个值给rsi地址的位置，我们提前设置好，然后直接跳过即可。

直接在原来的**asm**中观察，直接跳过scanf函数，更改rip指针的值即可。

> 这样就完全实现了自动化。

gdb call命令：

相当于直接手动调用某个函数。

比如调试的程序中有：

```C
int add(int a, int b) {
    return a + b;
}
```

那么我们可以在CLI中直接这样调用：（**这是很夸张也很强大的地方**）

```sh
(gdb) break main
(gdb) run
(gdb) call add(3, 4)
$1 = 7
```

​	其实在上面的一系列测试中，我们可以直接调用call (void)win()来解决问题。

​	在最后，我们可以直接使用jump命令来修改程序的执行的流程。我们直接跳过一段会造成段错误的代码然后直接执行即可。

> ​	到这里，gdb的简单入门就结束了。这是一个非常强大的工具。

## Building a Web Server

> 一个简单的服务器到底是怎样的？这里主要就是基础，进程是怎么通信的，还有fork函数等等。
>
> 可能会用的上下面这个syscall table：（我们使用这样的方式和硬件进行交互）

https://x64.syscall.sh/

1.打开一个文件的过程，FD---》文件描述符。

2.首先我们理解**退出的逻辑**：

rdi---》error code表示退出时候的状态。

rax---》进行系统的调用。

3.接下来是socket，系统调用创建一个server的socket。

```c
int socket(int domain, int type, int protocol);
```

1.指定使用的协议族，常用的有：

- `AF_INET`：IPv4（常用）
- `AF_INET6`：IPv6
- `AF_UNIX` / `AF_LOCAL`：本地 UNIX 域套接字

对于 Web 服务器监听 IPv4，通常用 `AF_INET`。



2.指定套接字的类型，常见的有：

- `SOCK_STREAM`：流式套接字（TCP）
- `SOCK_DGRAM`：数据报套接字（UDP）

3.protocol 一般设置为 0，表示让内核根据前两个参数自动选择合适的协议：

- 对于 `AF_INET + SOCK_STREAM`，自动选择 **TCP**。
- 对于 `AF_INET + SOCK_DGRAM`，自动选择 **UDP**。

同时我们注意一下这里的宏：



| 名称          | 值   | 说明                                  |
| ------------- | ---- | ------------------------------------- |
| `AF_INET`     | 2    | IPv4 地址族                           |
| `SOCK_STREAM` | 1    | TCP 流式套接字                        |
| `sys_socket`  | 41   | Linux x86-64 中的 socket syscall 编号 |
| `sys_exit`    | 60   | 退出 syscall 编号                     |

3.接着是利用bind函数进行绑定的操作，把刚刚创建的socket和我们的本地IP地址和端口号进行绑定。

​	我们要创建结构体，选定参数，本地端口和IP地址。

4.listen函数，进行监听，和客户端进行连接的操作。

5.当出现连接的时候，我们就应该使用accept函数。

​	这是我们接受的方式。后面两个参数直接传NULL，说明server不关心Client的地址，简单测试来使用。

```c
int accept(int sockfd, struct sockaddr *addr, socklen_t *addrlen);
```



6.wirte发送，linux中普通文件和socket是类似的，只要确定FD即可。

write操作之后还要close()。

​	我们要先read，读取客户端的请求，注意传参数的时候，地址的传递，lea rdi, [request]。---》注意传送数据地址的方法

listen--->accept--->read--->write--->close...

在open的时候，如果请求的是文件。提取文件路径很麻烦。---》比较复杂

```
O_WRONLY` (只写模式)，值为 `1
O_RDWR` (读写模式)，值为 `2
O_CREAT` (创建文件)，值为 `64
O_APPEND` (追加写模式)，值为 `8
```

> ​	值得注意的一点的是FD的生命周期，我们要及时备份。不仅是FD,还有read字节的返回值大小，我们要对于某些系统调用的返回值用r12, r13之类的寄存器保存起来。

| 用途                        | 推荐寄存器            |
| --------------------------- | --------------------- |
| 保存 socket FD（server）    | `rbx`                 |
| 保存 client 的 FD（accept） | `r12`                 |
| 保存打开的文件 FD           | `r13`                 |
| 临时 syscall 返回值         | `rax`（立即用后丢弃） |



tips：

定义数据的类型：

| 指令    | 定义的数据类型      | 示例                     | 含义              |
| ------- | ------------------- | ------------------------ | ----------------- |
| `.byte` | 8-bit（1字节）整数  | `.byte 42`               | 存一个字节 `0x2a` |
| `.word` | 16-bit（2字节）整数 | `.word 12345`            | 小端序：0x39 0x30 |
| `.long` | 32-bit（4字节）整数 | `.long 123456`           |                   |
| `.quad` | 64-bit（8字节）整数 | `.quad 1234567890123456` |                   |



这些定义的数据的操作数的写法：

| 数据类型（声明） | 占用大小 | 对应操作指令      | 示例                           | 含义                    |
| ---------------- | -------- | ----------------- | ------------------------------ | ----------------------- |
| `.byte`          | 1 字节   | `byte ptr [...]`  | `mov al, byte ptr [my_byte]`   | 从地址读取1字节到 `al`  |
| `.word`          | 2 字节   | `word ptr [...]`  | `mov ax, word ptr [my_word]`   | 从地址读取2字节到 `ax`  |
| `.long`          | 4 字节   | `dword ptr [...]` | `mov eax, dword ptr [my_long]` | 从地址读取4字节到 `eax` |
| `.quad`          | 8 字节   | `qword ptr [...]` | `mov rax, qword ptr [my_quad]` | 从地址读取8字节到 `rax` |



> ​	接下来就是加一个循环在listen---(loop)---accept---read----write---close---(loop)---exit，这样一个持久化的过程，一个客户端可以一直连接并且请求资源。
>

那么接下来我们要去实现一个并发的服务器：

```
+-----------------------------+
| Parent (main process)       |
|                             |
|  socket → bind → listen     |
|        ┌──── accept ───┐    |
|        ↓               ↓    |
|   fork()              wait? |
|        ↓                    |
|  Child process              |
|   → read → open → write     |
|   → close → exit            |
+-----------------------------+
```



这是一个基本架构，就是使用fork函数来实现并发的功能。

### Server.s

> ​	这样就实现了一个可以并发处理请求的服务器，一旦请求资源完毕之后就直接close,不保持连接。
>
> ​	注意fork（）函数是实现并发的关键，fork了之后，父进程不应当持有accept的客户端socket,应当close（），同理子进程不应当持有listen的socket,应当close（），都是要注意的地方。

```asm
.intel_syntax noprefix
.global _start

.section .data # 提前准备write的参数
response: .ascii "HTTP/1.0 200 OK\r\n\r\n"
resp_len = $ - response
request_file_len: .long 0
server_socket_number: .quad 0

.section .text
.lcomm request, 1024
.lcomm file_path 1024
.lcomm file_buffer 1024

_start:
mov rdi, 2  # 创建一个server的socket
mov rsi, 1
mov rdx, 0
mov rax, 0x29
syscall

mov qword ptr [server_socket_number], rax # socket的文件描述符号先放到rbx中
sub rsp, 16  # 创建struct sockaddr_in结构体的内容
mov word ptr [rsp], 0x0002
mov word ptr [rsp + 2], 0x5000
mov dword ptr [rsp + 4], 0x00000000
mov qword ptr [rsp + 8], 0

mov rdi, qword ptr [server_socket_number]    # socket的文件描述符号
lea rsi, [rsp]  # 结构体的地址
mov rdx, 16     # 结构体的大小
mov rax, 49     # 调用bind()
syscall

mov rdi, qword ptr [server_socket_number]    # listen，监听客户端
mov rsi, 0
mov rax, 50
syscall

.ans_req_loop:
mov rdi, qword ptr [server_socket_number]    # accpet，和发起请求的客户端连接,返回值是新建立的socket的FD
xor rsi, rsi
xor rdx, rdx
mov rax, 43
syscall
mov rbx, rax    # put the client fd into rbx

mov rax, 57
syscall
cmp rax, 0
je .cocurrent_loop

mov rdi, rbx
mov rax, 3
syscall
jmp .ans_req_loop


.cocurrent_loop:
mov rdi, [server_socket_number]
mov rax, 3
syscall

mov rdi, rbx    # read request content from client
lea rsi, [request]
mov rdx, 1024
mov rax, 0
syscall

lea rsi, [request]       
lea rdi, [file_path]     

.skip_spaces:
    mov al, byte ptr [rsi]
    cmp al, ' '             
    je .found_path
    inc rsi
    jmp .skip_spaces

.found_path:
    inc rsi                  

.copy_loop:
    mov al, byte ptr [rsi]
    cmp al, ' '
    je .done_copy
    cmp al, 13              
    je .done_copy
    mov byte ptr [rdi], al
    inc dword ptr [request_file_len]
    inc rsi
    inc rdi
    jmp .copy_loop

.done_copy:
    mov byte ptr [rdi], 0  


lea rdi, [file_path] 
mov rsi, 0
mov rdx, 0
mov rax, 2
syscall
mov r12, rax

mov rdi, rax       # 读取请求的文件到某个缓冲区内部, read
lea rsi, [file_buffer]
mov rdx, request_file_len
mov rax, 0
syscall
mov r13, rax

mov rdi, r12       # close，关闭关于这个文件的读取
mov rax, 3
syscall


mov rdi, rbx    # write，向客户端写入数据,OK回应
mov rsi, offset response
mov rdx, resp_len
mov rax, 1
syscall

mov rdi, rbx
lea rsi, [file_buffer] # write,把请求文件文件中的内容写给socket
mov rdx, r13
mov rax, 1
syscall

mov rdi, rbx # 关闭socket
mov rax, 3
syscall

mov rdi, 0  
mov rax, 60
syscall
```

实现POST请求的Server：

> ​	Expanding your server’s capabilities further, this challenge focuses on handling HTTP POST requests concurrently. POST requests are more complex because they include both headers and a message body. You will once again use [fork](https://man7.org/linux/man-pages/man2/fork.2.html) to manage multiple connections, while using [read](https://man7.org/linux/man-pages/man2/read.2.html) to capture the entire request. Again, you will parse the URL path to determine the specified file, but  this time instead of reading from that file, you will instead write to  it with the incoming POST data. In order to do so, you must determine the length of the incoming POST  data. The *obvious* way to do this is to parse the `Content-Length` header, which specifies exactly that. Alternatively, consider using the return value of [read](https://man7.org/linux/man-pages/man2/read.2.html) to determine the total length of the request, parsing the request to find the total length of the headers (which end with `\r\n\r\n`), and using that difference to determine the length of the body--this  seemingly more complicated algorithm may actually be easier to  implement. Finally, return just a `200 OK` response to the client to indicate that the POST request was successful.

有消息头和消息体。

向请求的文件写入内容。---》这就是所谓的表单提交的过程。

我要确定写入文件的长度，然后把这部分进行写入的操作。

> 注意的几个地方。

open打开的方式，因为我们是要写入一个文件：

```asm
mov rdi, file_path      ; 文件路径
mov rsi, 0x41           ; O_WRONLY | O_CREAT
mov rdx, 0o777          ; 权限模式（注意是八进制）
mov rax, 2              ; syscall: open
syscall
```

我们会读取这样的一个post请求：

```txt
read(4, "POST /tmp/tmpw0a5724c HTTP/1.1\r\nHost: localhost\r\nUser-Agent: python-requests/2.32.4\r\nAccept-Encoding: gzip, deflate, zstd\r\nAccept: */*\r\nConnection: keep-alive\r\nContent-Length: 228\r\n\r\n8jYMDjUSyPfG0mL4hhqLvJ8Ea1mvNrMRrja2H7qJH4Qt6l2L4BJHPaccAelllIFCYZjmsq022woekR9SO9TOT62roaelSox6mwLgomzPSHyyUbV8w1YEM1KIpPLNH8qcCmsWvZ65LYLqDUYkeZDFJsPg5MjvDrEuLcnWkjoykJdJTzTrh5OZpwq63tKezYLjQTURwxhXhI9RgTka81VGywnCLVuWojwgZFwW", 1024) = 411
```

消息头就是在\r\n\r\n之前的部分，我们要把之后的部分写入对应的文件路径。

> ​	那么这就是我们实现的postServer,能够处理post请求。

```asm
.intel_syntax noprefix
.global _start

.section .data # 提前准备write的参数
response: .ascii "HTTP/1.0 200 OK\r\n\r\n"
resp_len = $ - response
server_socket_number: .quad 0
body_length: .quad 0
body_offset: .quad 0
request_length: .quad 0


.section .text
.lcomm request, 1024
.lcomm file_path 1024
.lcomm file_buffer 1024
.lcomm request_body 1024

_start:
mov rdi, 2  # 创建一个server的socket
mov rsi, 1
mov rdx, 0
mov rax, 0x29
syscall

mov qword ptr [server_socket_number], rax # socket的文件描述符号先放到rbx中
sub rsp, 16  # 创建struct sockaddr_in结构体的内容
mov word ptr [rsp], 0x0002
mov word ptr [rsp + 2], 0x5000
mov dword ptr [rsp + 4], 0x00000000
mov qword ptr [rsp + 8], 0

mov rdi, qword ptr [server_socket_number]    # socket的文件描述符号
lea rsi, [rsp]  # 结构体的地址
mov rdx, 16     # 结构体的大小
mov rax, 49     # 调用bind()
syscall

mov rdi, qword ptr [server_socket_number]    # listen，监听客户端
mov rsi, 0
mov rax, 50
syscall

.ans_req_loop:
mov rdi, qword ptr [server_socket_number]    # accpet，和发起请求的客户端连接,返回值是新建立的socket的FD
xor rsi, rsi
xor rdx, rdx
mov rax, 43
syscall
mov rbx, rax    # put the client fd into rbx

mov rax, 57
syscall
cmp rax, 0
je .cocurrent_loop

mov rdi, rbx
mov rax, 3
syscall
jmp .ans_req_loop


.cocurrent_loop:
mov rdi, [server_socket_number]
mov rax, 3
syscall

mov rdi, rbx    # read request content from client
lea rsi, [request]
mov rdx, 1024
mov rax, 0
syscall
mov qword ptr [request_length], rax

# extract the file path from the content,the content is in the request
.extract_path:
lea rsi, [request]       
lea rdi, [file_path]     

.skip_spaces:
    mov al, byte ptr [rsi]
    cmp al, ' '             
    je .found_path
    inc rsi
    jmp .skip_spaces

.found_path:
    inc rsi                  

.copy_loop:
    mov al, byte ptr [rsi]
    cmp al, ' '
    je .done_copy
    cmp al, 13              
    je .done_copy
    mov byte ptr [rdi], al
    inc rsi
    inc rdi
    jmp .copy_loop

.done_copy:
    mov byte ptr [rdi], 0


# extract the body of the request into the `[request_body]` and calculate the length
# use rdi to storage the offset of the body in the request
.extract_body:
lea rsi, [request]
mov rdi, 0

.skip_header:
mov al, byte ptr [rsi]
cmp al, 0x0d
je .next_check
inc rsi
inc rdi
jmp .skip_header


.next_check:
inc rsi
inc rsi
inc rdi
inc rdi
mov al, byte ptr [rsi]
cmp al, 0x0d           
je .done_find_body
jmp .skip_header


.done_find_body:
inc rdi
inc rdi
mov qword ptr [body_offset], rdi
mov rdi, [request_length]
mov rsi, qword ptr [body_offset]
sub rdi, rsi
mov qword ptr [body_length], rdi


# 已经拿到了文件的路径，现在更改open的模式,open returns the fd of the file
lea rdi, [file_path]
mov rsi, 0x41  
mov rdx, 0x1ff
mov rax, 2
syscall
mov r12, rax


# 现在我们要尝试向这个文件写入client提交的内容
mov rdi, rax
lea rsi, [request]
mov rax, qword ptr [body_offset]
add rsi, rax
mov rdx, [body_length]
mov rax, 1
syscall


mov rdi, r12       # after written,close the file
mov rax, 3
syscall


mov rdi, rbx    # write，向客户端写入数据,OK回应,表示我们已经写完了
mov rsi, offset response
mov rdx, resp_len
mov rax, 1
syscall


mov rdi, rbx # 关闭socket
mov rax, 3
syscall

mov rdi, 0  
mov rax, 60
syscall
```

### 成品Server

> ​	我们最终要实现的server要满足post和get请求都可以处理，并且满足并发的要求。

```asm
.intel_syntax noprefix
.global _start

.section .data # 提前准备write的参数
response: .ascii "HTTP/1.0 200 OK\r\n\r\n"
resp_len = $ - response
server_socket_number: .quad 0
body_length: .quad 0
body_offset: .quad 0
request_length: .quad 0
is_post: .quad 0


.section .text
.lcomm request, 1024
.lcomm file_path 1024
.lcomm file_buffer 1024
.lcomm request_body 1024

_start:
mov rdi, 2  # 创建一个server的socket
mov rsi, 1
mov rdx, 0
mov rax, 0x29
syscall

mov qword ptr [server_socket_number], rax # socket的文件描述符号先放到rbx中
sub rsp, 16  # 创建struct sockaddr_in结构体的内容
mov word ptr [rsp], 0x0002
mov word ptr [rsp + 2], 0x5000
mov dword ptr [rsp + 4], 0x00000000
mov qword ptr [rsp + 8], 0

mov rdi, qword ptr [server_socket_number]    # socket的文件描述符号
lea rsi, [rsp]  # 结构体的地址
mov rdx, 16     # 结构体的大小
mov rax, 49     # 调用bind()
syscall

mov rdi, qword ptr [server_socket_number]    # listen，监听客户端
mov rsi, 0
mov rax, 50
syscall

.ans_req_loop:
mov rdi, qword ptr [server_socket_number]    # accpet，和发起请求的客户端连接,返回值是新建立的socket的FD
xor rsi, rsi
xor rdx, rdx
mov rax, 43
syscall
mov rbx, rax    # put the client fd into rbx

mov rax, 57
syscall
cmp rax, 0
je .cocurrent_loop

mov rdi, rbx
mov rax, 3
syscall
jmp .ans_req_loop


.cocurrent_loop:
mov rdi, [server_socket_number]
mov rax, 3
syscall

mov rdi, rbx    # read request content from client
lea rsi, [request]
mov rdx, 1024
mov rax, 0
syscall
mov qword ptr [request_length], rax

# extract the file path from the content,the content is in the request
.extract_path:
lea rsi, [request]       
lea rdi, [file_path]    
mov rdx, 0 

.skip_spaces:
    mov al, byte ptr [rsi]
    cmp al, ' '             
    je .found_path
    inc rsi
    inc rdx
    jmp .skip_spaces

.found_path:
    mov qword ptr [is_post], rdx
    inc rsi         

.copy_loop:
    mov al, byte ptr [rsi]
    cmp al, ' '
    je .done_copy
    cmp al, 13              
    je .done_copy
    mov byte ptr [rdi], al
    inc rsi
    inc rdi
    jmp .copy_loop

.done_copy:
    mov byte ptr [rdi], 0


mov rsi, is_post
cmp rsi, 3
je .handle_get_request


.handle_post_request:
# extract the body of the request into the `[request_body]` and calculate the length
# use rdi to storage the offset of the body in the request
.extract_body:
lea rsi, [request]
mov rdi, 0

.skip_header:
mov al, byte ptr [rsi]
cmp al, 0x0d
je .next_check
inc rsi
inc rdi
jmp .skip_header


.next_check:
inc rsi
inc rsi
inc rdi
inc rdi
mov al, byte ptr [rsi]
cmp al, 0x0d           
je .done_find_body
jmp .skip_header


.done_find_body:
inc rdi
inc rdi
mov qword ptr [body_offset], rdi
mov rdi, [request_length]
mov rsi, qword ptr [body_offset]
sub rdi, rsi
mov qword ptr [body_length], rdi


# 已经拿到了文件的路径，现在更改open的模式,open returns the fd of the file
lea rdi, [file_path]
mov rsi, 0x41  
mov rdx, 0x1ff
mov rax, 2
syscall
mov r12, rax


# 现在我们要尝试向这个文件写入client提交的内容
mov rdi, rax
lea rsi, [request]
mov rax, qword ptr [body_offset]
add rsi, rax
mov rdx, [body_length]
mov rax, 1
syscall


mov rdi, r12       # after written,close the file
mov rax, 3
syscall


mov rdi, rbx    # write，向客户端写入数据,OK回应,表示我们已经写完了
mov rsi, offset response
mov rdx, resp_len
mov rax, 1
syscall


jmp .over



.handle_get_request:

lea rdi, [file_path] 
mov rsi, 0
mov rdx, 0
mov rax, 2
syscall
mov r12, rax

mov rdi, rax       # 读取请求的文件到某个缓冲区内部, read
lea rsi, [file_buffer]
mov rdx, 1024
mov rax, 0
syscall
mov r13, rax       # 这里要先保存read到的字节数，用来确定最后要写入的字节数大小

mov rdi, r12       # close，关闭关于这个文件的读取
mov rax, 3
syscall


mov rdi, rbx    # write，向客户端写入数据,OK回应
mov rsi, offset response
mov rdx, resp_len
mov rax, 1
syscall

mov rdi, rbx
lea rsi, [file_buffer] # write,把请求文件文件中的内容写给socket
mov rdx, r13
mov rax, 1
syscall


.over:
mov rdi, rbx # 关闭socket
mov rax, 3
syscall

mov rdi, 0  # 退出这个程序
mov rax, 60
syscall
```

​	比较GET和POST的逻辑写的比较草率，但是我们一般的服务器也就会使用这两种http verb，并且注意我在汇编中经常犯的错误，没有搞清楚偏移量，内存地址，和数据的本身的值应该怎么进行操作的逻辑导致了一些错误。

​																		           2025.7.18

































































































