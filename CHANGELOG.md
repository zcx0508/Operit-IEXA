# Changelog

本文件记录 Operit · IEXA 视觉增强版分支。上游版本历史见 [README.upstream.md](README.upstream.md) 与[上游 Releases](https://github.com/AAswordman/Operit/releases)。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [v1.0.0-iexa] - 未发布

首个分支版本。界面改动为主，功能范围与上游一致。

### Added

- 新增 IEXA 组件文件 `ui/theme/IexaKitExtra.kt`，含 `IexaIconBadge`（渐变圆角图标块）、`IexaSegmentedControl`（弹性滑块分段切换）、`IexaColorSwatches`（选中框滑动选色）、`IexaAttachmentStrip`（可删除附件条）。
- 聊天页顶栏改为三行居中结构：`IEXA` / `◆ ● 默认模型 ▾` / `提供商 · 模型名`；第二行可直接打开模型选择弹层。
- 输入栏上方新增附件条，逐附件一个胶囊（按 MIME 类型的渐变图标 + 文件名 + ✕ 删除），数量角标实时同步。
- 模型选择弹层新增卡片分组（当前会话 / 提供商）、搜索胶囊、绿色勾选与默认徽标。
- 设置页新增 15 个入口，此前只能靠抽屉进入或不可达：记忆库、包管理、包市场、自定义表情、标签市场、工具箱、Shizuku 权限、终端、工作流、智能体、本地模型（MNN）、帮助、用户协议与隐私、更新日志、关于。
- 顶栏 ⚙ 按钮新增状态反馈：打开菜单时顺时针旋转 90° 并转主色，关闭时反向转回；菜单从齿轮下方展开。

### Changed

- `ThemePreferenceSnapshot.iexaPalette` 默认值改为 `true`，安装后即为 IEXA 外观（纯黑底、青色主色 `#4DD9D9`、卡片化列表），不再是可选主题。
- 顶栏容器色由 `surfaceContainer` 改为页面背景色，消除顶栏与内容区色差。
- 修复切页后顶栏残留上一个页面标题的问题：切页时清空旧标题。
- 顶栏右侧操作区精简为只保留 ⚙。
- 输入栏重构为单一胶囊：`+`（附件）/ `⚡`（快捷面板）/ 文本域 / `●`（语音 · 发送 · 停止）。
- 设置行图标改为 32dp 渐变圆角块 + 白色字形，配色按标题哈希稳定分配，同一功能永远同色。
- 设置页信息架构重排为 11 个分组：个性化 / AI 模型 / 提示词与角色 / 上下文 / 能力 / 运行 / 数据与权限 / 隐私 / 外部调用 / 远程连接 / 其它。
- 抽屉精简为 6 项：新对话 / 会话列表 / 聊天 / 设置 / 权限 / 关于。
- 动效统一：分段滑块、色块选中框、开关、面板选项统一使用 300–340ms 弹性曲线（spring），参数为 `dampingRatio = 0.72f`、`StiffnessMediumLow`。
- 附件角标口径明确：屏幕内容、引用记忆这类"引用"操作不计入附件数量。

### Removed

- 账号登录整体移除：设置页「账号」分组（GitHub 账号 / 登录 / 登出）。
- 移除 `Screen.GitHubAccount` 屏与 `navigateToGitHubAccount` 跳转，相关 import 一并清理。
- 包市场「发布产物」仍保留 GitHub 授权，仅在发布时触发，不受上述移除影响。

---

## 说明

- 本分支是 Operit 的社区二次修改，非上游官方版本。
- 许可证 LGPL-3.0-only，与上游一致；二次分发需保留同一许可证与上游声明。
- 功能范围未削减：多服务商对话、工具调用、扩展体系、长期记忆库、上下文压缩与摘要、TTS/ASR、工具箱 18 项、IEXA 远程与 DeepSeek Harness 远程均与上游一致。
