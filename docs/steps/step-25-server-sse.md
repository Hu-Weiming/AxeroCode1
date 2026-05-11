# Step 25 - Server + SSE Skeleton

## 这一步做了什么

Step 25 把 `axercode-server` 从空模块推进成了真正可启动的本地 Spring Boot 服务。

现在 server 已经支持：

- `GET /api/health`
- `POST /api/chat`
- `POST /api/chat/stream`
- `GET /api/sessions/{sessionId}`

这意味着 AxerCode 已经不再只有 CLI 壳了，而是开始具备给后续 Web UI 和桌面端复用的本地服务能力。

## 为什么这样设计

这个阶段最重要的是：

- 不重复发明 agent/provider 逻辑
- 只在 server 里补“传输层 + 会话编排层”

所以我用了一个很薄的 server 架构：

1. `ConversationAgent` 继续作为模型与工具编排核心  
2. `SqliteSessionRepository` 继续作为会话持久化核心  
3. `axercode-server` 只负责：
   - Spring Boot 启动
   - HTTP DTO
   - REST / SSE endpoint
   - session load/save orchestration

这样后面做 Web UI 时，前端只要调这些接口，不需要重新实现 CLI 那一套逻辑。

## 关键实现

### 1. Spring Boot 启动入口

新增：

- `AxerCodeServerApplication`

server 现在已经是标准的 Spring Boot 3.3.4 应用。

### 2. Server 独立配置层

新增：

- `ServerProviderProperties`
- `ServerAgentProperties`
- `ServerStorageProperties`
- `ServerProviderConfiguration`
- `ServerStorageConfiguration`
- `ServerToolConfiguration`

为什么单独配一套：

- CLI 里的配置包含 shell / REPL 语义
- server 需要的是纯 HTTP 服务边界

所以这里刻意没有直接依赖 CLI 的配置类。

### 3. 会话编排服务

新增：

- `ServerConversationService`
- `ServerConversationTurn`

它负责：

- 新建或加载 `SessionContext`
- 决定 effective model
- 调用 `ConversationAgent`
- 把更新后的 session 写回 SQLite

### 4. HTTP / SSE API

新增：

- `AxerCodeChatController`
- `ChatRequest`
- `ChatResponse`
- `SessionResponse`
- `HealthResponse`
- 相关 message / tool DTO

其中：

- `/api/chat` 返回一次完整 JSON 回合结果
- `/api/chat/stream` 使用 `SseEmitter`
  - 发送 `token` 事件
  - 最后发送 `complete` 事件

### 5. 可执行 Spring Boot jar

这一步真实运行里还暴露了一个打包问题：

- `axercode-server` 初始产物只是普通 jar
- 缺少 Spring Boot `repackage`
- 启动时报：`没有主清单属性`

我已经修正成真正的可执行 Boot jar：

- 指定 `mainClass`
- 配置 `spring-boot-maven-plugin` 的 `repackage`

## 这一步验证了什么

我实际运行并确认通过了这些命令：

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-server -am test "-Dtest=ServerConversationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-server -am test "-Dtest=AxerCodeChatControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-server -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

另外还做了打包后的真实启动验证：

1. 使用 `JDK 21` 启动 `axercode-server-0.1.0-SNAPSHOT.jar`
2. 用独立端口和独立 SQLite 文件运行
3. 实际请求：

`GET http://127.0.0.1:18080/api/health`

返回：

```json
{"status":"ok","application":"axercode-server"}
```

这说明：

- 打包产物现在是真正可执行的
- Spring Boot server 能在你当前机器上启动
- 基本 HTTP 主链已经活了

## 你可以学到什么

这一步最重要的工程点有三个：

1. CLI 能跑，不代表服务端边界已经存在  
需要单独补 transport 层和会话 orchestration 层。

2. SSE 的第一步不一定要上 WebFlux  
Spring MVC 的 `SseEmitter` 已经足够先把 token 流式链路打通。

3. `spring-boot-maven-plugin` 声明插件不等于产出可执行 jar  
如果没有 `repackage`，最终 jar 可能根本不能直接启动。

## 下一步

Step 26 最自然的方向就是开始做 Web UI：

- 一个最小聊天页
- 调 `/api/chat` 和 `/api/chat/stream`
- 先把“浏览器能连 server 并看到回复”这条链路跑通
