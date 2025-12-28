# Cross Site Scripting

[python -m http.server](https://docs.python.org/3/library/http.server.html)

[JavaScript Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch)

> ​	XSS:跨站脚本攻击.本次是Stored XSS的挑战(存储型XSS),我们将自己的代码注入Server,受害者是其余的用户.

| **类型**      | **触发方式**                       | **持久性**     | **典型案例**           |
| :------------ | :--------------------------------- | :------------- | :--------------------- |
| **存储型XSS** | 恶意代码存储在服务器（如论坛评论） | 长期有效       | 窃取访问用户的Cookie   |
| **反射型XSS** | 恶意代码通过URL参数传递            | 单次生效       | 钓鱼攻击中的伪造登录页 |
| **DOM型XSS**  | 前端JavaScript动态生成恶意内容     | 依赖客户端环境 | 篡改页面DOM元素        |

注意使用py的"""三引号,注入多行html代码.