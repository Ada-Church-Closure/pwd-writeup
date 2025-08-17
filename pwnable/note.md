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





























































