# Step 30 - Native Release Pipeline

## 这一步做了什么

Step 30 把 AxerCode 的 GraalVM Native Image 路线正式接进了仓库，并且在当前机器上做到了“能验证的都验证了，不能验证的明确告诉你卡在哪里”。

完成内容包括：

- 在父 `pom` 中加入 Graal Native Build Tools 版本管理
- 在 `axercode-cli` 和 `axercode-server` 中加入 `aot` / `native` Maven profile
- 新增 CLI 和 server 的 AOT-on-JVM 验证脚本
- 新增 CLI 和 server 的 native build preflight/build 脚本
- 新增按 D 盘偏好安装 GraalVM 的辅助脚本
- 更新 README 的原生发布说明

## 为什么这样设计

当前机器已经确认：

- 没有 `GraalVM`
- 没有 `native-image`
- 没有可见的 Windows C/C++ 构建工具链

所以这一步不能不诚实地说“原生 exe 已经打出来了”。正确的做法是两层推进：

1. 先把 Spring AOT 和 Graal Native Build Tools 接进仓库
2. 在当前机器上先验证 AOT-on-JVM
3. 把真正 native build 的阻塞点脚本化、文档化

这样后面只要机器补齐 GraalVM 和 Windows 链接器，就能沿着已经存在的脚本和 Maven profile 往前走，而不是重新设计。

## 关键文件职责

- `pom.xml`
  - 统一管理 `native-maven-plugin` 版本
- `axercode-cli/pom.xml`
  - CLI 的 `aot` / `native` build profile
- `axercode-server/pom.xml`
  - server 的 `aot` / `native` build profile
- `scripts/verify-cli-aot-jvm.ps1`
  - 在 JVM 上验证 CLI 的 Spring AOT 路线
- `scripts/verify-server-aot-jvm.ps1`
  - 在 JVM 上验证 server 的 Spring AOT 路线
- `scripts/build-native-cli.ps1`
  - CLI 原生构建预检查和执行入口
- `scripts/build-native-server.ps1`
  - server 原生构建预检查和执行入口
- `scripts/install-graalvm-on-d.ps1`
  - 把 GraalVM 安装到 `D:\GraalVM` 的辅助入口

## 真实验证

### 1. CLI AOT-on-JVM

执行：

```powershell
powershell -ExecutionPolicy Bypass -File D:\AeroCode1\scripts\verify-cli-aot-jvm.ps1
```

实际输出：

```text
AOT_CLI_OK
```

说明 CLI 在 Spring AOT 路线下已经可以成功生成并在 JVM 上运行。

### 2. Server AOT-on-JVM

执行：

```powershell
powershell -ExecutionPolicy Bypass -File D:\AeroCode1\scripts\verify-server-aot-jvm.ps1
```

实际输出：

```text
AOT_SERVER_OK
```

说明 server 在 Spring AOT 路线下已经可以成功生成并在 JVM 上启动、提供健康检查接口。

### 3. Native build preflight

执行：

```powershell
powershell -ExecutionPolicy Bypass -File D:\AeroCode1\scripts\build-native-cli.ps1
powershell -ExecutionPolicy Bypass -File D:\AeroCode1\scripts\build-native-server.ps1
```

实际输出都是友好失败：

```text
native-image not found. Install GraalVM to D: with scripts\install-graalvm-on-d.ps1 first.
```

这说明脚本已经能清楚指出当前机器的第一层阻塞点，而不是掉进 GraalVM 内部错误。

### 4. GraalVM 安装脚本 preflight

执行：

```powershell
powershell -ExecutionPolicy Bypass -File D:\AeroCode1\scripts\install-graalvm-on-d.ps1
```

实际输出：

```text
Download an official GraalVM for JDK 21 Windows ZIP, then rerun with -ZipPath <path-to-zip>. Install target will be D:\GraalVM.
```

说明安装脚本本身已经可用，而且满足“主要程序放 D 盘”的约束。

### 5. 全仓验证

顺序执行并通过：

```bat
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## 最终状态

到 Step 30 为止，AxerCode 的仓库级开发计划已经走完 `30/30`：

- CLI 可用
- Web UI 可用
- Windows app-image 可用
- AOT/native build track 已接入

仍然没有被强行夸大的部分也已经明确：

- 当前机器上还没有真正产出 GraalVM native `.exe`
- 阻塞点是 `GraalVM/native-image` 和 Windows C/C++ toolchain 缺失

## 这一步完成后的意义

Step 30 的价值不只是“多了几个脚本”，而是整个项目现在已经同时具备：

1. 当前机器上可交付的 JVM/desktop 产物
2. 面向 native release 的真实仓库结构和验证链

这意味着后续如果你决定继续补机器环境，Native Image 不再是从零开始，而是直接进入执行阶段。
