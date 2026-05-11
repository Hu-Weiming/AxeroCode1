# Step 26 - Minimal Web UI

## 这一步做了什么

Step 26 给 AxerCode 加上了第一版浏览器聊天页。

这一步没有上任何前端框架，而是直接把页面挂在 `axercode-server` 的静态资源目录下：

- `index.html`
- `assets/styles.css`
- `assets/app.js`

页面目标很明确：

- 尽量安静
- 尽量白
- 窄栏阅读感
- 不做后台式控制台 UI

## 为什么这样设计

你希望它像纳瓦尔博客那种“经典简约、白色为主”的感觉，所以我避免了：

- 深色主题
- 强烈渐变
- 侧边栏式应用框架
- 花哨动效

而是用了：

- 白底
- 窄栏
- serif 主体排版
- 很轻的边框和灰阶
- 像长文页面一样的留白

从架构上，这一步也刻意保持最小：

- 不引入 React / Vue
- 不增加前端构建链
- 直接复用 Step 25 已经打通的 `/api/chat/stream`

这样最适合当前阶段快速验证“Web UI 是否有必要继续放大”。

## 关键实现

### 1. 静态页面

新增：

- `index.html`

页面包含：

- 标题区
- 一段很简短的说明
- 聊天 transcript
- 一个 textarea + send 按钮

### 2. 样式

新增：

- `assets/styles.css`

设计方向是：

- 以白色为主
- 文字用接近出版物的排版节奏
- 控件非常克制
- 聊天块只做轻微区分

### 3. 浏览器逻辑

新增：

- `assets/app.js`

逻辑上它已经能做：

- 提交 prompt
- 立即渲染用户消息
- 创建 assistant 占位消息
- 通过 `fetch('/api/chat/stream')` 读取流式响应
- 逐 token 更新 assistant 文本
- 在 `complete` 事件里记住 `sessionId`
- 下一轮继续带上这个 `sessionId`

## 这一步验证了什么

我实际运行并确认通过了这些命令：

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-server -am test "-Dtest=AxerCodeWebUiTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-server -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

另外还做了打包后的真实启动验证：

1. 用 `JDK 21` 启动打包后的 `axercode-server`
2. 请求：

`GET http://127.0.0.1:18081/`

3. 实际确认：

- 返回 `200`
- 页面内容里包含 `AxerCode`
- 页面内容里包含 `/assets/app.js`

也就是根路径页面已经真的被服务出来了。

## 需要诚实说明的边界

这一步我已经验证了：

- 静态页面能被 server 提供
- JS 资源能被提供
- JS 已经接到了 `/api/chat/stream`

但我还没有做浏览器自动化回归，比如：

- 真正打开浏览器点击发送
- 自动断言 assistant 文本逐 token 出现

所以“页面上线了”这件事我已经验证过；“浏览器交互自动化回归”还没有做。

## 你可以学到什么

这一步最重要的工程点是：

1. 第一版 Web UI 完全可以不依赖前端框架  
只要交互模型够简单，静态页就能先跑通产品方向。

2. 服务端 SSE 就算是 `POST`，浏览器也还是能接  
可以用 `fetch + ReadableStream` 手动解析事件流。

3. MockMvc 对 welcome page 的断言要理解 Spring Boot 的 forward 行为  
根路径欢迎页在测试里不一定直接给内容体，可能是 forward 到 `index.html`。

## 下一步

Step 27 最自然的方向是把 Web UI 做到“能更像产品”一点：

- 会话历史显示更完整
- 工具执行反馈在网页里可见
- 错误状态更友好
- 可能再加一个简短的 session 状态信息区
