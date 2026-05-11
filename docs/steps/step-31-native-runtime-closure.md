# Step 31: Native Runtime Closure

## 这一步做了什么

Step 31 的目标不是新增功能，而是把 Step 30 剩下的真实交付阻塞点全部收掉，让 AxerCode 的 CLI、server、桌面壳和原生发布链都从“仓库里有脚本”推进到“本机真能跑”。

这一轮完成了四类收尾工作：

1. 补齐本机原生构建环境  
   - 安装 Visual Studio Build Tools 到 `D:\BuildTools2022`
   - 安装 Rust/Cargo 到 `D:\Rust`
   - 安装 GraalVM 到 `D:\GraalVM`
   - 安装 WiX binaries 到 `D:\WiX\wix314`

2. 修复原生构建链  
   - native profile 不再错误地对 Spring Boot fat jar 做 native-image
   - native 构建临时目录切到 `D:\AeroCode1\target`
   - SQLite 原生 DLL 导出到 `target` 目录，与 exe 放在一起

3. 修复原生运行时问题  
   - Picocli native 反射元数据补齐
   - Ollama request/response 的 Jackson native binding hints 补齐

4. 收口桌面产物  
   - `build-pake-shell.ps1` 在 MSI bundling 超时的情况下，仍会把已经构建出来的 `pake-axercode.exe` 复制到稳定产物目录 `dist\desktop\pake\AxerCode-Pake.exe`

## 为什么这样设计

Step 31 的核心原则是“先定位真实根因，再做最小修复”，而不是为了 native 去大改应用结构。

- native-image 首先失败，不是因为 GraalVM 不可用，而是因为 native profile 仍然吃到了 Spring Boot repackaged fat jar，主类在 `BOOT-INF/classes` 里不可见。
- 构建通过后，CLI native 首个运行时失败是 `sqlite-jdbc` DLL 没有导出。
- 修掉 SQLite 后，CLI native 继续暴露出 Picocli 的字段反射问题。
- server native 则继续暴露出 Jackson 对 `OllamaChatRequest/OllamaChatResponse` 的 native binding 问题。

这一轮的修法都是“针对真实失败点最小补洞”，所以不会把现有的 JVM 运行方式、Spring Boot 3.3.4 结构和 CLI/server 功能线打乱。

## 关键改动

### 1. 原生 profile 不再走 fat jar

文件：

- `D:\AeroCode1\axercode-cli\pom.xml`
- `D:\AeroCode1\axercode-server\pom.xml`

做法：

- 给 Spring Boot `repackage` execution 增加可控 `skip`
- 在 `native` profile 下把 repackage 关掉

这样 native-image 会基于正常 classpath 构建，而不是错误地把 repackaged archive 当成普通 jar 处理。

### 2. 原生构建目录固定到 D 盘

文件：

- `D:\AeroCode1\scripts\build-native-cli.ps1`
- `D:\AeroCode1\scripts\build-native-server.ps1`

做法：

- 构建时把 `TEMP`、`TMP`、`NATIVE_IMAGE_USER_HOME` 都切到 `D:\AeroCode1\target`
- 自动接入 `D:\BuildTools2022` 的 `vcvars64.bat`
- 自动读取用户环境变量中的 `GRAALVM_HOME`

这样避免了 C 盘空间和默认临时目录带来的不稳定因素。

### 3. SQLite DLL 与 exe 同目录导出

文件：

- `D:\AeroCode1\axercode-cli\pom.xml`
- `D:\AeroCode1\axercode-server\pom.xml`

做法：

- 在 native build args 里加上 `-Dorg.sqlite.lib.exportPath=${project.build.directory}`

结果：

- CLI native 产物旁边会生成 `sqlitejdbc.dll`
- server native 产物旁边也会生成 `sqlitejdbc.dll`

### 4. Picocli native 运行时补齐

文件：

- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\bootstrap\CliNativeHints.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\bootstrap\AxerCodeCliApplication.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\command\AxerCodeCliCommand.java`

做法：

- 移除 `mixinStandardHelpOptions = true`，显式声明 `-h/--help` 和 `-V/--version`
- 通过 `CliNativeHints` 显式注册 `AxerCodeCliCommand` 的反射访问

### 5. Ollama Jackson native binding 补齐

文件：

- `D:\AeroCode1\axercode-provider-ollama\src\main\java\com\axercode\provider\ollama\OllamaRuntimeHints.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\config\ProviderConfiguration.java`
- `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\config\ServerProviderConfiguration.java`

做法：

- 使用 Spring 的 `BindingReflectionHintsRegistrar`
- 注册 `OllamaChatRequest`、`OllamaChatResponse` 及其嵌套记录类型
- 在 CLI 和 server 的 ProviderConfiguration 中导入这组 hints

### 6. Pake 稳定产物目录

文件：

- `D:\AeroCode1\scripts\build-pake-shell.ps1`

做法：

- 构建后如果发现 `pake-axercode.exe` 已经生成，就复制到：
  - `D:\AeroCode1\dist\desktop\pake\AxerCode-Pake.exe`
- 如果本机存在 `D:\WiX\wix314\candle.exe` 和 `light.exe`，脚本会优先把 WiX 加进 PATH
- 即使 WiX/MSI bundling 因下载超时失败，脚本也会把已生成的桌面壳交付出来

## 这一步如何验证

这一轮做了真实验证，不是只看文件存在。

### CLI native

命令：

```powershell
& 'D:\AeroCode1\axercode-cli\target\axercode-cli.exe' --prompt 'Reply with exactly NATIVE_CLI_OK and nothing else.'
```

结果：

```text
NATIVE_CLI_OK
```

### server native

命令逻辑：

- 启动 `D:\AeroCode1\axercode-server\target\axercode-server.exe`
- 探活 `http://127.0.0.1:19095/api/health`
- 调用 `POST /api/chat`

结果：

```json
{"HealthStatus":"ok","Application":"axercode-server","Reply":"NATIVE_SERVER_OK"}
```

### Pake desktop shell

命令：

```powershell
powershell -ExecutionPolicy Bypass -File D:\AeroCode1\scripts\build-pake-shell.ps1
```

产物：

- `D:\AeroCode1\dist\desktop\pake\AxerCode-Pake.exe`

运行验证：

- 用 native server 在 `19090` 提供 UI
- 启动 `AxerCode-Pake.exe`
- 进程保持存活，说明桌面壳可正常打开

### 仓库验证

顺序执行：

```bat
cmd /c D:\AeroCode1\scripts\mvn-jdk21.cmd -q test
cmd /c D:\AeroCode1\scripts\mvn-jdk21.cmd -q package
```

两条都通过。

## 现在你可以直接使用的产物

- JVM CLI：
  - `D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar`

- native CLI：
  - `D:\AeroCode1\axercode-cli\target\axercode-cli.exe`

- JVM server：
  - `D:\AeroCode1\axercode-server\target\axercode-server-0.1.0-SNAPSHOT.jar`

- native server：
  - `D:\AeroCode1\axercode-server\target\axercode-server.exe`

- jpackage desktop app image：
  - `D:\AeroCode1\dist\desktop\AxerCode\AxerCode.exe`

- Pake desktop shell：
  - `D:\AeroCode1\dist\desktop\pake\AxerCode-Pake.exe`

## 这一步你可以学到什么

Step 31 最有价值的经验是：native 发布链最难的部分通常不是“装上 GraalVM”，而是把运行时里所有依赖反射、资源、DLL、fat jar 布局这些隐性假设一层层揭出来。

换句话说，原生发布的真正工作量，往往发生在“第一次成功编译出 exe 之后”。
