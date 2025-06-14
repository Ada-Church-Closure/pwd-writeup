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



接下来是我们关于debugger的一些简单的使用，在bomblab中也是提到过很多次了。





































































