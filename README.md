# MangaHaven

MangaHaven 是一个基于 **Android + Kotlin + Compose** 的漫画阅读器项目，采用多模块架构，支持本地与远程源（WebDAV / SMB）导入、阅读与进度管理。

## 项目目标

- 稳定产出 Debug APK（本地 + CI 一致）
- 支持“vibe coding”协作（Codex/Jules 等代理可控、可回滚、可验证）
- 提供可追踪的构建日志和运行时崩溃日志，降低“只靠口述 bug”带来的排障成本

## 模块结构

- `app`：应用入口、导航、全局初始化
- `core-model`：领域模型和仓库接口
- `core-reader`：阅读核心能力
- `data-local`：Room/DataStore 本地数据层
- `data-files`：文件/远程源与导入逻辑
- `feature-library`：书库相关 UI/状态
- `feature-reader`：阅读器 UI/状态
- `feature-settings`：设置页

## 快速开始

### 环境

- JDK 17
- Android SDK（由 Android Studio 或 CI 安装）
- 使用仓库内 Gradle Wrapper

### 推荐命令

```bash
./scripts/ci/build_debug.sh
```

该脚本会按顺序执行：

1. `./gradlew :data-local:compileDebugKotlin --stacktrace`
2. `./gradlew assembleDebug --stacktrace`
3. 校验 `*/build/outputs/apk/debug/*.apk` 是否存在
4. 将日志输出到 `build-logs/`

## CI/CD 流水线（简化后）

GitHub Actions 使用与本地一致的入口：

- `./scripts/ci/build_debug.sh`
- 成功时上传 Debug APK
- 失败时上传 `build-logs/` 与 Gradle 报告

详见：`.github/workflows/android.yml`

## 运行时错误日志（新增）

应用会在未捕获异常时将崩溃信息写入：

- `files/crash-logs/*.log`（应用沙箱内）

可通过脚本导出（连接设备后）：

```bash
./scripts/ci/export_crash_logs.sh com.mangahaven.app
```

这能把“口述问题”转成可归档日志，便于流水线回放与定位。

## 协作约束（vibe coding）

请先阅读根目录 `AGENTS.md`：

- 只做最小可验证改动
- 禁止在仓库根目录堆放临时 patch 脚本
- 所有改动都要包含验证命令与结果
- CI 与本地构建入口必须保持一致

