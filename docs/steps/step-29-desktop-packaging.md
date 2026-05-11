# Step 29 - Desktop Packaging

## 这一步做了什么

Step 29 把 AxerCode 的桌面打包推进到了“有真实产物”的状态，同时也把 Pake 路线接进了仓库，但没有假装当前机器已经能直接打出 Pake 壳。

完成内容包括：

- 新增 `jpackage` 的 Windows app-image 打包脚本
- 新增 `desktop/pake-shell` 的本地 Pake workspace
- 固定 `pake-cli` 版本为 `3.11.3`
- 新增 Rust-on-D 的安装辅助脚本
- 更新 README 的桌面打包说明

## 为什么这一步要两条线并行

当前机器已经确认：

- 没有 `cargo`
- 没有 `rustup`
- 没有可见的 MSVC / Visual Studio Build Tools

在这种条件下，直接宣称“Pake 已完成”是不诚实的。Pake/Tauri 在 Windows 上还依赖 Rust 和链接器工具链，所以这一步采用的是混合策略：

1. 先用现有 JDK 21 的 `jpackage` 产出一个真实可运行的 Windows 桌面包
2. 同时把 Pake 路线所需的 workspace、版本、脚本、安装入口全部接好

这样既没有放弃原来的技术路线，也不让桌面交付继续卡住。

## 关键文件职责

- `desktop/pake-shell/package.json`
  - Pake workspace 和 `pake-cli` 版本锁定
- `desktop/pake-shell/README.md`
  - 说明当前 Pake 路线的状态与前置条件
- `scripts/build-pake-shell.ps1`
  - Pake 构建预检查和执行入口
- `scripts/install-rust-on-d.ps1`
  - 按 D 盘偏好安装 Rust 的辅助脚本
- `scripts/build-desktop-app-image.ps1`
  - `jpackage` Windows app-image 打包入口

## 真实验证

### Pake workspace 验证

先安装并验证了 Node 侧依赖：

```powershell
cd D:\AeroCode1\desktop\pake-shell
D:\nodejss\npm.cmd install
D:\nodejss\npm.cmd run doctor
```

实际结果：

- `npm install` 成功
- `npm run doctor` 输出 `3.11.3`

然后执行了 Pake 构建脚本预检查：

```powershell
powershell -ExecutionPolicy Bypass -File D:\AeroCode1\scripts\build-pake-shell.ps1
```

实际结果是友好失败，不是异常堆栈：

```text
cargo not found. Install Rust to D: first with scripts\install-rust-on-d.ps1, and ensure Windows C++ build tools are available.
```

这证明 Pake 路线当前的阻塞点已经被明确、脚本化并落到仓库里了。

### jpackage 验证

执行了真正的打包：

```powershell
powershell -ExecutionPolicy Bypass -File D:\AeroCode1\scripts\build-desktop-app-image.ps1
```

实际产物存在：

- `D:\AeroCode1\dist\desktop\AxerCode\AxerCode.exe`

并且不是只看文件存在，我还直接启动了打包后的 exe：

```powershell
Start-Process D:\AeroCode1\dist\desktop\AxerCode\AxerCode.exe -ArgumentList '--axercode.desktop.launch-on-startup=false','--server.port=19092'
```

随后探活：

```text
http://127.0.0.1:19092/
```

实际验证输出：

```text
JPACKAGE_APP_ROOT_OK
```

说明当前桌面包已经能真实启动本地 AxerCode 服务并提供 Web UI。

## 全仓验证

顺序执行并通过：

```bat
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## 这一步完成后的意义

到 Step 29 为止，AxerCode 已经具备：

- 一个当前机器上可真实产出的 Windows 桌面包
- 一条已接入仓库、已锁版本、已预检查的 Pake 路线

这意味着 Step 30 可以专注在最终发布质量：例如 GraalVM Native Image、后端替换、以及在具备 Rust/C++ 工具链后把 Pake 壳真正打出来。
