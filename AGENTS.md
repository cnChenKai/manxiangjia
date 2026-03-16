# AGENTS.md

## Scope

适用于仓库根目录及全部子目录。

## Mission

本仓库以 Android Debug APK 可持续产出为核心目标。所有代理（Codex/Jules/其他 vibe coding agents）都必须遵循：

1. 本地与 CI 构建路径一致
2. 问题定位有日志、有证据
3. 改动最小化、可回滚、可验证

---

## Required workflow (必须执行)

1. 先阅读：`README.md`、`.github/workflows/android.yml`、`settings.gradle.kts`、`gradle/libs.versions.toml`
2. 再修改：只修复与当前目标直接相关的问题
3. 每次关键修改后至少执行：
   - `./gradlew :data-local:compileDebugKotlin --stacktrace`
4. 最终必须执行：
   - `./gradlew assembleDebug --stacktrace`
5. 必须确认 APK 实际存在：
   - `*/build/outputs/apk/debug/*.apk`

---

## Vibe coding guardrails

### 1) 根目录清洁规则

禁止在仓库根目录新增或遗留以下临时文件：

- `patch_*.py`
- `tmp_*.py`
- `test_*.py`（非正式测试目录）
- 一次性调试文本（例如 `*_check.txt`）

临时脚本请放到：

- `scripts/`（可复用）
- `docs/`（说明文档）

### 2) 禁止投机性修复

不要用以下方式“骗过编译”：

- 注释掉大段业务代码
- 删除 `override` 规避签名问题
- 写 fake repository / fake implementation 混过构建
- 大规模重构与任务无关代码

### 3) 输出要求

最终说明必须包含：

- 根因摘要
- 修改文件列表
- 每处修改的必要性
- 执行过的验证命令
- APK 实际路径

---

## CI alignment

- CI 必须使用 Gradle Wrapper（`./gradlew`）
- CI 构建入口优先复用仓库脚本：`./scripts/ci/build_debug.sh`
- 成功时上传 APK；失败时上传构建日志（`build-logs/` + Gradle reports）

---

## Runtime error observability

为支持“只靠语言描述 bug”场景，代理修改涉及稳定性时应优先保证：

1. 未捕获异常可落盘（崩溃日志）
2. 日志可从设备导出并归档
3. 日志字段至少包含：时间、线程、异常消息、堆栈

---

## Preferred PR title

```text
fix(android): restore debug APK build pipeline
```
