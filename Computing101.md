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











































