# SPEC.md
# 漫享家 / MangaHaven
# Android 离线漫画阅读器产品与技术规格（对标：可达阅读器）

## 1. 项目目标

开发一款 Android 原生漫画/图片书阅读器，核心面向以下场景：

- 用户已经拥有本地漫画文件
- 用户使用 NAS / SMB / WebDAV / OPDS 管理个人漫画库
- 用户希望直接打开压缩包或目录阅读，而不是手动解压整理
- 用户重视阅读体验、进度记忆、搜索和书架管理

本项目不做在线漫画源聚合，不做爬虫，不以资源站检索为核心卖点。

---

## 2. 产品定位

### 2.1 一句话定位
一个面向本地与私有内容源的高质量离线漫画阅读器。

### 2.2 核心价值
1. 支持本地与远程漫画内容导入
2. 支持目录、压缩包、后续扩展更多容器格式
3. 提供顺滑、稳定、可定制的阅读体验
4. 提供可维护的大书架、搜索、标签和阅读状态管理

### 2.3 MVP 范围
首个可用版本只做：
- 本地目录导入
- 单文件导入
- 图片文件夹阅读
- ZIP / CBZ 阅读
- 书架展示
- 阅读进度记忆
- 基础阅读模式（左右翻页 / 纵向滚动）

---

## 3. 非目标

以下内容不进入前两批版本：

- 在线漫画源聚合
- 站点爬虫
- 用户账号体系
- 云同步
- AI 画质增强
- 游戏手柄支持
- 多端协同
- 社区评论
- 复杂 PDF / EPUB / MOBI 全量支持
- RAR / 加密压缩包首发支持

---

## 4. 技术栈

- 语言：Kotlin
- UI：Jetpack Compose
- 架构：Clean-ish 分层架构（UI / Domain / Data）
- 状态管理：ViewModel + StateFlow
- 数据库：Room
- 设置存储：DataStore
- 后台任务：WorkManager
- 网络：OkHttp
- 图片加载：Coil
- 序列化：kotlinx.serialization
- 依赖注入：Hilt 或 Koin（二选一，推荐 Hilt）
- 测试：JUnit + Turbine + Compose UI Test

---

## 5. 系统架构

### 5.1 模块划分

#### `app`
- Application
- Navigation
- Theme
- DI 配置
- 全局错误处理

#### `core-model`
通用数据模型：
- Source
- LibraryItem
- PageRef
- ReadingProgress
- ReaderSettings
- Snapshot
- Tag

#### `core-reader`
阅读器核心能力：
- PageProvider
- PageLayoutEngine
- SpreadResolver
- CropEngine
- ReaderSession
- PreloadController

#### `data-local`
- Room DAO
- DataStore
- 封面缓存元数据
- 阅读进度持久化
- 书架索引

#### `data-files`
- Android SAF
- 本地目录扫描
- 图片文件枚举
- ZIP / CBZ 读取

#### `data-remote`
后续版本启用：
- WebDAV
- SMB
- OPDS
- 认证与连接配置
- 流式读取

#### `feature-library`
- 书架
- 搜索
- 标签
- 阅读状态
- 最近阅读
- 继续阅读

#### `feature-reader`
- 漫画阅读界面
- 手势
- 翻页
- 缩放
- 阅读设置

#### `feature-settings`
- 阅读偏好
- 缓存策略
- 导入入口
- 隐私与实验特性开关

---

## 6. 关键数据模型

### 6.1 Source
表示内容来源。

字段建议：
- `id: String`
- `type: SourceType`
- `name: String`
- `configJson: String`
- `authRef: String?`
- `isWritable: Boolean`
- `lastSyncAt: Long?`

`SourceType`：
- LOCAL
- SAF_TREE
- SMB
- WEBDAV
- OPDS

### 6.2 LibraryItem
表示书架中的作品或条目。

字段建议：
- `id: String`
- `sourceId: String`
- `path: String`
- `title: String`
- `coverPath: String?`
- `itemType: LibraryItemType`
- `pageCount: Int?`
- `readingStatus: ReadingStatus`
- `lastReadAt: Long?`
- `createdAt: Long`
- `updatedAt: Long`

`LibraryItemType`：
- FOLDER
- ARCHIVE
- BOOK
- REMOTE_ENTRY

### 6.3 ReadingProgress
- `itemId: String`
- `currentPage: Int`
- `totalPages: Int`
- `scrollOffset: Float?`
- `readingMode: ReadingMode`
- `updatedAt: Long`

### 6.4 ReaderSettings
全局阅读设置：
- `readingMode`
- `pageDirection`
- `enableCrop`
- `enablePreload`
- `doublePageMode`
- `keepScreenOn`
- `tapZoneProfile`

### 6.5 ItemReaderSettingsOverride
每本书单独覆盖设置：
- `itemId`
- `readingMode?`
- `pageDirection?`
- `cropEnabled?`
- `doublePageMode?`
- `pageOffset?`

### 6.6 Snapshot
远程书库的搜索快照：
- `id`
- `sourceId`
- `path`
- `title`
- `normalizedTitle`
- `tags`
- `pageCount`
- `updatedAt`

---

## 7. 核心接口设计

### 7.1 SourceClient
统一所有内容源的访问接口。

```kotlin
interface SourceClient {
    suspend fun list(path: String): List<SourceEntry>
    suspend fun stat(path: String): SourceEntry?
    suspend fun openStream(path: String): InputStream
    suspend fun exists(path: String): Boolean
}
```

### 7.2 ContainerReader
统一目录与压缩包读取。

```kotlin
interface ContainerReader {
    suspend fun listPages(target: ContainerTarget): List<PageRef>
    suspend fun openPage(pageRef: PageRef): InputStream
    suspend fun extractCover(target: ContainerTarget): InputStream?
}
```

### 7.3 PageProvider
供阅读器按页访问。

```kotlin
interface PageProvider {
    suspend fun getPageCount(): Int
    suspend fun openPage(index: Int): InputStream
    suspend fun preload(indices: List<Int>)
}
```

### 7.4 ProgressRepository

```kotlin
interface ProgressRepository {
    suspend fun getProgress(itemId: String): ReadingProgress?
    suspend fun saveProgress(progress: ReadingProgress)
}
```

---

## 8. UI 页面设计

### 8.1 书架页
模块：
- 最近阅读横滑区域
- 继续阅读卡片
- 全部书籍网格
- 搜索入口
- 过滤与排序入口

状态：
- 空状态
- 加载中
- 内容展示
- 错误提示

### 8.2 导入页
入口：
- 选择文件
- 选择目录
- 后续添加远程源

行为：
- 读取并校验权限
- 扫描图片 / ZIP / CBZ
- 建立书架条目
- 生成封面

### 8.3 阅读器页
能力：
- 左右翻页
- 纵向滚动
- 缩放
- 拖拽
- 单击唤出菜单
- 页面预加载
- 进度记忆

顶部菜单：
- 返回
- 标题
- 页码
- 更多操作

底部菜单：
- 阅读模式
- 页面方向
- 跳页
- 设置

### 8.4 设置页
- 全局阅读设置
- 缓存大小
- 书架排序
- 调试开关
- 关于页面

---

## 9. 分批开发计划

### Phase 0：工程骨架
目标：
- 建立模块化工程
- 导航、主题、DI、数据库、设置、后台任务接入完成

交付：
- 可运行空壳应用
- 三个空页面
- 基础数据库和设置存储
- 统一错误处理

### Phase 1：MVP 本地阅读闭环
目标：
- 用户导入一本漫画并完整读完

范围：
- SAF 导入文件 / 目录
- 图片文件夹阅读
- ZIP / CBZ
- 书架
- 最近阅读
- 继续阅读
- 阅读器基础模式
- 进度记忆

验收：
- 可导入并持久化一本漫画
- 应用重启后进度仍在
- 500+ 页 CBZ 可稳定打开
- 基本不卡顿、不崩溃

### Phase 2：阅读体验增强
范围：
- 双页模式
- 左翻 / 右翻 / 纵向切换
- 白边裁切
- 页码偏移
- 缩略图跳页
- 每本书独立阅读设置

验收：
- 可手动切换单双页
- 对特定漫画设置页偏移
- 阅读体验明显提升

### Phase 3：远程源接入
顺序：
1. WebDAV
2. SMB
3. OPDS

范围：
- 添加远程源
- 浏览目录
- 拉取封面
- 按需读取
- 简单缓存
- 基础重试

验收：
- 能连接并浏览远程库
- 能打开远程漫画阅读
- 非整本下载
- 断网有明确反馈

### Phase 4：大书架能力
范围：
- 快照索引
- 全局搜索
- 标签
- 阅读状态
- 卷号排序
- 后台刷新

验收：
- 大书库可用
- 搜索秒级返回
- 刷新不阻塞阅读

### Phase 5：高级功能
可选：
- RAR / 加密包
- PDF
- EPUB(纯图片)
- AI 增强
- 隐私模式
- 蓝牙键盘 / 手柄
- 多端同步

---

## 10. 性能与稳定性要求

### 10.1 必须满足
- 阅读器页面切换不明显掉帧
- 打开大体积 CBZ 不崩溃
- Bitmap 不出现频繁 OOM
- 预加载不抢占主线程
- 进度存储可靠

### 10.2 实现原则
- 所有 IO 放在后台线程
- 图片按需解码，避免原尺寸无脑加载
- 限制预加载页数
- 缓存采用 LRU
- 解码失败可跳过并提示
- 对损坏文件有兜底策略

---

## 11. 错误处理策略

统一错误类型：
- PermissionDenied
- FileNotFound
- UnsupportedFormat
- CorruptedArchive
- NetworkUnavailable
- AuthFailed
- Timeout
- Unknown

UI 策略：
- 可恢复错误尽量给重试按钮
- 读取失败的单页允许跳过
- 导入失败需明确告诉用户原因
- 权限失效需要引导重新授权

---

## 12. 测试策略

### 单元测试
- ZIP / CBZ 页面枚举
- 阅读进度读写
- 排序和过滤
- 页码偏移逻辑
- 白边裁切核心算法

### 集成测试
- SAF 导入目录
- 导入单文件
- 打开阅读器
- 退出恢复进度

### UI 测试
- 书架列表展示
- 点击封面进入阅读器
- 切换阅读模式
- 打开设置并保存

### 手工测试
- 超长漫画
- 大图漫画
- 文件名带中文
- 路径层级很深
- 损坏压缩包
- 权限被系统回收

---

## 13. 版本发布建议

### v0.1
工程骨架

### v0.2
本地阅读 MVP

### v0.3
阅读增强

### v0.4
WebDAV / SMB

### v0.5
搜索、快照、标签

### v0.6+
高级格式与差异化特性

---

## 14. 成功标准

项目成功不以“功能最多”为标准，而以以下指标判断：

1. 用户能稳定导入并阅读自己的漫画
2. 阅读器性能稳定，不轻易崩溃
3. 书架可维护，能找得到书
4. 后续能平滑扩展到远程协议和更多格式
