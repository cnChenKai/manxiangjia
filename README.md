# 漫享家 / MangaHaven

Android 原生离线漫画阅读器，面向本地与私有内容源（NAS/SMB/WebDAV/OPDS）。

## 核心功能

- **多格式支持**: ZIP/CBZ/RAR/PDF/EPUB/图片文件夹
- **多源导入**: 本地 SAF、SMB、WebDAV、OPDS
- **阅读器**: 左右翻页/右左翻页/纵向滚动、双页模式、白边裁切、音量键翻页
- **书架管理**: 封面网格、搜索、收藏、阅读状态过滤、最近阅读
- **进度记忆**: 自动保存/恢复阅读进度，支持每本书独立设置
- **隐私保护**: 生物识别/设备凭据锁
- **设置导入导出**: JSON 格式备份与恢复

## 项目结构

```
app/                  # 应用入口、导航、主题、DI、日志
core-model/           # 纯领域模型（无 Android 依赖）
core-reader/          # 阅读器接口 + 白边裁切
data-local/           # Room DB、DataStore、Repository
data-files/           # 容器读取器、远程客户端、导入器
feature-library/      # 书架 UI、源管理
feature-reader/       # 阅读器 UI
feature-settings/     # 设置页
```

## 技术栈

- Kotlin 2.1.0 + Jetpack Compose (BOM 2024.12.01)
- Material 3 + Dynamic Color (Android 12+)
- Hilt (依赖注入) + Room (数据库) + DataStore (偏好存储)
- Coil (图片加载) + OkHttp (网络)
- jcifs-ng (SMB) + junrar (RAR)
- WorkManager (后台同步)

## 开发环境

- JDK 17
- Android SDK (Android Studio 或 CI 安装)
- 使用仓库内 Gradle Wrapper

## 构建

```bash
# 推荐：使用 CI 脚本
./scripts/ci/build_debug.sh

# 或手动构建
./gradlew assembleDebug --stacktrace

# 验证 APK
ls */build/outputs/apk/debug/*.apk
```

## 当前开发进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 0: 工程骨架 | ✅ 完成 | 多模块架构、DI、数据库、导航 |
| Phase 1: MVP 本地阅读 | ✅ 完成 | SAF 导入、ZIP/CBZ、书架、阅读器、进度记忆 |
| Phase 2: 阅读增强 | ✅ 完成 | 双页模式、白边裁切、音量键、每本书设置 |
| Phase 3: 远程源 | 🟡 大部分完成 | WebDAV/SMB 已通，OPDS UI 和远程压缩包待完成 |
| Phase 4: 大书架 | 🔴 进行中 | Snapshot、搜索、标签、批量操作 |
| Phase 5: 高级功能 | 🔴 未开始 | RAR/PDF/EPUB 增强、AI 增强、多端同步 |

## 已知限制

- 远程压缩包（ZIP/CBZ on SMB/WebDAV）暂不支持直接阅读
- OPDS 源无法通过 UI 添加（客户端已实现，UI 未接入）
- 测试覆盖率较低
- 所有界面文字为中文硬编码

## 协作约定

详见 `AGENTS.md`：
- 只做最小可验证改动
- 禁止在根目录堆放临时 patch 脚本
- 所有改动需包含验证命令与结果
- CI 与本地构建入口保持一致

## 许可证

详见 `LICENSE`。
