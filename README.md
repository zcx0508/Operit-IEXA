# Operit · IEXA 视觉增强版分支

> 上游项目：[AAswordman/Operit](https://github.com/AAswordman/Operit)　·　许可证：LGPL-3.0-only
> 上游原始 README 保留在 [README.upstream.md](README.upstream.md)

这是 Operit 的 **UI/UX 增强分支**。功能层面保持与上游一致，不阉割能力；改动集中在界面呈现与交互路径——把 IEXA 手机端视觉作为默认主体，装好即生效，而不是一个需要手动开启的可选主题。

本分支是社区二次修改，**不是上游官方版本**。功能、协议、数据格式的权威说明请以上游文档为准。

---

## 分支定位

| 维度 | 说明 |
|------|------|
| 是什么 | Operit Android 版的一个界面分支，代码库直接来自上游 |
| 改了什么 | 主题默认值、顶栏、输入栏、附件呈现、模型弹层、设置页信息架构、抽屉、动效与新增 UI 组件 |
| 没改什么 | 模型接入、工具调用、扩展体系、记忆库、远程连接等能力均未削减 |
| 适合谁 | 喜欢 IEXA 这种纯黑 + 青色的克制视觉、但需要 Operit 完整能力的人 |
| 不适合谁 | 需要与上游逐像素一致、或依赖上游官方发布节奏的人 |

稳定性判断：改动集中在 `ui/theme/` 与若干界面文件的呈现层，不触碰推理、工具执行与存储层逻辑。需要最保守选择时，请使用上游版本。

---

## 与原版对比

| 项目 | 上游 Operit | 本分支 |
|------|-------------|--------|
| 默认外观 | 可选主题，由用户选择 | IEXA 外观默认开启（`ThemePreferenceSnapshot.iexaPalette = true`） |
| 主色 / 底色 | 随所选主题 | 青色 `#4DD9D9` / 纯黑底 |
| 页面顶栏 | `surfaceContainer` 容器色，与页面背景存在色差 | 容器色等于页面背景色，切页时清空旧标题 |
| 聊天页顶栏 | 单行标题 | 三行居中信息（品牌 / 默认模型入口 / 提供商 · 模型名） |
| 顶栏操作区 | 多个图标 | 仅保留 ⚙，带 90° 旋转与主色反馈 |
| 输入区 | 多按钮布局 | 单一胶囊：`+` / `⚡` / 文本域 / `●` |
| 附件呈现 | 列表内展示 | 输入栏上方独立附件条，逐条可删 |
| 设置页结构 | 原有分组 | 重排为 11 个分组，新增 15 个可达入口 |
| 设置行图标 | 单色图标 | 32dp 渐变圆角块 + 白色字形，配色按标题哈希稳定分配 |
| 抽屉 | 完整功能入口 | 精简为 6 项 |
| 账号登录 | 有 | 已移除 |

---

## 改动清单

### 视觉

- `ThemePreferenceSnapshot.iexaPalette` 默认 `true`：安装后即为 IEXA 外观——纯黑底、青色主色 `#4DD9D9`、卡片化列表。
- 顶栏容器色改用页面背景色（原先为 `surfaceContainer`），消除顶栏与内容区之间的色差。
- 设置行图标改为 32dp 渐变圆角块 + 白色字形；配色由标题哈希稳定分配，同一功能每次进入颜色一致。

### 交互

- 聊天页三行居中顶栏：`IEXA`（15sp 粗体）/ `◆ ● 默认模型 ▾`（11.5sp，点击打开模型弹层）/ `提供商 · 模型名`（10.5sp）。
- 顶栏右侧只保留 ⚙：打开菜单时顺时针旋转 90° 并转为主色，关闭时反向转回；菜单从齿轮下方展开，不遮挡按钮。
- 输入栏改为单一胶囊，从左到右为 `+`（附件）、`⚡`（快捷面板）、文本域、`●`（语音 / 发送 / 停止）。
- 输入栏上方新增附件条：每个附件一个胶囊，带按 MIME 类型区分的渐变图标、文件名与 ✕ 删除；数量角标实时同步。屏幕内容、引用记忆这类"引用"操作不计入附件角标。
- 模型选择弹层：卡片分组（当前会话 / 提供商）+ 搜索胶囊 + 绿色勾选 + 默认徽标。
- 切页时清空旧标题，修复返回后残留上一个页面标题的问题。

### 结构

- 设置页重排为 11 个分组：个性化 / AI 模型 / 提示词与角色 / 上下文 / 能力 / 运行 / 数据与权限 / 隐私 / 外部调用 / 远程连接 / 其它。
- 设置页新增 15 个入口（此前只能靠抽屉进入或不可达）：记忆库、包管理、包市场、自定义表情、标签市场、工具箱、Shizuku 权限、终端、工作流、智能体、本地模型（MNN）、帮助、用户协议与隐私、更新日志、关于。
- 抽屉精简为：新对话 / 会话列表 / 聊天 / 设置 / 权限 / 关于。

### 组件

新增 `app/src/main/java/com/ai/assistance/operit/ui/theme/IexaKitExtra.kt`：

- `IexaIconBadge` —— 渐变圆角图标块
- `IexaSegmentedControl` —— 弹性滑块分段切换
- `IexaColorSwatches` —— 选中框滑动选色
- `IexaAttachmentStrip` —— 可删除附件条

动效统一：分段滑块、色块选中框、开关、面板选项均使用 300–340ms 弹性曲线（spring）。

### 移除

- 账号登录整体移除：设置页「账号」分组（GitHub 账号 / 登录 / 登出）、`Screen.GitHubAccount` 屏与 `navigateToGitHubAccount` 跳转、相关 import 一并删除。
- 包市场「发布产物」仍需要 GitHub 授权，只在发布时触发，与账号登录无关。

详细到文件路径的说明见 [docs/IEXA-视觉增强说明.md](docs/IEXA-视觉增强说明.md)，版本记录见 [CHANGELOG.md](CHANGELOG.md)。

---

## 能做什么

能力范围与上游一致，没有削减：

- **多服务商对话**：OpenAI、Anthropic、Google、DeepSeek、xAI、Ollama、MNN 本地模型
- **工具调用**：文件、终端、Shell、Shizuku、UI 自动化、屏幕、通知、剪贴板、网络
- **扩展体系**：包管理、包市场、技能、MCP、工作流、定时任务、自定义表情、标签市场
- **长期记忆库**：画像、偏好、日记
- **上下文管理**：压缩与摘要
- **语音**：TTS + ASR
- **工具箱**：18 项
- **远程连接**：IEXA 远程、DeepSeek Harness 远程

---

## 构建

前置：JDK 21 + Android SDK。

```bash
./gradlew :app:assembleRelease
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

上游构建流程中涉及的外部归档与子模块要求，请参考 [README.upstream.md](README.upstream.md) 与上游构建文档。本分支未修改构建脚本。

---

## 目录速览

与本次改动相关的部分：

```
app/src/main/java/com/ai/assistance/operit/
├── data/preferences/
│   └── ThemePreferenceSnapshot.kt      # iexaPalette 默认值
├── ui/theme/
│   ├── IexaColorScheme.kt              # IEXA 的 M3 ColorScheme（主色 #4DD9D9）
│   ├── IexaDesign.kt                   # IEXA 语义色板与布局规格
│   ├── IexaKit.kt                      # 开关、模型 pill、弹层外壳
│   ├── IexaKitExtra.kt                 # 图标块 / 分段控件 / 色块 / 附件条
│   ├── IexaInputBar.kt                 # 单一胶囊输入栏
│   └── IexaSheet.kt                    # 设置行、模型选择弹层等
├── ui/features/chat/components/
│   └── ChatScreenHeader.kt             # 聊天页三行顶栏
├── ui/features/settings/screens/
│   └── SettingsScreen.kt               # 11 分组信息架构
└── ui/main/components/
    └── DrawerContent.kt                # 抽屉精简
```

---

## 致谢与许可

本项目基于 [Operit](https://github.com/AAswordman/Operit)（作者 AAswordman 与 Operit 团队）二次开发，界面设计参考 IEXA 手机端视觉规范。上游作者的持续投入是这份代码存在的前提。

许可证：[GNU LGPL v3（LGPL-3.0-only）](LICENSE)，与上游一致。

二次分发要求：

- 保留同一许可证（LGPL-3.0-only）
- 保留上游声明与出处
- 说明本分支相对上游的改动

本分支是社区二改，非上游官方版本；不以上游名义发布，也不代表上游立场。
