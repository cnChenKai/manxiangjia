# Pipeline redesign notes

## 目标

将原先“手工 debug → 手工编译 → 手工下载 APK → 口述反馈错误”的流程，收敛为统一可复现流水线。

## 统一入口

- 本地：`./scripts/ci/build_debug.sh`
- CI：`.github/workflows/android.yml` 中调用同一脚本

## 标准产物

成功：

- `*/build/outputs/apk/debug/*.apk`

失败：

- `build-logs/compileDebugKotlin.log`
- `build-logs/assembleDebug.log`
- `**/build/reports/`

## 运行时错误回传

应用内未捕获异常会落盘到：

- `files/crash-logs/*.log`

导出方式：

```bash
./scripts/ci/export_crash_logs.sh com.mangahaven.app
```

## 面向 agent 的执行要求

1. 不允许只改 CI 不改本地命令
2. 不允许只做“口头分析”不产出可执行脚本/文档
3. 任何新增流程都必须在 README 和 AGENTS.md 同步
