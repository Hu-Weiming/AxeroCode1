# Step 23 - Session Diff Mode

## 这一步做了什么

Step 23 给 AxerCode CLI 增加了第一版 `/diff`。

现在 interactive shell 支持：

- `/diff`
- `/diff checkpoint <name>`
- `/diff branch <name>`

行为规则：

- `/diff` 优先对比 active checkpoint
- 如果没有 active checkpoint，就尝试 active branch
- 如果两者都没有，就给出友好的引导提示
- 输出的是 message-level diff，不是 Git 文件 diff

## 为什么这样设计

当前项目已经有：

- 会话快照 `SessionContext`
- 命名 checkpoint
- 会话 branch

所以 Step 23 最稳的落点不是直接做 Git diff，而是先做“会话差异摘要”。

这样做的好处是：

- 可以立即复用 checkpoint / branch 这两类已有状态
- 不需要引入文件系统扫描或 Git 依赖
- 能先验证命令层、状态层、渲染层的交互边界

## 关键实现

### 1. Core 差异器

新增：

- `SessionDiff`
- `SessionContextDiffer`

这套工具按“最长公共前缀 + 双方尾部差异”的方式比较两个会话。

比较规则：

- 比较 message 的 `role` 和 `content`
- 忽略 `message id`

忽略 `message id` 是必要的，因为 branch 和 checkpoint 快照本来就会产生新的消息 id。

### 2. Slash 命令

`SlashCommandDispatcher` 现在支持：

- `/diff`
- `/diff checkpoint <name>`
- `/diff branch <name>`

输出格式会显示：

- 对比目标
- 公共前缀消息数
- 当前会话独有消息
- 参考会话独有消息

如果完全一致，会返回：

- `[AxerCode] No differences.`

### 3. JLine 可发现性

`InteractiveShellService` 的 completer 和 `/help` 现在都已经包含 `/diff`。

## 这一步里发现并修掉的真实问题

Step 23 在打包后的真实 interactive 验证里暴露了一个之前没被测出来的问题：

- SQLite checkpoint 原来并不是“冻结快照”
- `saveCheckpoint(...)` 只是把 checkpoint 名称指向当前 `session_id`
- 后面当前会话继续写回同一个 `session_id` 时，checkpoint 也会跟着被改掉

这个问题会直接导致：

- `/diff` 对 active checkpoint 误报“没有差异”

我在这一步顺手把 checkpoint 语义修正成真正的快照：

- 保存 checkpoint 时先克隆成新的 session snapshot
- `/restore` 时再克隆成新的工作会话

这样 checkpoint 自己不会被后续对话污染。

## 这一步验证了什么

我实际运行并确认通过了这些命令：

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-core -am test "-Dtest=SessionContextDifferTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=SlashCommandDispatcherTest,InteractiveShellServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-storage-sqlite -am test "-Dtest=SqliteShellStateRepositoryTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-core -am test
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q -pl axercode-storage-sqlite -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

另外还做了打包后的真实 CLI 回归：

1. 发第一轮消息
2. `/checkpoint alpha`
3. 再发第二轮消息
4. `/diff`

最终实际输出里看到：

- `Common prefix messages: 2`
- `Current-only messages: 2`

说明 checkpoint 现在已经是冻结快照，`/diff` 也能正确显示后续新增对话。

## 你可以学到什么

这一步最重要的工程点有两个：

1. “能存下来”不等于“是快照”

如果只是保存引用关系或同一个主键，后续写入会把所谓 checkpoint 一起带着变化。

2. 真正的回归经常出现在打包产物和真实交互里

前面的单元测试都过了，但只有这一步用真实 CLI 跑出来，才把 checkpoint 的语义漏洞暴露出来。

## 下一步

现在 Step 23 和 Step 24 都已经补齐了，接下来继续往后推进时，最自然的是进入 Step 25，把服务端 REST/SSE 骨架搭起来。
