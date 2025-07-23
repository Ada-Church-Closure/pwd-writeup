> 在思考之前先行动.

# Playing With Programs.

> ​	一些安全中非常基本的话题。

## 编码

> ​	很基本的话题，就是关于encoding这块，我们再来熟悉一下。
>
> ​	编码，是编成一个二进制码，解码，是解成可读的字符。

echo + “|” 作为程序的输入。

```sh
echo "Hello, World!" | ./your_program
echo "Hello, World!" > input.txt
./your_program < input.txt
```

取消自动补上一个换行符：

```sh
echo -n "yourpassword" | /challenge/runme
```

没有权限创建文件但是还需要程序从某个文件读取？

```sh
$ echo -n bvwmigzr > /tmp/mypass	# 首先创建一个临时的文件
$ exec 3</tmp/mypass 				# 为这个临时的文件分配一个fd
$ ln -s /proc/self/fd/3 byyf		# 创建符号链接，也就是一个“伪文件”，实际指向的是mypass
$ /challenge/runme 					# 这个程序会读取我们的伪造文件
Read 8 bytes.
Congrats! Here is your flag:
```

> ​	Now, life must get complex. You may have noticed the `b` letters in front of the password constants throughout this module. Python has two types of string-like constants: `str`ings (specified as `"asdf"`) and `bytes` (specified as `b"asdf"`). Let's talk about `bytes` in this level.
>

**Fun facts:** Some other Pythonisms that might be useful:

- If you `print(n)` a number or convert it to a string with `str(n)`, the number will be represented in base 10.
- You can get a hexadecimal string representation of a number using `hex(n)`.
- You can get a binary string representation of a number using `bin(n)`.
- Converting a string to a number with `int(s)` will read it as a base 10 number by default.
- You can specify a different base to use with a second argument: `int(s, 16)` will interpret the string as hex, `int(s, 2)` will interpret it as binary.
- You can try to auto-identify the number base using `int(s, 0)`, which requires a prefex on the string (`0b` or binary, `0x` for hex, nothing for decimal).

decode()是把字节转换为str的方法。

```py
>>> bytes.fromhex("68656c6c6f")
b'hello'

>>> bytes.fromhex("68 65 6c 6c 6f")
b'hello'
```

区分好这两个概念：

| 项目         | 字符串 (`str`)    | 字节串 (`bytes`)              |
| ------------ | ----------------- | ----------------------------- |
| 表示内容     | 人类可读字符      | 原始字节（二进制）            |
| 示例         | `"abc"`           | `b"abc"` 或 `b"\x61\x62\x63"` |
| 转换为 bytes | `"abc".encode()`  | 本身就是 `bytes` 类型         |
| 转换为 str   | `b"abc".decode()` | 解码得到 `"abc"`              |

### ASCII表

> ​	Values *above* `0x80` ("extended ASCII") were used by different countries for their own characters, leading to some chaos due to colliding byte values. In the US, the typical "extended ASCII" encoding was called [Latin 1](https://en.wikipedia.org/wiki/ISO/IEC_8859-1), and it defined a character for each of the 256 possible byte values. This is useful for us because we can use "latin1" to easily convert between Python's bytes and strings, including: `b"\x80".decode("latin1")`.

| Dec  | Hex  | Char | 说明               |
| ---- | ---- | ---- | ------------------ |
| 0    | 0x00 | NUL  | 空字符（Null）     |
| 1    | 0x01 | SOH  | 标题开始           |
| 2    | 0x02 | STX  | 正文开始           |
| 3    | 0x03 | ETX  | 正文结束           |
| 4    | 0x04 | EOT  | 传输结束           |
| 5    | 0x05 | ENQ  | 请求               |
| 6    | 0x06 | ACK  | 收到通知           |
| 7    | 0x07 | BEL  | 响铃（Beep）       |
| 8    | 0x08 | BS   | 退格               |
| 9    | 0x09 | TAB  | 水平制表符（Tab）  |
| 10   | 0x0A | LF   | 换行               |
| 11   | 0x0B | VT   | 垂直制表符         |
| 12   | 0x0C | FF   | 换页               |
| 13   | 0x0D | CR   | 回车               |
| 14   | 0x0E | SO   | Shift Out          |
| 15   | 0x0F | SI   | Shift In           |
| 16   | 0x10 | DLE  | 数据链路转义       |
| 17   | 0x11 | DC1  | 设备控制 1         |
| 18   | 0x12 | DC2  | 设备控制 2         |
| 19   | 0x13 | DC3  | 设备控制 3         |
| 20   | 0x14 | DC4  | 设备控制 4         |
| 21   | 0x15 | NAK  | 否定应答           |
| 22   | 0x16 | SYN  | 同步               |
| 23   | 0x17 | ETB  | 传输块结束         |
| 24   | 0x18 | CAN  | 取消               |
| 25   | 0x19 | EM   | 介质结束           |
| 26   | 0x1A | SUB  | 替换               |
| 27   | 0x1B | ESC  | 转义               |
| 28   | 0x1C | FS   | 文件分隔符         |
| 29   | 0x1D | GS   | 组分隔符           |
| 30   | 0x1E | RS   | 记录分隔符         |
| 31   | 0x1F | US   | 单元分隔符         |
| 32   | 0x20 |      | 空格               |
| 33   | 0x21 | !    | 感叹号             |
| 34   | 0x22 | "    | 双引号             |
| 35   | 0x23 | #    | 井号               |
| 36   | 0x24 | $    | 美元符号           |
| 37   | 0x25 | %    | 百分号             |
| 38   | 0x26 | &    | 和号               |
| 39   | 0x27 | '    | 单引号             |
| 40   | 0x28 | (    | 左括号             |
| 41   | 0x29 | )    | 右括号             |
| 42   | 0x2A | *    | 星号               |
| 43   | 0x2B | +    | 加号               |
| 44   | 0x2C | ,    | 逗号               |
| 45   | 0x2D | -    | 减号               |
| 46   | 0x2E | .    | 点号               |
| 47   | 0x2F | /    | 斜杠               |
| 48   | 0x30 | 0    | 数字 0             |
| 49   | 0x31 | 1    | 数字 1             |
| 50   | 0x32 | 2    | 数字 2             |
| 51   | 0x33 | 3    | 数字 3             |
| 52   | 0x34 | 4    | 数字 4             |
| 53   | 0x35 | 5    | 数字 5             |
| 54   | 0x36 | 6    | 数字 6             |
| 55   | 0x37 | 7    | 数字 7             |
| 56   | 0x38 | 8    | 数字 8             |
| 57   | 0x39 | 9    | 数字 9             |
| 58   | 0x3A | :    | 冒号               |
| 59   | 0x3B | ;    | 分号               |
| 60   | 0x3C | <    | 小于号             |
| 61   | 0x3D | =    | 等号               |
| 62   | 0x3E | >    | 大于号             |
| 63   | 0x3F | ?    | 问号               |
| 64   | 0x40 | @    | 邮箱符号           |
| 65   | 0x41 | A    | 大写 A             |
| 66   | 0x42 | B    | 大写 B             |
| 67   | 0x43 | C    | 大写 C             |
| 68   | 0x44 | D    | 大写 D             |
| 69   | 0x45 | E    | 大写 E             |
| 70   | 0x46 | F    | 大写 F             |
| 71   | 0x47 | G    | 大写 G             |
| 72   | 0x48 | H    | 大写 H             |
| 73   | 0x49 | I    | 大写 I             |
| 74   | 0x4A | J    | 大写 J             |
| 75   | 0x4B | K    | 大写 K             |
| 76   | 0x4C | L    | 大写 L             |
| 77   | 0x4D | M    | 大写 M             |
| 78   | 0x4E | N    | 大写 N             |
| 79   | 0x4F | O    | 大写 O             |
| 80   | 0x50 | P    | 大写 P             |
| 81   | 0x51 | Q    | 大写 Q             |
| 82   | 0x52 | R    | 大写 R             |
| 83   | 0x53 | S    | 大写 S             |
| 84   | 0x54 | T    | 大写 T             |
| 85   | 0x55 | U    | 大写 U             |
| 86   | 0x56 | V    | 大写 V             |
| 87   | 0x57 | W    | 大写 W             |
| 88   | 0x58 | X    | 大写 X             |
| 89   | 0x59 | Y    | 大写 Y             |
| 90   | 0x5A | Z    | 大写 Z             |
| 91   | 0x5B | [    | 左中括号           |
| 92   | 0x5C | \    | 反斜杠             |
| 93   | 0x5D | ]    | 右中括号           |
| 94   | 0x5E | ^    | 脱字符             |
| 95   | 0x5F | _    | 下划线             |
| 96   | 0x60 | `    | 重音符             |
| 97   | 0x61 | a    | 小写 a             |
| 98   | 0x62 | b    | 小写 b             |
| 99   | 0x63 | c    | 小写 c             |
| 100  | 0x64 | d    | 小写 d             |
| 101  | 0x65 | e    | 小写 e             |
| 102  | 0x66 | f    | 小写 f             |
| 103  | 0x67 | g    | 小写 g             |
| 104  | 0x68 | h    | 小写 h             |
| 105  | 0x69 | i    | 小写 i             |
| 106  | 0x6A | j    | 小写 j             |
| 107  | 0x6B | k    | 小写 k             |
| 108  | 0x6C | l    | 小写 l             |
| 109  | 0x6D | m    | 小写 m             |
| 110  | 0x6E | n    | 小写 n             |
| 111  | 0x6F | o    | 小写 o             |
| 112  | 0x70 | p    | 小写 p             |
| 113  | 0x71 | q    | 小写 q             |
| 114  | 0x72 | r    | 小写 r             |
| 115  | 0x73 | s    | 小写 s             |
| 116  | 0x74 | t    | 小写 t             |
| 117  | 0x75 | u    | 小写 u             |
| 118  | 0x76 | v    | 小写 v             |
| 119  | 0x77 | w    | 小写 w             |
| 120  | 0x78 | x    | 小写 x             |
| 121  | 0x79 | y    | 小写 y             |
| 122  | 0x7A | z    | 小写 z             |
| 123  | 0x7B | {    | 左大括号           |
| 124  | 0x7C | \|   | 竖线               |
| 125  | 0x7D | }    | 右大括号           |
| 126  | 0x7E | ~    | 波浪线             |
| 127  | 0x7F | DEL  | 删除字符（Delete） |

在实际中我们用pwntools来和程序进行交互，就是使用py脚本。

### 理解

> ​	比如这样的一个程序，实际上我们是要来构造一个字节流的输入，然后程序会把我们的字节流解码成二进制的字符串，接着进行比较的操作。

```py
import sys


def decode_from_bits(s):
    s = s.decode("latin1")
    assert set(s) <= {"0", "1"}, "non-binary characters found in bitstream!"
    assert len(s) % 8 == 0, "must enter data in complete bytes (each byte is 8 bits)"
    return int.to_bytes(int(s, 2), length=len(s) // 8, byteorder="big")


print("Enter the password:")
entered_password = sys.stdin.buffer.read1()
correct_password = b"1001110011101000110101011000110011010011110110111111101110111010"

print(f"Read {len(entered_password)} bytes.")


correct_password = decode_from_bits(correct_password)


if entered_password == correct_password:
    print("Congrats! Here is your flag:")
    print(open("/flag").read().strip())
else:
    print("Incorrect!")
    sys.exit(1)
```

实际上就是：

```py
import pwn
p = pwn.process("/challenge/runme")
p.write(b"\x9c\xe8\xd5\x8c\xd3\xdb\xfb\xba")
result = p.readall()
print(result)
```

UTF-8编码，宽字符的编码问题。--->最全最完整的，并且也是国际通用的。

解码不一致而造成的路径穿越的攻击：

**UTF-8**：

- 与 ASCII 向后兼容（前 128 个字符编码一致）
- 是网页、JSON、URL 编码等主流格式的默认编码

**UTF-16**：

- 不兼容 ASCII，文件开头通常需要 **BOM（Byte Order Mark）** 来标识字节序（大端或小端）
- 在 Java 和 Windows 内部常用

```py
# 伪代码
user_input = get_input_from_user()  # 攻击者输入: %2e%2e/%2e%2e/secret.txt
decoded_once = urllib.parse.unquote(user_input)  # 第一次 decode：变成 ../..../secret.txt
if ".." in decoded_once:
    reject_request()
else:
    file_path = os.path.join(base_dir, urllib.parse.unquote(user_input))
    open(file_path)  # 第二次 decode（再次解码 %2e%2e → ..），打开敏感文件

```

> ​	涉及到UTF-16的之类的编码，我们不要手写，而要用py来生成，这也是最好和最快的选择,也是作为一个程序员的选择。
>

```py
with open("input03.txt", "wb") as f:
    f.write("pfjwmfxa".encode("utf-16"))
```

当我们能更改编码时，可能会出现错误：

```console
hacker@dojo:~$ ipython
In [1]: "🎈".encode("utf-8")
Out[1]: b'\xf0\x9f\x8e\x88'
```

​	If we mess with the resulting bytes, and then decode them, we would (of course) get something different:

```console
In [2]: b'\xf0\x9f\x8e\xaa'.decode("utf-8")
Out[2]: '🎪'

In [3]: b'\xf0\x9f\x8e\x42'.decode("utf-8")
---------------------------------------------------------------------------
UnicodeDecodeError                        Traceback (most recent call last)
Cell In[3], line 1
----> 1 b'\xf0\x9f\x8e\x42'.decode("utf-8")

UnicodeDecodeError: 'utf-8' codec can't decode bytes in position 0-2: invalid continuation byte
```

那么就可以更改来达到攻击的效果。

接下来是**base64**编码的问题：

​	The name "base64" comes from the fact that there are 64 characters used in each output character. These can actually vary, but the standard base64 encoding uses an "alphabet" of the uppercase letters `A` through `Z`, the lowercase letters `a` through `z`, the digits `0` through `9`, and the `+` and `/` symbols. This results in 64 total output symbols, and each symbol can encode `2**6` (2 to the power of 6) possible input symbols, or 6 bits of data. That means that to encode a single byte (8 bits) of input, you need more than one base64 output character. In fact, you need *two*: one that encodes the first 6 bits and one that encodes the remaining 2 (with 4 bits of that second output character being unused). To mark these unused bits, base64 encoded data appends an `=` for every two unused bits. For example:

```sh
hacker@dojo:~$ echo -n A | base64
QQ==
hacker@dojo:~$ echo -n AA | base64
QUE=
hacker@dojo:~$ echo -n AAA | base64
QUFB
hacker@dojo:~$ echo -n AAAA | base64
QUFBQQ==
```

​	Base64 编码将 **每三个字节（3×8=24位）** 的数据，分为 **四组（4×6=24位）**，每组6位，用下面这个 **64个字符的表** 来映射编码：

```txt
A–Z => 0–25  
a–z => 26–51  
0–9 => 52–61  
+    => 62  
/    => 63
```

​	注意，有人可能会混淆，编码肯定不能提升安全性，可能有一点混淆视听的效果，当我们尝试去攻击的时候：

> ​	"Good Artists Copy, Great Artists Steal!" When you're doing security analysis and need to interact with bespoke software, ripping the implementations of custom communication protocols out of that software is a good way to reach interoperability. 

仔细阅读互操作和通信部分的代码。

> ​	关于编码，就是类似于等式两边变形最终相同一样，当加密手段没有被加入的时候，其实是不复杂的，但是要理解编码的原理以及目的的问题。

## web基础

### 关于GET和POST以及基本网络工具和py库

发起Http GET请求



Flask,小型web框架,py





page source查看网页的源代码



**metadata**元数据,是用来描述数据的数据

请求元数据:



| Header 字段       | 说明                                                 |
| ----------------- | ---------------------------------------------------- |
| `Host`            | 目标主机域名或IP（必须字段）                         |
| `User-Agent`      | 客户端信息（如浏览器类型）                           |
| `Accept`          | 支持的内容类型（如 `text/html`、`application/json`） |
| `Accept-Encoding` | 支持的压缩格式（如 `gzip`, `deflate`）               |
| `Cookie`          | 携带的用户 cookie                                    |
| `Authorization`   | 身份验证信息（如 token）                             |
| `Content-Length`  | 请求体的长度（用于 POST）                            |
| `Content-Type`    | 请求体的类型（如 `application/json`）                |

相应元数据:

| Header 字段                   | 说明                                                 |
| ----------------------------- | ---------------------------------------------------- |
| `Content-Type`                | 响应内容的类型（如 `text/html`, `application/json`） |
| `Content-Length`              | 内容的字节数                                         |
| `Set-Cookie`                  | 设置客户端 cookie                                    |
| `Server`                      | 服务器信息（如 nginx, Apache）                       |
| `Cache-Control`               | 缓存策略                                             |
| `Content-Encoding`            | 响应内容的压缩格式                                   |
| `Access-Control-Allow-Origin` | CORS 跨域设置                                        |
| `Date`                        | 响应发送时间                                         |



**netcat**命令:可以实现很多有意思的功能.

```sh
nc [选项] 主机名/IP地址 端口
```

比如:

```sh
$ nc 127.0.0.1 80 # 监听本地的80端口
```

**http**的**request**格式:

```http
GET /path/to/resource HTTP/1.1
Host: example.com
User-Agent: curl/7.68.0
Accept: */*

<空行>
```



curl命令:http中进行数据的传输.

发送一个GET请求.

```sh
curl http://example.com
```

比如访问一个本地的Sever:

```sh
curl -X GET http://localhost:80/validate
```



发起一个POST请求.

```sh
curl -X POST -d "username=admin&password=1234" http://example.com/login
```



添加请求header.

```sh
curl -H "Authorization: Bearer TOKEN123" http://example.com/api
```



文件upload.

```sh
curl -F "file=@example.txt" http://example.com/upload
```



比如要分析或者逆向一个httpServer.

| 选项            | 说明                                      |
| --------------- | ----------------------------------------- |
| `-X`            | 指定请求方法（如 GET、POST、PUT、DELETE） |
| `-d`            | 发送表单数据（POST 请求）                 |
| `-H`            | 自定义请求头                              |
| `-F`            | 表单上传文件（multipart/form-data）       |
| `-o`            | 将输出写入文件                            |
| `-L`            | 跟随重定向（如 301、302）                 |
| `-v`            | 输出详细过程                              |
| `--data-binary` | 发送原始数据流                            |



我们的测试利器:https://requests.readthedocs.io/en/latest/

> 不是哥们,这真有点帅了......

![Requests logo](https://requests.readthedocs.io/en/latest/_static/requests-sidebar.png)

主要利用request library,就是脚本.

​	现代的大型服务器通常会托管多个网站,而用户的host请求头指名了我们要请求哪些网站的资源,有很多虚拟主机,而host确定了我们要访问的是哪个虚拟主机.

​	我们手动设置headers.

```py
import requests
url = 'http://localhost:80/qualify'
headers = {
    "Host":"cryptohack.org:80"
}
r = requests.get(url, headers = headers)
print(r.text)
```

用curl请求:

```sh
curl -H "Host:www.google.com" http://localhost:80/man
```

用nc手动构造一次请求.

> ​	我们要了解其中的细节问题.

```sh
$ curl -v -H "Host:flaws.cloud:80" http://localhost:80/check
* Host localhost:80 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:80...
* connect to ::1 port 80 from ::1 port 33860 failed: Connection refused
*   Trying 127.0.0.1:80...
* Connected to localhost (127.0.0.1) port 80
* using HTTP/1.x
> GET /check HTTP/1.1
> Host:flaws.cloud:80
> User-Agent: curl/8.12.1
> Accept: */*
> 
* Request completely sent off
< HTTP/1.1 400 BAD REQUEST
< Server: Werkzeug/3.0.6 Python/3.8.10
< Date: Tue, 22 Jul 2025 04:24:06 GMT
< Content-Type: text/html; charset=utf-8
< Content-Length: 149
< Connection: close
< 
<!doctype html>
<html lang=en>
<title>400 Bad Request</title>
<h1>Bad Request</h1>
<p>You are using an incorrect client to access this resource!</p>
* shutting down connection #0
```



​	Any tricky characters (such as spaces) are simply hex-encoded, with a `%` plopped in front of them. Of course, because `%` thus becomes a tricky character in itself, it must also be encoded. In the above example, `/solve my challenge` would become `/solve%20my%20challenge`, as the hex value of the ASCII space character is `0x20`.

​	如果url的路径出现了空格,那么就有问题.空格直接解析成%20.



请求中可以携带参数,比如:

```sh
http://example.com/search?keyword=cat&limit=5
```

中的`keyword=cat&limit=5`就是Query String.

客户端不可信,就会造成安全问题:

> ​	It's tempting to think of HTTP parameters as similar to parameters to a function call. However, keep in mind: when you're writing C or Python or Java code, an attacker (typically) can't just call random functions in your program with random parameters. But with HTTP, they *can*. They can just make HTTP requests wherever they want! This has caused quite a few security issues...

传递参数:

```py
import requests
url = 'http://localhost:80/authenticate'
headers = {
    "Host":"challenge.localhost:80"
}
payload = {
    "security":"ekddfknb"
}
r = requests.get(url, headers = headers, params=payload)
print(r.text)
```

怎么传递多参数?

```txt
/authenticate?secure_key=jcbaywzw&private_key=miwgzszt&access_code=buadbiky
```

在使用 `curl` 指定多个 HTTP 参数（即多个 GET 参数）时，需要特别小心，因为：

- 在 **shell（终端）中**，`&` 有特殊含义 —— 它不是普通字符，而是用来将命令放到后台执行的。

- 所以如果你在命令行中直接写：

- ```sh
  curl http://localhost:80/authenticate?secure_key=xxx&private_key=yyy&access_code=zzz
  ```

  shell 会把 `&private_key=yyy` 和 `&access_code=zzz` 当作新的命令（甚至后台运行的命令），**不是 URL 的一部分**。

如果有多个参数,使用了&,那么一定要加上'',注意这里的细节.

http form表单的提交POST

浏览器直接提交,比如注册信息之类的

那么curl命令:

```sh
$ curl -H "Host:challenge.localhost:80" -X POST "http://localhost:80/meet" -d "access=ugchpjev"
```

那么实际的请求是:

A form using `application/x-www-form-urlencoded` content encoding (the default) sends a request where the body contains the form data in `key=value` pairs, with each pair separated by an `&` symbol, as shown below:

> ​	用nc提交表单是比较麻烦的,要把header和body之间分开才可以.并且注意多个参数的处理方法,长度也必须经过计算才可以得到.

```http
POST /test HTTP/1.1
Host: example.com
Content-Type: application/x-www-form-urlencoded
Content-Length: 27

field1=value1&field2=value2
```



用py来做处理:

```py
import requests
url = 'http://localhost:80/hack'
payload = {
    "challenge_key":"bxfzwxzl"
}
headers = {
    "Host":"challenge.localhost:80"
}
r = requests.post(url, headers = headers, data = payload)
print(r.text)
```



或者写html,直接构造表单,然后用浏览器打开:

```html
<!DOCTYPE html>
<html>
  <body>
    <form action="http://challenge.localhost/gate" method="POST">
      <input type="hidden" name="credential" value="cejsemob">
      <input type="submit" value="Submit">
    </form>
  </body>
</html>
```

那么就是小练习,提交多参数的表单:

```sh
curl -v -X POST "http://localhost:80/hack" \
  -H "Host: challenge.localhost:80" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=ieovmiim&authcode=dhcrcdvp&access=cbmupwsi"
```

> ​	注意中间的空格,没有空格就和在一起了,会出错.
>

### Redirects(重定位)

> ​	也是一个有趣并且常见的话题.
>

原理就是:

```py
@app.route("/", methods=["GET"])
def challenge_redirector():
    if name_of_program_for(peer_process_of(flask.request.input_stream.fileno())) not in ["nc"]:
        flask.abort(400, "You are using an incorrect client to access this resource!")

    return flask.redirect(f"/{secret_endpoint}-gateway")


@app.route(f"/{secret_endpoint}-gateway", methods=["GET"])
def challenge():
    if name_of_program_for(peer_process_of(flask.request.input_stream.fileno())) not in ["nc"]:
        flask.abort(400, "You are using an incorrect client to access this resource!")

    return f"""
        <html>
          <head><title>Talking Web</title></head>
        <body>
          <h1>Great job!</h1>
          <p>{open("/flag").read().strip()}</p>
        </body>
        </html>
    """
```

很简单,当我们请求的时候返回了一个新的URL.

接着我们使用curl命令,-L能够直**接跟随重定向**来处理,非常方便,分析过程:

```sh
$ curl -v -L -H "Host:challenge.localhost:80" "http://localhost:80/"
* Host localhost:80 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:80...
* connect to ::1 port 80 from ::1 port 53122 failed: Connection refused
*   Trying 127.0.0.1:80...
* Connected to localhost (127.0.0.1) port 80
* using HTTP/1.x
> GET / HTTP/1.1
> Host:challenge.localhost:80
> User-Agent: curl/8.12.1
> Accept: */*
> 
* Request completely sent off
< HTTP/1.1 302 FOUND
< Server: Werkzeug/3.0.6 Python/3.8.10
< Date: Tue, 22 Jul 2025 15:59:38 GMT
< Content-Type: text/html; charset=utf-8
< Content-Length: 221
< Location: /xOmLEvDu-qualify
< Connection: close
< 
* shutting down connection #0
* Issue another request to this URL: 'http://localhost:80/xOmLEvDu-qualify'
* Hostname localhost was found in DNS cache
*   Trying [::1]:80...
* connect to ::1 port 80 from ::1 port 53124 failed: Connection refused
*   Trying 127.0.0.1:80...
* Connected to localhost (127.0.0.1) port 80
* using HTTP/1.x
> GET /xOmLEvDu-qualify HTTP/1.1
> Host:challenge.localhost:80
> User-Agent: curl/8.12.1
> Accept: */*
> 
* Request completely sent off
< HTTP/1.1 200 OK
< Server: Werkzeug/3.0.6 Python/3.8.10
< Date: Tue, 22 Jul 2025 15:59:38 GMT
< Content-Type: text/html; charset=utf-8
< Content-Length: 224
< Connection: close
< 

        <html>
          <head><title>Talking Web</title></head>
        <body>
          <h1>Great job!</h1>
          <p>pwn.college{M4e0fOBc4jlqEJbSN9HQ48rt0u1.QX4kjMzwyM2gjMyEzW}</p>
        </body>
        </html>
* shutting down connection #1
```

py的requests库也可以直接跟随重定向.

### Cookies

> ​	服务器保持无状态,防止占用过多的资源.

Server给Client分配一段cookie,之后交互的过程中会自动携带上这一段cookie:

1.用户登录认证（如登录状态维持）

2.会话管理（如购物车、语言偏好）

3.跟踪用户行为（广告/分析）

比如:

```txt
* Mark bundle as not supporting multiuse
< HTTP/1.1 302 FOUND
< Server: Werkzeug/3.0.6 Python/3.8.10
< Date: Wed, 23 Jul 2025 02:29:04 GMT
< Content-Length: 189
< Location: /
< Set-Cookie: cookie=3c7d6a2b6b7ebc20bc71c719e8cac768; Path=/
< Server: pwn.college
< Connection: close
```

Server给我们分配了一段cookie.

当我们使用curl带cookie去requests的时候:

```sh
$ curl -v -L -b "3c7d6a2b6b7ebc20bc71c719e8cac768" "http://localhost:80/"
```

如果使用nc带cookie的话:

```sh
$ nc 127.0.0.1 80
GET / HTTP/1.1
Host: 127.0.0.1
Cookie: cookie=fc8ec903d4b8a0e0b30fb5ade530809b
```

有状态的Server保存上下文,requests应该会自动处理.

一个开始时最简单的Server:

```py
#!/opt/pwn.college/python

import flask
import os

app = flask.Flask(__name__)


@app.route("/", methods=["GET"])
def challenge():
    if "Firefox" not in flask.request.headers.get("User-Agent"):
        flask.abort(400, "You are using an incorrect client to access this resource!")

    return f"""
        <html>
          <head><title>Talking Web</title></head>
        <body>
          <h1>Great job!</h1>
          <p>{open("/flag").read().strip()}</p>
        </body>
        </html>
    """


app.secret_key = os.urandom(8)
app.run("challenge.localhost", 80)
```

我们打起来一个Server监听客户端的连接.

那么怎么自己给客户端做重定向.

> ​	不会就找文档.

https://flask.palletsprojects.com/en/stable/

注意不要把域名指定错误了,妈的.

### JavaScript

用JS加上我们上面写的重定向的代码来搞浏览器的重定向:

```html
<html>
    <head><title>GOGOGO</title></head>
    <body>
        <script>
            window.location = "http://localhost:1337/"
        </script>
    </body>
</html>
```

> ​	Exfiltration is the art of smuggling sensitive data out right under the nose of its owners: in this case, /challenge/client and /challenge/server.
>
> ​	这是比较复杂的.



























































































