# MangaHaven (漫享家) 项目开发指南

## 项目概述

Android 原生离线漫画阅读器，对标"可达阅读器"。核心面向本地与私有内容源（NAS/SMB/WebDAV/OPDS），不做在线漫画源聚合。

**技术栈**: Kotlin 2.1.0 + Jetpack Compose (BOM 2024.12.01) + Material 3 + Hilt + Room + DataStore + Coil + OkHttp

## 模块结构

| 模块 | 职责 |
|---|---|
| `app` | 入口、导航、主题、DI、日志、应用锁 |
| `core-model` | 纯领域模型（无 Android 依赖） |
| `core-reader` | 阅读器接口 + 白边裁切 |
| `data-local` | Room DB、DataStore、Repository、封面管理 |
| `data-files` | 容器读取器、远程客户端、导入器、缩略图、同步 Worker |
| `feature-library` | 书架 UI、源管理、远程浏览 |
| `feature-reader` | 阅读器 UI、设置面板、缩略图条 |
| `feature-settings` | 设置页、导入导出、缓存管理 |

## 开发进度

- **Phase 0** (工程骨架): ✅ 已完成
- **Phase 1** (MVP 本地阅读): ✅ 已完成
- **Phase 2** (阅读体验增强): ✅ 已完成（含点击区域、缩放、连续滚动、滑块跳页）
- **Phase 3** (远程源): ✅ 已完成（含 OPDS UI、远程压缩包下载缓存）
- **Phase 4** (大书架): ✅ 已完成（标签、Snapshot、批量操作、统计）
- **Phase 5** (高级功能): 🟡 部分完成（RAR/PDF/EPUB、缩放、连续滚动、隐私锁）

## 构建与验证

```bash
# 编译检查
./gradlew :data-local:compileDebugKotlin --stacktrace

# 完整构建
./gradlew assembleDebug --stacktrace

# 验证 APK
ls */build/outputs/apk/debug/*.apk
```

## 关键架构约定

### 数据流
```
UI (Compose) → ViewModel (StateFlow) → Repository → DAO / SourceClient
```

### 容器读取
所有格式（ZIP/CBZ/RAR/PDF/EPUB/文件夹）通过 `ContainerReader` 接口统一：
```kotlin
interface ContainerReader {
    suspend fun listPages(target: ContainerTarget): List<PageRef>
    suspend fun openPage(pageRef: PageRef): InputStream
    suspend fun extractCover(target: ContainerTarget): InputStream?
}
```

### 远程源
通过 `SourceClient` 接口统一 WebDAV/SMB/OPDS：
```kotlin
interface SourceClient {
    suspend fun list(path: String): List<SourceEntry>
    suspend fun stat(path: String): SourceEntry?
    suspend fun openStream(path: String): InputStream
    suspend fun exists(path: String): Boolean
}
```

### 阅读器设置
支持全局默认 + 每本书覆盖（`ItemReaderSettingsOverride`），ViewModel 层做 merge。

## 已知技术债务

1. **`ContainerReaderFactory.createReader()` 返回 `Any`** — ✅ 已修复为返回 `ContainerReader`
2. **数据库使用 `fallbackToDestructiveMigration()`** — 正式版需改为增量迁移
3. **`PdfContainerReader` 和 `OpdsSourceClient` 字符串模板转义错误** — ✅ 已修复
4. **`SourceEntity` 缺少 `isVirtual` 字段** — ✅ 已补充
5. **`CrashUploader` 硬编码邮箱** — ✅ 已改为占位值
6. **`PreloadController` 和 `ReaderSession` 接口未实现** — 已定义但无实现
7. **TapZoneProfile 枚举存在但无实际逻辑** — ✅ 已实现 4 种模式
8. **远程压缩包（ZIP/CBZ on SMB/WebDAV）无法打开** — ✅ 已实现下载缓存策略
9. **OPDS 源无法通过 UI 添加** — ✅ 已集成
10. **测试覆盖率极低** — 约 10 个测试文件覆盖 60+ 源文件

## 编码规范

- 所有 IO 和解码不能阻塞主线程
- 使用 Kotlin 协程 + Flow
- UI 使用 Jetpack Compose + Material 3
- 依赖注入使用 Hilt
- 日志使用 Timber
- 注释使用中文（与现有代码保持一致）
- 不生成在线漫画源爬虫功能
- 临时脚本放 `scripts/`，不要堆在根目录
