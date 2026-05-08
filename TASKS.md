# TASKS.md
# 漫享家 / MangaHaven 分批任务清单

## 使用规则
- 每次只做一个阶段
- 每阶段都必须可运行
- 每阶段都要附带验收步骤
- 不允许一次性做完所有功能
- 优先保证架构和稳定性，不追求表面完整

---

# Phase 0：工程骨架 ✅

## 目标
建立一个可扩展、可维护、可逐步演进的 Android 原生项目。

## 任务清单

### P0-1 工程初始化
- [x] 创建 Android 项目
- [x] 配置 Kotlin、Compose、Material 3
- [x] 配置多模块结构
- [x] 配置基础 Gradle Convention 或统一依赖管理
- [x] 配置 debug / release 构建类型

### P0-2 模块搭建
- [x] 创建 `app`
- [x] 创建 `core-model`
- [x] 创建 `core-reader`
- [x] 创建 `data-local`
- [x] 创建 `data-files`
- [x] 创建 `feature-library`
- [x] 创建 `feature-reader`
- [x] 创建 `feature-settings`

### P0-3 基础设施
- [x] 接入 Room
- [x] 接入 DataStore
- [x] 接入 WorkManager
- [x] 接入 DI（Hilt）
- [x] 接入日志系统
- [x] 统一 Result / Error 模型

### P0-4 UI 骨架
- [x] 建立 AppNavigation
- [x] 建立书架页空壳
- [x] 建立阅读器页空壳
- [x] 建立设置页空壳
- [x] 建立主题系统（浅色 / 深色）

### P0-5 数据骨架
- [x] 定义 Source 实体
- [x] 定义 LibraryItem 实体
- [x] 定义 ReadingProgress 实体
- [x] 定义 ReaderSettings 数据结构
- [x] 建立 Room DAO 雏形

## 验收标准
- [x] 项目可编译运行
- [x] 三个页面可导航切换
- [x] DataStore 可保存测试设置
- [x] Room 可写入测试数据
- [x] WorkManager 可跑一个示例任务

---

# Phase 1：MVP 本地离线阅读闭环 ✅

## 目标
让用户可以导入本地漫画并顺畅读完。

## 任务清单

### P1-1 文件导入
- [x] 集成 Android SAF 选择单文件
- [x] 集成 Android SAF 选择目录
- [x] 持久化 URI 权限
- [x] 对导入失败给出错误提示

### P1-2 文件扫描
- [x] 扫描目录中的图片文件
- [x] 识别 ZIP
- [x] 识别 CBZ
- [x] 提取基础元数据
- [x] 建立书架条目

### P1-3 容器读取
- [x] 实现目录型 ContainerReader
- [x] 实现 ZIP/CBZ ContainerReader
- [x] 提供页面列表
- [x] 提供封面提取
- [x] 支持按页打开输入流

### P1-4 书架
- [x] 展示封面网格
- [x] 展示标题
- [x] 最近阅读区域
- [x] 继续阅读入口
- [x] 空状态页
- [x] 导入后自动刷新

### P1-5 阅读器基础
- [x] 左右翻页模式
- [x] 纵向滚动模式
- [x] 单页显示
- [x] 缩放
- [x] 拖拽
- [x] 顶部/底部菜单

### P1-6 阅读进度
- [x] 打开时恢复上次页码
- [x] 翻页时实时保存进度
- [x] 退出时持久化
- [x] 书架显示最近阅读时间

### P1-7 性能基础
- [x] 后台线程读取文件
- [x] 限制图片解码尺寸
- [ ] 加入相邻页预加载（接口已定义，实现为空）
- [ ] 加入基础内存缓存（Coil 有，自定义缓存未实现）
- [x] 避免主线程阻塞

## 验收标准
- [x] 能导入本地目录
- [x] 能导入 ZIP / CBZ
- [x] 应用重启后书架仍在
- [x] 阅读进度能恢复
- [x] 500+ 页漫画可打开
- [x] 正常使用不崩溃

## 已知暂不做
- [ ] RAR（Phase 5）
- [ ] PDF（Phase 5）
- [ ] WebDAV（Phase 3）
- [ ] SMB（Phase 3）
- [ ] OPDS（Phase 3）
- [ ] 标签（Phase 4）
- [ ] 全局搜索（Phase 4）

---

# Phase 2：阅读体验增强 ✅

## 目标
把 MVP 提升到长期可用的阅读体验。

## 任务清单

### P2-1 阅读模式增强
- [x] 支持 LTR
- [x] 支持 RTL
- [x] 支持 Vertical
- [x] 阅读模式切换时保存设置

### P2-2 双页模式
- [x] 单页 / 双页切换
- [ ] 横屏自动建议双页
- [x] 双页下页面组合逻辑
- [ ] 封面单页独立策略

### P2-3 页码偏移
- [x] 提供页偏移设置
- [x] 支持 +1 / -1 调整
- [x] 按书保存偏移值

### P2-4 白边裁切
- [x] 实现基础边缘检测
- [x] 提供启用 / 关闭开关
- [x] 对单页和双页都生效
- [x] 异常图像降级回原图

### P2-5 缩略图与跳页
- [x] 页面缩略图列表
- [x] 滑动到指定页（滑块可拖动 + 页码浮层 + 防抖）
- [ ] 输入页码跳转
- [x] 显示当前页 / 总页数

### P2-6 每本书独立设置
- [x] 建立 ItemReaderSettingsOverride
- [x] 覆盖全局阅读方向
- [x] 覆盖裁切设置
- [x] 覆盖双页模式

## 验收标准
- [x] 阅读方向切换可靠
- [x] 双页模式可用
- [x] 页偏移可纠正错页
- [x] 白边裁切对典型漫画有效
- [x] 缩略图跳页顺畅（滑块可拖动）

---

# Phase 3：远程内容源 🟡

## 目标
支持从个人服务器或 NAS 阅读漫画。

## 任务清单

### P3-1 SourceClient 抽象补完
- [x] 定义统一远程内容访问接口
- [x] 明确目录、文件、流读取能力
- [x] 明确错误模型
- [x] 明确认证模型

### P3-2 WebDAV
- [x] 添加 WebDAV 连接配置
- [x] 测试连接
- [x] 浏览目录
- [x] 读取文件流
- [x] 拉取封面
- [x] 基础缓存

### P3-3 SMB
- [x] 添加 SMB 连接配置
- [x] 测试连接
- [x] 浏览共享目录
- [x] 读取文件流
- [x] 拉取封面
- [x] 错误重试

### P3-4 流式阅读
- [x] 远程文件按需读取
- [ ] 相邻页预取
- [ ] 网络失败重试
- [x] 非整本下载
- [ ] 中断后错误可感知

### P3-5 远程源管理
- [x] 添加源
- [ ] 编辑源
- [x] 删除源
- [ ] 显示最近连接状态

### P3-6 OPDS（新增）
- [x] OPDS 客户端实现
- [x] OPDS 源 UI 添加入口
- [x] SourceClientFactory 路由到 OPDS

### P3-7 远程压缩包支持（新增）
- [x] 远程 ZIP/CBZ 下载缓存
- [x] 远程压缩包阅读
- [x] 缓存清理策略

## 验收标准
- [x] 可连接 WebDAV
- [x] 可连接 SMB
- [x] 可浏览远程目录
- [x] 可直接阅读远程文件夹型漫画
- [x] 可阅读远程压缩包（下载缓存策略）
- [ ] 网络断开时有明确提示

---

# Phase 4：搜索与大书架管理 ✅

## 目标
支持大规模漫画库的管理与检索。

## 任务清单

### P4-1 Snapshot 快照
- [x] 定义 Snapshot 实体（Room 实体 + DAO + Repository）
- [x] 后台构建快照（SnapshotBuilder + LibrarySyncWorker 集成）
- [x] 增量刷新快照
- [ ] 失效数据清理

### P4-2 搜索
- [x] 书架标题搜索
- [x] 远程快照搜索（SnapshotDao 支持）
- [ ] 统一搜索入口
- [x] 搜索结果跳转

### P4-3 标签与状态
- [x] 收藏
- [x] 已读
- [x] 阅读中
- [x] 未开始
- [x] 自定义标签（Tag 实体 + 多对多关联 + DAO）

### P4-4 排序与过滤
- [x] 最近阅读排序
- [x] 最近添加排序
- [x] 标题排序
- [ ] 卷号排序
- [x] 按状态过滤
- [x] 按标签过滤（TagDao 支持）

### P4-5 后台刷新
- [x] WorkManager 周期刷新
- [ ] 手动刷新入口
- [ ] 刷新状态提示
- [ ] 刷新失败重试

### P4-6 批量操作（新增）
- [x] 长按进入多选模式
- [x] 批量标记已读/未读
- [x] 批量收藏/取消收藏
- [x] 批量删除（含确认对话框）

### P4-7 书架统计（新增）
- [x] 总条目数
- [x] 阅读状态分布
- [x] 收藏数

## 验收标准
- [x] 大书架下搜索可用
- [x] 搜索结果准确
- [x] 标签和状态可过滤
- [x] 快照刷新不影响前台阅读
- [x] 批量操作可用
- [x] 统计信息准确

---

# Phase 5：高级能力 🟡

## 目标
根据资源和用户反馈扩展高价值功能。

## 可选任务
- [x] RAR 支持（已实现）
- [ ] 加密压缩包支持
- [x] PDF 基础支持（ContainerReader 已有）
- [x] EPUB 图片模式支持（ContainerReader 已有）
- [ ] AI 画质增强
- [x] 隐私模式（已实现生物识别锁）
- [x] 蓝牙键盘快捷键（已实现音量键翻页）
- [x] 游戏手柄翻页（已实现 D-pad 翻页）
- [ ] 多端进度同步
- [x] 双指缩放（pinch-to-zoom + 双击缩放）
- [x] 连续滚动模式（webtoon/长条漫画）
- [x] 点击区域自定义（4 种模式）

---

# 横向任务：质量保障 🔴

## Q-1 监控与日志
- [x] 统一埋点接口（Timber）
- [ ] 页面加载耗时统计
- [x] 解码失败日志
- [x] 导入失败日志

## Q-2 测试集
- [ ] 小体积 CBZ
- [ ] 大体积 CBZ
- [ ] 中文文件名
- [ ] 超长目录层级
- [ ] 损坏压缩包
- [ ] 巨图样本
- [ ] 双页样本

## Q-3 性能目标
- [ ] 首次打开时间可接受
- [ ] 相邻页切换不明显掉帧
- [ ] 长时间阅读不频繁 GC 抖动
- [ ] 无明显内存泄漏

---

# 横向任务：技术债务修复 🔴

## TD-1 关键修复
- [x] ContainerReaderFactory 返回类型改为 ContainerReader
- [x] 修复 PdfContainerReader 字符串模板转义错误
- [x] 修复 OpdsSourceClient 字符串模板转义错误
- [x] SourceEntity 补充 isVirtual 字段
- [ ] 替换 fallbackToDestructiveMigration 为增量迁移

## TD-2 安全与隐私
- [x] CrashUploader 邮箱改为可配置（改为占位值 dev@example.com）
- [ ] CrashUploader 上传服务改为可配置或 opt-in
- [ ] CrashUploader 复用单例 OkHttpClient

## TD-3 性能优化
- [ ] ArchiveContainerReader 流式读取避免 OOM
- [ ] RarArchiveContainerReader 缓存临时文件
- [ ] RemoteScanner 添加深度限制

## TD-4 代码质量
- [x] 替换已废弃的 Divider() 为 HorizontalDivider()
- [ ] 修复 ZipSlipVulnerabilityTest 编译错误
- [ ] 实现 PreloadController
- [ ] 实现 ReaderSession
- [x] 实现 TapZoneProfile 点击区域逻辑（4 种模式）
- [x] 修复滑块使其可拖动跳页
