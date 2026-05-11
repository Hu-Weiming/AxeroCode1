# Step 24 - Session Branching

## 这一步做了什么

Step 24 给 AxerCode CLI 增加了第一版会话分支能力。

现在 interactive shell 支持：

- `/branch`
- `/branch status`
- `/branch list`
- `/branch <name>`

行为规则：

- 如果 `/branch <name>` 对应的分支已经存在，就切换到这个已保存分支
- 如果这个分支还不存在，就把当前会话克隆成一个新的 `SessionContext`，保存为该分支并切换过去
- 当前激活分支会写入 SQLite，并在 CLI 重启后恢复
- 当前激活分支会通过临时 `SYSTEM` 消息进入模型请求，但不会污染真实会话历史

## 为什么这样设计

这个项目当前已经有：

- `SessionContext` 作为会话快照
- `SqliteSessionRepository` 负责持久化会话
- `ShellStateStore` 负责 shell 级状态

所以最稳的做法不是引入 Git 或新的会话层，而是把“分支”设计成：

- `branch name -> persisted session snapshot`
- 再额外维护一个 `active branch`

这样好处是：

- 不会破坏现有 session 持久化结构
- `/branch` 可以和 `/checkpoint`、`/plan`、`/focus` 一样走 shell 状态层
- 后面做 `/diff`、更高级的分支管理时还能继续复用

## 关键实现

### 1. 分支克隆器

新增 `SessionContextBrancher`：

- 创建新的 `SessionId`
- 为每条消息创建新的 `message id`
- 保留原始消息的 `role`、`content`、`createdAt`

这是必须的，因为 SQLite 里的 `message_id` 是全局唯一，不能把原会话消息直接复用到新分支。

### 2. Shell 状态扩展

`ShellStateStore` 现在除了 focus、checkpoint、plan mode 之外，还支持：

- `activeBranchName()`
- `setActiveBranchName(...)`
- `clearActiveBranchName()`
- `saveBranch(...)`
- `loadBranch(...)`
- `branchNames()`

内存实现和 SQLite 实现都补齐了这套能力。

### 3. SQLite 分支表

`SqliteShellStateRepository` 新增了两张表：

- `shell_branches`
- `shell_active_branch`

其中：

- `shell_branches` 保存分支名到 `session_id` 的映射
- `shell_active_branch` 保存当前激活的分支名

### 4. Slash 命令

`SlashCommandDispatcher` 现在支持：

- 查看当前分支
- 创建并切换新分支
- 切换已有分支
- 列出所有已保存分支
- `/new` 时清理当前 active branch
- `/status` 时显示 active branch

### 5. Prompt 上下文增强

`ShellContextAugmenter` 现在会在 active branch 存在时临时注入：

`Current session branch: <name>...`

这条消息只会进入 provider 请求，不会写进 SQLite 会话历史，也不会出现在 `/history` 里。

## 这一步验证了什么

我实际运行并确认通过了这些命令：

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-core -am test "-Dtest=SessionContextBrancherTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=SqliteBackedShellStateStoreTest,SlashCommandDispatcherTest,CliChatServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-core -am test
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

另外还做了两次打包后 CLI 真实运行验证：

1. 第一次启动执行 `/branch feature-a`
2. 第二次启动执行 `/status`

结果第二次启动后仍然显示：

- `Active branch: feature-a`

这说明分支状态已经能够跨重启恢复。

## 你可以学到什么

这一阶段最重要的工程点是：

- “分支”不一定意味着 Git 分支，它也可以是会话层的逻辑分支
- 只要数据边界清楚，就能先把用户体验做出来，再决定以后是否和 Git 真正联动
- 处理 SQLite 这类持久化系统时，复制对象不能只复制内容，还要考虑主键和唯一约束

## 下一步

Step 25 更自然的方向是 `/diff`：

- 先对比当前会话和 active checkpoint / active branch
- 再把差异以 shell 命令形式暴露出来

这样 `/checkpoint`、`/branch`、`/diff` 三者就能真正形成一个闭环。
