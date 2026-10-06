# 第一阶段 AI Coding 执行计划

## 1. 使用方式

本文件是第一阶段的执行计划，不替代产品验收条件。行为以 [lockscreen-prototype-acceptance.md](lockscreen-prototype-acceptance.md) 为准；本文件只记录当前任务、依赖、验证证据和停止条件。

每次只展开一张任务卡，遵循：

```text
ready -> implement -> verify -> done
```

任务完成必须有命令退出码、关键输出或设备现场记录。缺少证据时只能标记为 `implemented-unverified` 或 `blocked`，不能标记为 `done`。

计划不把“代码看起来正确”当作验证，也不把主机上的构建结果当作荣耀手机验收结果。行为规格发生变化时，先修改验收文档，再修改任务卡和代码；不能在实现任务中顺手改变验收标准。

当前状态：C1/C1-R1 已在红米 `23013RK75C` 上完成核心页面和系统时间变化观察；C4 的锁屏代码已实现，但这台红米的锁屏主路径未通过，荣耀结果仍未测试。C2 等待 release keystore 保管/备份方案，C5 等待荣耀手机现场。C6 不创建。

红米 `23013RK75C`、Android 14（API 34）、构建 `V816.0.16.0.UMNCNXM` 已完成 debug APK 安装和现场观察：直接进入三行页面，竖屏保持锁定，前后台返回后时间正确；系统字体调大再调回后，固定字号不变化；修改时间、日期、时区后返回页面均立即按新值显示，测试结束后已恢复原设置且页面同步恢复。该机点亮后、解锁前未看到 App 页面，记录为这台设备的锁屏主路径未通过，不代表荣耀结果，也不据此创建通知。

C2 已完成主机侧 release 构建：正式签名文件位于仓库外的 `仓库外的 release 签名文件`，密码文件与签名文件权限均为仅当前用户可读写，用户已确认复制到另一存储位置。`app-release.apk` 的包名为 `com.anxinkan.app`，`minSdk` 为 31，`targetSdk` 为 36，版本 `0.1.0`，唯一 launcher 为 `MainActivity`，v2 签名验证通过，不是 debuggable，且没有网络、通知、开机或唤醒权限。该 APK 尚未安装到荣耀手机。

2026-10-04 决定：暂停锁屏后续设计。红米设备上的锁屏主路径未通过，但不再为它添加通知、点亮屏幕、后台拉起或其他绕过方案；荣耀现场如果以后需要，再单独重新评估。当前可交付行为以解锁后打开的核心大字页面为准。

## 2. 冻结约定

- `applicationId`: `com.anxinkan.app`。
- `minSdk`: 31，最低支持 Android 12。
- `compileSdk`: 36。
- `targetSdk`: 36。
- UI：Kotlin + Jetpack Compose，单模块、单 Activity、竖屏。
- 第一份核心页面：纯离线、无运行时权限、无网络权限、无账号、无后端。
- 第一份核心页面不包含通知、开机广播、后台拉起、自动点亮屏幕、常亮、前台服务、全屏通知或测试入口。
- debug APK 只用于本地和临时设备验证，不是交付物。
- release 密钥库延后到需要生成可长期覆盖更新的 APK 之前；密钥库放在仓库外，未明确保管和备份方案前不生成。
- 第一阶段不引入 Room、DataStore、网络客户端、导航框架或其他未来功能的空模块。

## 3.1 工具链前置证据

已确认 `专用 Build-Tools 36.0.0 目录中的 aapt`、`aapt2` 和 `apksigner` 均为可执行文件；其中 `aapt`、`aapt2` 是 arm64/x86_64 Mach-O，`apksigner` 是可执行 shell 启动脚本。任务卡使用绝对路径，不依赖当前 shell 是否加载 `PATH`。

## 3. 证据等级

### 主机证据

可以证明纯函数、格式化、测试、Gradle 构建、Manifest、APK 权限、包名、targetSdk、签名和 debuggable 状态。不能证明字体可辨认、首帧肉眼速度、荣耀安全区或锁屏行为。

### 开发设备证据

可以证明指定开发设备上的安装、启动、前后台恢复和可见布局。不能替代荣耀手机现场验收。

### 荣耀现场证据

必须由实际荣耀手机和亲人使用记录产生。包括型号、MagicOS、Android 版本、字体/显示大小、页面辨认、锁屏点亮、重启后行为和后续反馈。

## 4. 任务卡

### C1：离线核心大字页

**结果**

创建最小 Android 工程。唯一 Activity 启动后直接显示当前系统时间的 `yyyy-MM-dd`、24 小时制 `HH:mm` 和固定中文 `星期 N`；星期一到星期日固定映射为 1 到 7。页面前台运行时更新分钟，回到前台时立即使用新的系统时间。

**明确不做**

不做 `setShowWhenLocked`、`setTurnScreenOn`、通知、开机接收器、后台服务、第二个 Activity、Room、网络库、账号、测试按钮、模拟日期或横屏布局。不按架构笔记建立 LockScreen 类树。

**依赖**

无。工程使用已安装的 JDK 21、SDK Platform 36 和 Build-Tools 36.0.0。

**文件边界**

允许创建工程根目录的 Gradle/settings 文件、`app` 模块源码、`app/src/test` 测试和必要的资源；不创建后端目录、空模块或未来功能接口。

**验证**

- `./gradlew :app:testDebugUnitTest`：覆盖七天映射、补零、24 小时制、无秒/上午下午、Locale 和周起始日不改变映射、跨午夜更新、恢复前台后使用新时间。
- `./gradlew :app:assembleDebug`：退出码为 0。
- `专用 Build-Tools 36.0.0 目录中的 aapt dump badging <debug-apk>`：检查包名、minSdk、targetSdk 和唯一 launcher Activity。
- `专用 Build-Tools 36.0.0 目录中的 aapt dump permissions <debug-apk>`：检查权限列表。

**禁止声称**

不能声称首帧不超过 1 秒、字号在荣耀手机上不重叠、字体调大后仍清晰、锁屏可见或老人已经能辨认。

**停止条件**

Gradle/AGP 与 JDK 21 不兼容时停止；不改用 JBR、不换第三方镜像、不手工拼 APK。为了测试而添加调试入口、网络权限或模拟器时停止并重新评估任务边界。
**完成证据（已取得的主机证据）**

- `./gradlew --no-daemon :app:testDebugUnitTest`：`BUILD SUCCESSFUL`，纯时间模型测试通过。
- `./gradlew --no-daemon :app:assembleDebug`：`BUILD SUCCESSFUL`，生成 `app/build/outputs/apk/debug/app-debug.apk`。
- `aapt dump badging`：包名 `com.anxinkan.app`，`sdkVersion 31`，`targetSdkVersion 36`，唯一 launcher 为 `com.anxinkan.app.MainActivity`，声明竖屏。
- `aapt dump permissions`：没有 `INTERNET`、`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED` 或 `WAKE_LOCK`；AndroidX 生成的 `com.anxinkan.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` 是内部非导出接收器权限，不是运行时权限。
- 以上证据不能证明 Activity 已响应系统时间、日期或时区变化；C1-R1 完成前不把 C1 标记为 `done`。构建期间 SDK 自动补装了 Build-Tools `35.0.0`，已保留为工具链新增记录。

### C1-R1：系统时间与时区事件刷新

**结果**

修正核心页面，使其在 Activity 处于 `STARTED` 时响应 `TIME_CHANGED`、`DATE_CHANGED` 和 `TIMEZONE_CHANGED`；每次事件重新读取当前系统时间和默认时区。回到前台时也必须立即重算，不等待分钟节拍。分钟更新仍只在前台运行。

**明确不做**
不增加网络、权限、后台服务、开机接收器、锁屏、通知、保持常亮或测试入口；不把主机测试表述为真机验证。动态 receiver 只处理 `Intent.ACTION_TIME_CHANGED`（系统实际 action 为 `android.intent.action.TIME_SET`）、`Intent.ACTION_DATE_CHANGED` 和 `Intent.ACTION_TIMEZONE_CHANGED`，不跨越 `STOPPED` 状态监听。

**依赖**

依赖 C1 已有核心页面、纯时间模型和 debug 工程；不依赖 release keystore 或 Android 设备。

**文件边界**
只修改现有时间模型、Activity/Compose 生命周期刷新代码和 `app/src/test` 测试；允许在 Activity `STARTED` 期间注册匿名动态 `BroadcastReceiver`，并在 `ON_STOP` 注销；不新增 Manifest receiver、Activity、Service、其他组件或后端目录。

**验收条件**

- 手动改变时间后，页面在前台收到 `TIME_CHANGED` 并刷新，不等待下一分钟。
- 手动改变日期后，页面在前台收到 `DATE_CHANGED` 并刷新日期和星期。
- 手动改变时区后，页面在前台收到 `TIMEZONE_CHANGED`，重新读取默认 `ZoneId` 并刷新日期、时间和星期。
- 页面从停止状态恢复到前台时，使用恢复瞬间的新时间和新时区。
- 停止状态不通过后台轮询或唤醒维持页面更新。

**验证命令**

- `./gradlew --no-daemon :app:testDebugUnitTest`：纯时间模型覆盖七天映射、时区、跨午夜；可测试的刷新状态逻辑覆盖时间/日期/时区事件和前台恢复。
- `./gradlew --no-daemon :app:assembleDebug`：退出码为 0。
- 重新执行绝对路径 `aapt dump badging` 和 `aapt dump permissions`，确认本卡没有引入权限或额外组件。

**完成证据**

命令退出码、关键测试结果和 APK 审计输出；没有设备时仍不能声称字体、首帧速度或锁屏行为通过。

**停止条件**

如果只能通过后台服务、常驻轮询、额外权限或系统自动拉起实现，停止并重新评估验收范围；不要绕过系统限制。
**Reviewer 证据（Grok，条件通过）**

- 无必须修正项：动态 receiver 只在 `STARTED` 注册、`ON_STOP` 注销；只处理三个系统 action；每次刷新读取新的 `Instant.now()` 和 `ZoneId.systemDefault()`；Channel 事件会打断本地分钟等待；停止态不继续轮询。
- 8 个 JVM 用例通过，覆盖七天、补零、24 小时、Locale、跨午夜、本地分钟边界、全部刷新触发原因、停止态和新时区输出。
- 合并 Manifest 中的 `ProfileInstallReceiver`、`PreviewActivity` 和 AndroidX 内部权限来自依赖，不是本卡新增；本卡未新增 Manifest receiver 或应用权限。
- 条件：没有真实设备记录，不能声称系统广播已在设备收到，不能声称字体、首帧、锁屏或荣耀通过；C1 和 C1-R1 保持 `implemented-unverified`。

### C2：可长期覆盖安装的 release APK

**结果**

在 release keystore 的保管和备份方案明确后，用固定签名生成非 debuggable 的 release APK；包名、版本号和权限符合第一阶段约定。此卡不阻塞 C1 的 JVM 测试或 debug 构建。

**明确不做**

不在密钥方案未确定时生成 keystore；不把 keystore、密码或私钥写入仓库；不把 release APK 发给亲人，直到 C5 的现场准备条件满足。

**依赖**

依赖 C1 的核心页面和 debug 构建证据；release keystore 的位置、alias、密码保管和备份方式必须先由用户确定。

**文件边界**

允许修改 release 构建配置和版本元数据；keystore 必须位于仓库外。不得修改核心页面行为、增加权限或引入后续功能。

**验证**

- `./gradlew :app:assembleRelease`：退出码为 0。
- `专用 Build-Tools 36.0.0 目录中的 apksigner verify <release-apk>`：签名验证通过。
- `专用 Build-Tools 36.0.0 目录中的 aapt dump badging <release-apk>`：包名为 `com.anxinkan.app`，`debuggable` 不为 true，版本信息存在。
- `专用 Build-Tools 36.0.0 目录中的 aapt dump permissions <release-apk>`：没有网络、通知、开机、唤醒或其他未授权权限。

**禁止声称**

不能声称手机已经安装成功、荣耀手机通过或亲人能辨认。

**停止条件**

密钥保管方案未确定、签名验证失败、release 仍可调试、包名改变或出现额外权限时停止；不为了通过构建把密钥提交到 Git。

### C3：开发设备核心页冒烟

**结果**

在实际可用的 Android 设备上安装 debug APK，观察启动、前后台恢复、分钟更新、竖屏布局和字体放大一级后的完整性。

**明确不做**

不测试锁屏主路径，不修改亲人手机，不加调试面板，不为了开发机测试引入通知或后台机制。

**依赖**

依赖 C1；不依赖 release keystore。没有设备时可以保持 `blocked`，不阻塞没有设备依赖的后续主机审计。

**文件边界**

不改仓库代码；只产生设备安装状态和本次冒烟记录。代码缺陷必须回到新的实现卡。

**验证**

记录设备型号、Android 版本、字体与显示大小、安装退出码和可观察结果。没有开发设备时标记 `blocked`，不能用代码阅读或构建成功替代设备证据。

**禁止声称**

不能声称荣耀手机通过、亲人能辨认、锁屏可见或首帧满足 1 秒要求。

**停止条件**

发现核心页面缺陷时回到新的修正卡；不在本卡顺手加入锁屏或通知。

### C4：同一 Activity 的锁屏显示验证

**结果**

在核心页面稳定后，只在同一个 Activity 上增加 `setShowWhenLocked(true)`，并按验收文档的安全锁流程记录系统允许或拒绝的结果。

**明确不做**

不加 `setTurnScreenOn`、常亮、唤醒锁、悬浮窗、无障碍、全屏 Intent、`BOOT_COMPLETED`、前台服务、请求解除锁屏或第二个页面。

**依赖**

依赖 C1 的核心页面证据；如果要交给亲人测试，还依赖 C2 的 release APK 证据。C3 不是锁屏结论的必要条件。

**文件边界**

只允许修改承载核心页面的既有 Activity 及其必要 Manifest 属性；不新增 Activity、Service、Receiver 或通知相关文件。

**验证**

有安全锁的设备上，打开页面、熄屏、用户主动点亮，在输入凭据前观察页面；如果系统先显示系统锁屏，记录为设备限制。无安全锁只能记录为“仅熄屏测试”，不能算完整锁屏验收。主机只能审计新权限和组件清单，不能证明锁屏结果。

**禁止声称**

不能把解锁后重新打开 App 的结果替代锁屏检查，也不能把一台设备的结果推广到所有厂商。

**停止条件**

系统拒绝时记录并停止，不通过增加后台拉起、无障碍或悬浮窗绕过限制。
**完成证据（主机与 reviewer）**

- `./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug`：`BUILD SUCCESSFUL`。
- `aapt dump badging`：包名 `com.anxinkan.app`、`sdkVersion 31`、`targetSdkVersion 36`、唯一 launcher 为 `com.anxinkan.app.MainActivity`、debug APK 可调试。
- `aapt dump permissions`：没有 `INTERNET`、`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED` 或 `WAKE_LOCK`；仅有 AndroidX 内部非导出接收器权限。
- Grok reviewer：有条件通过，无必须修正项；确认 `setShowWhenLocked(true)` 只请求窗口在锁屏上显示，不会点亮屏幕、保持常亮、解除锁屏或后台拉起。
- 未取得安全锁真机证据，因此不能标记 `done`，不能声称锁屏可见；无安全锁设备只能记为“仅熄屏测试”。

### C5：荣耀现场记录

**结果**

由人在实际荣耀手机上按验收文档记录型号、MagicOS、Android 版本、字体/显示大小、安装、核心页面、亲人辨认、锁屏点亮、再次点亮和重启后的行为。

**明确不做**

不在现场临时添加权限、通知、保活、后台拉起或测试入口；不把照护者的判断代替亲人辨认；不把现场修改系统日期作为唯一的星期逻辑证据。

**依赖**

依赖 C2 的 release APK 和 C4 的锁屏实现；这是用户现场记录，不是 agent 可以自行完成的实现卡。

**文件边界**

不修改代码或设备配置；只产生由人填写的现场记录，计划文件只能在收到记录后追加证据。

**验证**

现场记录是唯一可以确认荣耀行为、可辨认性和真实使用结果的证据。低于 Android 12 时停止，不临时降低 `minSdk`。

**禁止声称**

工程师不能仅凭 debug 构建、JVM 测试或自己的观察把本卡标记为完成。

**停止条件**

发现问题只记录并创建新的修正卡；现场不改变产品边界。


### C6：通知降级（触发式，当前不创建）

只有同时满足以下条件才创建：C4 或 C5 已记录锁屏主路径未通过；真实用户仍需要通知入口；先修改验收文档，解决通知权限不能污染核心启动路径的问题。

在这些条件满足前，不声明通知权限、不建空类、不建通知渠道、不写降级代码。

## 5. 完成定义

### 工程完成

C1 和 C2 有主机命令证据；核心 APK 权限和组件审计通过；C3/C4 如果没有设备则明确标记为 `blocked` 或 `implemented-unverified`；仓库没有通知、开机或后台拉起代码。

这不等于第一阶段完成。

### 第一阶段现场完成

只有实际荣耀手机完成验收文档中核心页面条款，并且锁屏结果被单独记录后，才能称为第一阶段现场验收完成。锁屏失败可以作为系统限制记录，不阻止解锁后核心页面通过；通知未实现也不阻止核心页面通过。

## 6. 执行规则

- 一次只展开一张卡；前卡完成或明确阻塞后，才开始下一张依赖卡。
- 任务卡不预先写死实现文件名；实现时根据工程结构决定，避免计划反过来规定代码架构。
- 每张卡完成后只追加证据，不改写原验收标准。
- 发现新问题开新修正卡，不把旧卡的通过条件改成适应当前实现。
- 任何“主机做不到”的结论必须保留为未验证，不能用推断填补。
- 联系人、拨号和大语言模型不进入当前天气实现。天气 Worker 是单独的下一阶段，不把 Open-Meteo 地址写进 App。
## 7. 依据

- [OpenAI Codex workflow guidance](https://developers.openai.com/cookbook/articles/codex_exec_plans)
- [Anthropic：Claude Code best practices](https://www.anthropic.com/engineering/claude-code-best-practices)
- [GitHub：Copilot coding agent task guidance](https://docs.github.com/en/copilot/tutorials/cloud-agent/get-the-best-results)
- [Android：Gemini in Android Studio best practices](https://developer.android.com/studio/gemini/best-practices)
- [Android：Compose testing APIs](https://developer.android.com/develop/ui/compose/testing/apis)

## 8. 天气阶段任务卡

天气阶段不把语音设备实测当作数据功能的前置条件，但每张卡仍遵循 `ready -> implement -> verify -> Grok review -> done`。

### W1：Cloudflare Worker 天气契约与本地服务

- **结果**：`worker/src/index.mjs` 提供 `/weather?latitude=&longitude=&days=3|5`；调用 Open-Meteo，按查询地点时区生成稳定 JSON；本地 HTTP 服务可运行并接受不同经纬度。
- **明确不做**：不部署 Cloudflare；不让 Android 直连 Open-Meteo；不加入账号、数据库或 TTS。
- **验证**：纯函数测试；handler 注入 fetch 的参数和响应测试；本地 HTTP 服务使用真实上游分别请求至少两个经纬度，检查 timezone、日期、星期、天气类型、温度和 speech 字段。
- **未验证边界**：Cloudflare 部署后的域名、国内手机到 Worker 的链路未验证。
- **停止条件**：契约不稳定、上游响应字段缺失或本地真实请求失败时，不接 Android。
**W1 实际证据与状态**

- Worker 纯函数和 handler 测试通过：覆盖参数校验、一次上游调用、`timezone=auto`、daily 字段、3 天、400/404/502 和上游异常。
- 本地模拟 Open-Meteo + 本地 Worker HTTP 服务已用上海 `31.2304,121.4737` 与洛杉矶 `34.0522,-118.2437` 请求验证；两次返回的时区、当天排序、天气类型、温度和 speech 均与坐标不同，`days[0]` 与 speech 的当天一致。
- 曾发现并修复 `speech` 与 `days` 排序不一致的问题，并补回归测试。
- 真实 `https://api.open-meteo.com` 请求在当前环境超时；Cloudflare 尚未部署。因此 W1 状态为 `implemented-unverified`，不能开始依赖它的 W2。
- Grok reviewer：修复后的 handler 契约无必须修正项，但因真实上游未验证，不能标记 `done`；W2 暂停。

### W2：Android 定位与单页天气展示

- **结果**：时钟三行先显示；同一页面异步追加天气行；同时请求精确和大致定位权限；仅精确授权后使用 GPS；App 只调用 Worker URL；无天气时不遮挡时钟。
- **明确不做**：不直连 Open-Meteo；不做手动城市；不做第二页面；不接 TTS 自动播放。
- **验证**：JVM 契约/分类测试、debug 构建、Manifest 权限审计；Worker URL 未部署时只标记联网未验证，不称天气功能完成。
- **未验证边界**：Android 真机定位、权限弹窗、页面布局、真实 Worker 访问。
- **停止条件**：缺少精确位置时不能伪造天气；天气失败不能遮挡时钟。

### W3：部署前 release 门禁

- **结果**：release 构建必须注入已部署的 HTTPS Worker URL；没有 URL 时构建失败；签名、包名、权限和 URL 配置可审计。
- **明确不做**：不自动部署、不提交密钥、不把 Worker URL 写死为未验证地址。
- **验证**：空 URL 负向构建失败；HTTPS URL 正向配置构建成功；APK 权限和签名审计。
- **未验证边界**：Cloudflare 真实部署和手机端到端请求需用户执行。

### W4：中文 TTS（后续独立卡）

- **结果**：单独实现 TextToSpeech 生命周期、中文语言可用性检查和释放。
- **明确不做**：不阻塞 W1-W3；不把屏幕文字当成不识字用户的语音降级。
- **验证**：代码/JVM 可测部分；红米和荣耀真机实际朗读记录。

**W3 当前证据**

- 未设置 `WEATHER_WORKER_BASE_URL` 执行 `:app:assembleRelease`，任务在 `preReleaseBuild` 失败，退出码为 1，错误为“Release 构建必须设置 WEATHER_WORKER_BASE_URL 为已部署的 https Worker 地址”。
- 该证据只证明空 URL 不会生成天气版 release；正向 HTTPS 构建、APK 审计和 Cloudflare 真实链路仍未验证。
 - Grok reviewer：负向门禁结论充分、无必须修正项；W3 状态为 `implemented-unverified`。debug/JVM 不受空 URL 门禁影响，但正向 HTTPS 构建和 APK 审计未完成。
 - 注意：当前门禁只检查 `https://` 前缀，不能证明地址已部署或属于本项目 Worker；正向构建只能使用用户部署后提供的最终 HTTPS Worker URL。
