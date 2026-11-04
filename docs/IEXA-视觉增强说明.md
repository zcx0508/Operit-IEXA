# IEXA 视觉增强说明

本文记录本分支相对上游 Operit 的全部界面改动，按「视觉层 / 交互层 / 结构层 / 组件层 / 移除项」五个部分展开，尽量落到具体文件。

范围声明：改动集中在 `app/src/main/java/com/ai/assistance/operit/ui/` 下的呈现层，以及 `data/preferences/ThemePreferenceSnapshot.kt` 的一个默认值。模型接入、工具执行、记忆库、扩展体系、远程连接的实现未做删减。

---

## 一、视觉层

### 1.1 默认主题：IEXA 成为主外观

**文件**：`app/src/main/java/com/ai/assistance/operit/data/preferences/ThemePreferenceSnapshot.kt`

```kotlin
val iexaPalette: Boolean get() = true
```

上游这一项是可选项，用户需要自己挑主题。本分支把它改成默认 `true`：装好即 IEXA 外观，纯黑底、青色主色 `#4DD9D9`、卡片化列表。

### 1.2 色板与配色方案

**文件**：`ui/theme/IexaColorScheme.kt`

把 IEXA 的配色映射成 Material 3 的 `ColorScheme`（36 字段）。深色模式的关键值：

| 角色 | 值 |
|------|-----|
| `primary` / `surfaceTint` | `#4DD9D9` |
| 页面底色 | `#000000` |
| 容器 | `#1C1C1E` |
| 高容器 | `#2C2C2E` |

**文件**：`ui/theme/IexaDesign.kt`

与上面那套 M3 `ColorScheme` 并行存在的，是 IEXA 自己的 10 个语义色（`IexaPalette`：`pageBackground` / `surface` / `surfaceAlt` / `primaryText` / `mutedText` / `accent` / `onAccent` / `accentDark` / `border` / `danger`）加布局规格。两套色板的分工：M3 那套喂给 Material 组件，`IexaPalette` 喂给本分支自己写的组件，通过 `LocalIexaPalette` 下发。

新增组件一律读 `LocalIexaPalette`，不直接引用 `MaterialTheme.colorScheme`，避免两套色板在同一屏上打架。

### 1.3 顶栏容器色与页面背景对齐

顶栏容器原本用 `surfaceContainer`，和页面背景之间存在可见色差，滚动时尤为明显。改为与页面背景同色后，顶栏与内容区融为一体，只靠留白和字重区分层级。

### 1.4 设置行图标：渐变圆角块

**文件**：`ui/theme/IexaKitExtra.kt`（`iexaBadgeColors()` / `IexaIconBadge`）

设置页每行左侧的图标，从单色矢量图标改为 32dp 的渐变圆角块 + 白色字形。共 12 套渐变，按行标题字符串做哈希（`h = h * 31 + c.code`，模 `99991`）稳定落到某一套。

选哈希而不是按索引，是为了让"同一个功能永远同色"——分组顺序调整、入口增删都不会让已有条目的颜色跳变。

附件条里的小图标复用同一个函数，`seed` 用文件名，因此不同类型的附件也会得到稳定且互不相同的颜色。

---

## 二、交互层

### 2.1 聊天页三行居中顶栏

**文件**：`ui/features/chat/screens/AIChatScreen.kt`（顶栏区，约 280–400 行）

上游是单行标题。本分支改成三行居中结构：

| 行 | 内容 | 字号 |
|----|------|------|
| 1 | `IEXA` | 15sp 粗体 |
| 2 | `◆ ● 默认模型 ▾` | 11.5sp，可点击，打开模型选择弹层 |
| 3 | `提供商 · 模型名` | 10.5sp |

第二行把"当前用哪个模型"从设置里提到了首屏可点位置：换模型不再需要先进设置页。

**同时修复**：切页时清空旧标题。上游在页面切换后顶栏会残留上一个页面的标题，原因是标题状态未被重置；现在每次切换都先置空再由新页面写入。

### 2.2 顶栏右侧只保留 ⚙

齿轮按钮带两段状态反馈：

- 打开菜单：顺时针旋转 90°，同时颜色转为主色
- 关闭菜单：反向转回，颜色恢复

菜单从齿轮正下方展开，而不是覆盖在按钮上方——避免展开瞬间把触发源盖住，用户失去"这里是开关"的视觉锚点。

### 2.3 输入栏：单一胶囊

**文件**：`ui/theme/IexaInputBar.kt`（`IexaChatInputBar`）

上游输入区是多个并列按钮。本分支收进一个 `RoundedCornerShape(28.dp)` 的胶囊里，从左到右：

| 控件 | 尺寸 | 动作 |
|------|------|------|
| `+` | 44dp 圆形 | 打开附件菜单 |
| `⚡` | 44dp 圆形 | 快捷面板（清空 / 压缩 / 记忆 / 思考） |
| 文本域 | 自适应 | 输入 |
| `●` | 48dp 主色圆 | 语音 / 发送 / 停止，按 `isProcessing` 与文本是否为空切换语义 |

保留 `attachmentCount` 参数，供角标使用。

### 2.4 附件条

**文件**：`ui/theme/IexaKitExtra.kt`（`IexaAttachmentStrip` / `IexaAttachmentChip`）

输入栏上方独立一条横向滚动的附件列表，每个附件一个胶囊：按 MIME 类型取渐变图标 + 文件名（`widthIn(max = 120.dp)`，单行省略）+ ✕ 删除。

原先附件信息混在列表/输入区里，看不到"现在到底挂了几个东西"。拆成独立一条后，增删是逐条可见、逐条可点的。

列表为空时直接 `return`，不占高度。

**角标口径**：屏幕内容、引用记忆这类"引用"操作不计入附件数量角标——它们不是用户主动添加的附件，计入会让数字虚高。

### 2.5 模型选择弹层

**文件**：`ui/theme/IexaSheet.kt`（`IexaModelSheet` / `IexaModelItem`）

卡片分组，分「当前会话」与「提供商」两组；顶部一个搜索胶囊；选中项显示绿色勾选；默认模型额外带一枚「默认」徽标。

分开"当前会话指定"和"按提供商浏览"，是因为这两件事在模型多的时候很容易混淆：前者是覆盖项，后者是选择池。

### 2.6 动效统一

分段滑块、色块选中框、开关、面板选项，统一使用 spring 曲线：

```kotlin
spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
```

落地时长约 300–340ms。上游这些控件各自用不同的动画参数，观感不一；统一后同一屏内的位移节奏一致。

---

## 三、结构层

### 3.1 设置页重排为 11 个分组

**文件**：`ui/features/settings/screens/SettingsScreen.kt`

| # | 分组 |
|---|------|
| 1 | 个性化 |
| 2 | AI 模型 |
| 3 | 提示词与角色 |
| 4 | 上下文 |
| 5 | 能力 |
| 6 | 运行 |
| 7 | 数据与权限 |
| 8 | 隐私 |
| 9 | 外部调用 |
| 10 | 远程连接 |
| 11 | 其它 |

分组逻辑从"按实现模块堆叠"改为"按用户的思考顺序"：先调外观，再配模型，然后才是上下文、能力、运行这些使用中才会关心的项；涉及数据与网络的项集中到后半段，不干扰日常配置。

### 3.2 新增 15 个可达入口

以下入口此前只能通过抽屉进入，或根本没有可达路径：

记忆库、包管理、包市场、自定义表情、标签市场、工具箱、Shizuku 权限、终端、工作流、智能体、本地模型（MNN）、帮助、用户协议与隐私、更新日志、关于。

抽屉的容量有限，塞不下这些；放回设置页后，功能发现性从"知道抽屉深处有"变成"翻设置就能看到"。

### 3.3 抽屉精简

**文件**：`ui/main/components/DrawerContent.kt`

从完整功能入口集合缩减为 6 项：

```
新对话 / 会话列表 / 聊天 / 设置 / 权限 / 关于
```

抽屉只承担"页面级跳转"，具体功能配置交给设置页。两级职责分开后，抽屉不再随功能增加而无限膨胀。

---

## 四、组件层

**文件**：`app/src/main/java/com/ai/assistance/operit/ui/theme/IexaKitExtra.kt`

四个新增组件，对应已定稿的交互稿：

### `IexaIconBadge`

渐变圆角图标块。`size` 默认 30dp，圆角为 `size * 0.3f`，图标白色（alpha 0.96）。设置行用 30dp / 16dp 图标，附件条用 18dp / 11dp 图标——同一组件两个尺度，不必为小尺寸另写一份。

### `IexaSegmentedControl`

分段切换（允许 / 询问 / 禁止，深色 / 浅色 / 跟随系统这类）。高亮滑块用 `animateFloatAsState` + spring 在两档之间滑过去，而不是淡入淡出。选中项字重 `SemiBold`、颜色 `primaryText`，未选中 `mutedText`。

### `IexaColorSwatches`

主色色块选择。选中框用 2dp `primaryText` 描边的圆角矩形，滑动到被选色块上，同时色块内 ✓ 显示。选中框是独立于色块层的 `Box`，靠 `offset` 位移，因此滑动过程不会带着色块一起动。

### `IexaAttachmentStrip`

见 2.4。用 `LazyRow` 实现，横向滚动，`itemsIndexed` 提供 `onRemove(index)`。

### 配套组件

**文件**：`ui/theme/IexaKit.kt`、`ui/theme/IexaSheet.kt`

- `IexaKit.kt`：开关、模型 pill、弹层外壳等基础件
- `IexaSheet.kt`：`SettingsRow`（设置行）、`NewChatPill`（新对话胶囊）、`IexaTopBar`（会话列表页顶栏：汉堡 + 标题 + 新对话 pill）、`IexaModelItem` / `IexaModelSheet`（模型选择）

---

## 五、移除项

### 5.1 账号登录整体移除

以下内容全部删除：

| 删除对象 | 说明 |
|----------|------|
| 设置页「账号」分组 | 含 GitHub 账号、登录、登出三项 |
| `Screen.GitHubAccount` | 路由枚举中的该项 |
| `navigateToGitHubAccount` | 跳转函数 |
| 相关 import | 随上述代码一并清理 |

`AIChatScreen.kt`、`SettingsScreen.kt` 与导航层中对该屏的引用均已断开；`ui/features/settings/screens/GitHubAccountScreen.kt` 文件本身仍留在仓库里，但已无调用点（`OperitScreens.kt` 中有对应注释说明该屏已移除）。

移除理由：本分支不做账号体系。少一个登录态，就少一处需要同步、需要处理过期、需要在断网时报错的地方。

### 5.2 保留：包市场发布授权

包市场「发布产物」仍然需要 GitHub 授权，但只在发布动作触发时才走，与已移除的账号登录无关。日常浏览、安装包不受影响。

---

## 附：改动文件索引

| 文件 | 改动性质 |
|------|----------|
| `data/preferences/ThemePreferenceSnapshot.kt` | `iexaPalette` 默认值改为 `true` |
| `ui/theme/IexaColorScheme.kt` | IEXA 的 M3 `ColorScheme` |
| `ui/theme/IexaDesign.kt` | 语义色板与布局规格 |
| `ui/theme/IexaKit.kt` | 基础件（开关 / pill / 弹层外壳） |
| `ui/theme/IexaKitExtra.kt` | 新增四组件 |
| `ui/theme/IexaInputBar.kt` | 单一胶囊输入栏 |
| `ui/theme/IexaSheet.kt` | 设置行 / 新对话 pill / 会话列表顶栏 / 模型弹层 |
| `ui/features/chat/screens/AIChatScreen.kt` | 三行顶栏、附件条接入、顶栏图标形态 |
| `ui/features/chat/components/ChatScreenHeader.kt` | 聊天页头部与 IEXA 判定 |
| `ui/features/settings/screens/SettingsScreen.kt` | 11 分组信息架构、15 个新入口 |
| `ui/main/components/DrawerContent.kt` | 抽屉精简为 6 项 |
| `ui/main/screens/OperitScreens.kt` | `Screen.GitHubAccount` 移除 |

---

本分支为社区二次修改，基于 [Operit](https://github.com/AAswordman/Operit)，许可证 LGPL-3.0-only。上游原始说明见 [../README.upstream.md](../README.upstream.md)。
