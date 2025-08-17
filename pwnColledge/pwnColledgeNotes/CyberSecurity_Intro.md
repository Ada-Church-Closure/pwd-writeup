# Intro to Cybersecurity.

> ​	Can you feel it? The sun is beginning to rise on your journey of cybersecurity. Armed with the fundamentals, you begin to push ever deeper into the realms of knowledge that previously eluded you. **Fear not: with perseverance, grit, and gumption, you will lay the groundwork for a towering mastery of security in your future.**

## Web Security

> ​	Web content is served up via the internet by *web servers*, and like everything else, these web servers, and the pages that they serve up, contain **vulnerabilities**! In this module, you will wrap yourself in the mysteries of the web, exploring various types of vulnerabilities that can occur. As you work through this module, keep in mind, **these aren't theoretical curiosities: these are common, critical vulnerabilities that occur *all the time* in the modern web** and can lead to massive data breaches, account takeover, and more.

### Injection

代码注入的问题.什么是代码注入,have a glance,bro......

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

#### Path Traversal

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

我们从一个子目录下面开始向上遍历.







































































































































