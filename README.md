# 漫享家 / MangaHaven

Android 原生漫画阅读器，面向本地漫画与私有内容源（NAS/SMB/WebDAV/OPDS），支持离线书架管理、远程源导入和多格式阅读。

## 核心功能

- **多格式支持**: ZIP/CBZ/RAR/PDF/EPUB/图片文件夹
- **多源导入**: 本地 SAF、SMB、WebDAV、OPDS
- **远程阅读**: 支持远程源扫描、OPDS 添加入口，以及远程压缩包下载缓存后阅读
- **阅读器**: 左右翻页/右左翻页/纵向滚动/连续纵向滚动（Webtoon）、双页模式、白边裁切、音量键翻页
- **阅读交互**: 双指缩放、双击缩放、点击区域导航、可拖动页面滑块与页码浮层
- **书架管理**: 封面网格、搜索、收藏、阅读状态过滤、最近阅读、标签、多选批量操作
- **进度与快照**: 自动保存/恢复阅读进度，支持每本书独立设置，并提供书籍快照能力
- **统计信息**: 设置页展示书架总数、阅读状态分布、收藏数量等统计
- **隐私保护**: 生物识别/设备凭据锁
- **设置导入导出**: JSON 格式备份与恢复阅读偏好、应用配置和远程源配置

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
- kotlinx.serialization (设置导入导出)

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
| Phase 3: 远程源 | ✅ 完成 | WebDAV/SMB、OPDS UI 接入、远程压缩包下载缓存阅读 |
| Phase 4: 大书架 | ✅ 完成 | 标签、快照、批量操作、书架统计 |
| Phase 5: 高级功能 | 🔴 未开始 | AI 增强、多端同步、更多阅读体验优化 |

## 近期更新

### Phase 3-4 完成

- 接入 OPDS UI 添加入口，并通过 `SourceClientFactory` 统一路由远程源客户端
- 支持远程压缩包下载到本地缓存后阅读，补齐 SMB/WebDAV 远程漫画阅读链路
- 新增标签系统，支持书籍与标签的多对多关联
- 新增快照系统，用于保存书籍状态与元信息快照
- 书架支持长按多选，并提供批量标记、收藏、删除等操作
- 设置页新增书架统计，展示总数、阅读状态分布和收藏数量

### 阅读器增强

- 新增双指缩放与双击缩放
- 新增点击区域导航，并支持 4 种 `TapZoneProfile` 模式
- 新增连续纵向滚动模式，适配 Webtoon 类阅读体验
- 页面滑块支持拖动，并提供页码浮层与防抖处理

### 设置备份与恢复

- 设置页新增「一键导出设置」和「一键导入设置」
- 使用 Android SAF `CreateDocument` / `OpenDocument`，无需额外存储权限
- JSON 备份包含阅读设置、应用配置和远程源配置
- 导入时远程源使用覆盖策略，避免重复导入

## 已知限制与注意事项

- 设置导出的 JSON 会包含远程源连接信息；在内部测试阶段便于迁移配置，但请勿将导出文件公开分享
- 远程压缩包当前采用下载缓存后阅读，不是完整的随机流式读取
- 测试覆盖率仍需继续补强
- 多端同步、AI 增强等高级功能尚未实现
- 所有界面文字目前仍以中文硬编码为主，尚未接入完整国际化资源

## 协作约定

详见 `AGENTS.md` 和 `CLAUDE.md`：
- 只做最小可验证改动
- 禁止在根目录堆放临时 patch 脚本
- 所有改动需包含验证命令与结果
- CI 与本地构建入口保持一致

## 许可证

详见 `LICENSE`。
