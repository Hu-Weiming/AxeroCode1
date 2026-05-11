# Step 28 - Desktop Preview Runtime

## 这一步做了什么

Step 28 为 AxerCode 增加了真正的桌面预览运行层，但还没有进入 Rust/Cargo 或 Pake 打包阶段。

完成内容包括：

- 新增 `desktop` Spring profile
- 新增桌面预览配置对象、URL 构建器、浏览器启动器和启动事件监听器
- 新增 Windows `cmd` / `PowerShell` 预览启动脚本
- 保持现有 `axercode-server` 作为唯一运行时，不额外复制一套桌面后端

## 为什么这样设计

当前机器没有 Rust/Cargo，所以现在不适合直接开始 Pake。先把“桌面模式下本地服务怎么起、怎么限制在 localhost、怎么自动打开 UI”定义清楚，后面真正做桌面壳时就只需要包裹这条稳定运行链，而不是一边打包一边重新发明启动行为。

## 关键文件职责

- `axercode-server/src/main/java/com/axercode/server/desktop/DesktopApplicationProperties.java`
  - 绑定 `axercode.desktop.*` 配置
- `axercode-server/src/main/java/com/axercode/server/desktop/DesktopUrlBuilder.java`
  - 统一构建本地 UI 启动地址
- `axercode-server/src/main/java/com/axercode/server/desktop/BrowserLauncher.java`
  - 浏览器打开能力抽象
- `axercode-server/src/main/java/com/axercode/server/desktop/SystemBrowserLauncher.java`
  - 桌面系统浏览器的真实实现
- `axercode-server/src/main/java/com/axercode/server/desktop/DesktopStartupLauncher.java`
  - 根据 desktop 配置决定是否在启动后打开 UI
- `axercode-server/src/main/java/com/axercode/server/desktop/DesktopApplicationReadyListener.java`
  - 把 Spring Boot 的 `ApplicationReadyEvent` 转成桌面预览启动动作
- `axercode-server/src/main/resources/application-desktop.yml`
  - 桌面预览的 localhost 配置和默认端口
- `scripts/launch-desktop-preview.cmd`
  - Windows CMD 桌面预览入口
- `scripts/launch-desktop-preview.ps1`
  - Windows PowerShell 桌面预览入口

## TDD 过程

先写了 3 个失败测试：

- `DesktopUrlBuilderTest`
- `DesktopStartupLauncherTest`
- `DesktopApplicationReadyListenerTest`

第一次运行时确认红灯来自缺失实现。中途还修正了一处测试自身的问题：`ApplicationReadyEvent` 需要 `ConfigurableApplicationContext`，所以测试里改成了 `ServletWebServerApplicationContext`。之后再补最小实现，并把 `BrowserLauncher` 从 checked exception 接口改成了更适合运行时和 mock 的无 checked exception 接口。

## 真实验证

顺序执行并通过：

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-server -am test "-Dtest=DesktopUrlBuilderTest,DesktopStartupLauncherTest,DesktopApplicationReadyListenerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-server -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

还做了打包产物的真实启动验证：

- 以 `desktop` profile 启动 `axercode-server-0.1.0-SNAPSHOT.jar`
- 临时加 `--axercode.desktop.launch-on-startup=false`
- 探活 `http://127.0.0.1:19091/`
- 实际结果：返回根页面并包含 `AxerCode` 与 `/assets/app.js`

验证输出：

```text
DESKTOP_PROFILE_ROOT_OK
```

## 你可以怎么运行

### 直接运行打包产物

```bat
C:\Program Files\Java\jdk-21\bin\java.exe -jar D:\AeroCode1\axercode-server\target\axercode-server-0.1.0-SNAPSHOT.jar --spring.profiles.active=desktop
```

### 使用桌面预览脚本

```bat
D:\AeroCode1\scripts\launch-desktop-preview.cmd
```

或：

```powershell
D:\AeroCode1\scripts\launch-desktop-preview.ps1
```

## 这一步完成后的意义

到这一步，AxerCode 已经具备“像桌面应用一样启动”的基础运行方式：本地 profile、localhost 绑定、自动打开界面、Windows 启动脚本都已经具备。下一步 Step 29 就可以专注在真正的桌面打包，而不是继续补启动层。
