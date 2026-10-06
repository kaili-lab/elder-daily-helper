# 安心看：技术架构讨论记录

## 当前架构结论

采用“原生 Android + 轻量后端 + 分阶段验证”。当前最大的技术风险是 Android 锁屏显示能力，不是后端框架。应先验证锁屏原型，再决定联网服务和服务器部署细节。

```text
Android App
├── 锁屏大字显示
├── 时间、日期、星期
├── 天气查询
├── 语音输入与播报
├── 本地联系人
└── 拨号确认

        HTTPS

轻量后端
├── 天气服务适配
├── 语音服务适配
├── 大语言模型适配
├── 用户和设备配置
└── SQLite

        HTTPS

第三方服务
├── 天气 API
├── 语音识别 API
├── 语音合成 API
└── 大语言模型 API
```

## Android 技术栈

- Kotlin
- Jetpack Compose
- Android Jetpack
- ViewModel
- Room
- DataStore
- Kotlin Coroutines
- Retrofit 或 Ktor Client

第一版不建议使用 Flutter 或 React Native。锁屏显示、点亮屏幕、通知、后台启动限制、语音识别、文字转语音、电话权限、电池优化和无障碍适配都需要直接处理 Android 系统能力，使用 Kotlin 可以减少跨平台封装层带来的不确定性。

## 锁屏模块

锁屏功能独立设计，不和天气、语音模块混在一起：

```text
LockScreen
├── LockScreenActivity
├── ScreenWakeController
├── LockStateController
├── LargeClockView
├── DateAndWeekView
├── LockScreenCapabilityChecker
└── LockScreenFallback
```

可以验证以下系统能力：

```kotlin
window.setShowWhenLocked(true)
window.setTurnScreenOn(true)
```

这两个 API 只表示应用请求系统允许锁屏显示和点亮屏幕，不保证所有 Android 版本或手机厂商都会在用户点亮屏幕后自动打开页面。

必须验证：

1. 用户主动打开 App 后锁屏时是否仍然可见；
2. 屏幕点亮时 App 是否可以显示在锁屏上；
3. App 是否可以点亮屏幕；
4. 屏幕熄灭后是否还能恢复；
5. 通知点击后是否可以进入大字页面；
6. 手机重启后是否仍然有效；
7. 电池优化是否阻止功能运行；
8. 不同 Android 版本和手机厂商的行为差异。

主路径和降级路径：

```text
系统允许锁屏显示
    -> 锁屏上显示大字页面

系统不允许自动显示
    -> 锁屏通知显示摘要
    -> 用户点击通知进入大字页面
```

不能把监听屏幕点亮事件后自动拉起 Activity 作为唯一方案。现代 Android 对后台启动 Activity 有限制，厂商系统可能进一步限制。第一版不引入无障碍服务来绕过系统限制，除非后续验证确实需要。

## 本地数据和联系人

预存联系人默认只保存在手机本地：

```text
Room
├── Contact
│   ├── name
│   ├── phoneNumber
│   └── alias
└── AppSettings
```

多个称呼可以映射到同一联系人，例如“女儿”“闺女”“孩子”。电话号码默认不上传后端，以减少隐私风险和数据迁移成本。

## 语音识别隐私边界

Android `SpeechRecognizer` 不等于离线语音识别。它可能调用系统配置的识别服务，也可能将音频或识别请求发送到网络服务商。第一版必须明确以下问题：

- 使用 Android 系统识别服务，还是指定第三方语音识别服务；
- 音频是否上传；
- 上传到哪家供应商或哪一地区的服务；
- 音频和识别文本保留多久；
- 是否保存音频；
- 是否允许用户关闭联网语音识别；
- 网络不可用时如何提示和降级。

因此，阶段二不应把功能写成“本地语音识别”。准确表述是“语音识别方案待选择”，并在实现前确定音频上传和数据保留规则。

持续监听和唤醒词不作为第一版范围。第一版采用按键说话：

```text
用户点击麦克风按钮
    -> 开始录音
    -> 用户说话
    -> 停止录音
    -> 语音识别
    -> 大语言模型理解
    -> 文字和语音播报结果
```

## 语音拨号流程

大语言模型只负责理解，不直接控制拨号：

```text
用户说：“给女儿打电话”
    -> 语音识别
    -> 大语言模型识别意图
    -> 返回结构化结果：call_contact / 女儿
    -> Android 本地匹配联系人
    -> 语音确认：“要拨打女儿吗？”
    -> 用户确认
    -> 打开拨号界面
```

第一版使用 `ACTION_DIAL`：

- 打开系统拨号界面；
- 预填匹配到的电话号码；
- 由用户在系统拨号界面确认拨出；
- 不申请 `CALL_PHONE` 直接拨号权限。

第一版不使用 `ACTION_CALL`。直接拨号需要 `CALL_PHONE` 权限，误识别时风险更高。后续是否支持直接拨号，必须单独讨论权限、确认机制和用户可见提示。

MVP 验收条件：

- 未识别到唯一联系人时不得打开拨号界面；
- 匹配到联系人后必须先语音确认；
- 用户拒绝或未确认时不得拨号；
- 通过 `ACTION_DIAL` 打开系统拨号界面；
- 不申请 `CALL_PHONE` 权限；
- 模型输出必须经过结构化校验。

示例结构化意图：

```json
{
  "intent": "call_contact",
  "contactName": "女儿",
  "needsConfirmation": true
}
```

## 语音和模型接口

具体供应商不直接写死在 Android 端：

```kotlin
interface SpeechRecognizer {
    suspend fun recognize(audio: ByteArray): String
}

interface SpeechSynthesizer {
    suspend fun synthesize(text: String): AudioResult
}

interface AssistantModel {
    suspend fun understand(input: String): AssistantIntent
}
```

大语言模型只通过后端调用。后端负责隐藏密钥、限制请求频率、统一提示词、校验模型输出、转换结构化意图和切换供应商。

模型允许的意图应使用有限集合，例如：

```text
weather_query
call_contact
unknown
```

时间显示、联系人匹配、拨号确认等确定性逻辑由 Android App 控制，不交给模型自由执行。

## 后端技术栈

采用轻量模块化单体：

- TypeScript
- Fastify
- SQLite
- Caddy
- systemd
- 固定域名

建议结构：

```text
backend
├── src
│   ├── routes
│   ├── services
│   │   ├── weather
│   │   ├── speech
│   │   ├── llm
│   │   └── contacts
│   ├── providers
│   ├── db
│   └── config
├── migrations
└── backups
```

服务层和第三方服务适配层分开：

```text
WeatherService
    -> WeatherProvider

AssistantService
    -> LLMProvider
```

第一版不需要 Kubernetes、Redis、消息队列、微服务、本地大语言模型或本地大型语音模型。2 核 2G 服务器足够运行一个 API 服务、SQLite、Caddy 和少量后台任务。

## 服务器迁移

App 不写死服务器 IP，只访问固定域名：

```text
https://api.example.com
```

部署关系：

```text
固定域名
    -> DNS
    -> Caddy
    -> Fastify API
    -> SQLite
```

迁移服务器时需要备份 SQLite、部署后端代码、恢复环境变量、恢复数据库、修改 DNS、检查 HTTPS 和 API，并维护相应的备案接入关系和 App 登记信息。

## 分阶段开发顺序

### 阶段一：离线时钟页面

已完成解锁后的大字日期、时间和星期。锁屏显示已从当前交付中移除。2026-10-04 起，下一阶段是同一页面上的天气，不再等待试用反馈后才开始。

### 阶段二：本地联系人和语音拨号

实现手动添加联系人、联系人别名、语音识别方案、语音确认和 `ACTION_DIAL`。在实现前明确识别服务、音频上传目的地、保存期限和网络不可用时的行为。

### 当前下一阶段：天气

2026-10-04 改为在现有单页上做天气，不再先做联系人和拨号：

```text
打开 App 时申请定位权限
    -> GPS 得到经纬度
    -> Cloudflare Worker 调用 Open-Meteo
    -> 同一页面显示并播报日期、时间、星期、是否下雨和气温
```

不做手动输入城市，不增加第二个页面。

### 阶段四：大语言模型

接入模型处理天气自然语言问题、联系人称呼、模糊表达、语音命令分类和不明确请求的追问。模型不负责直接拨号或修改本地联系人。

## 当前建议

```text
Android：Kotlin + Jetpack Compose
当前页面：日期、时间、星期和天气在同一页
天气：打开时申请精确定位，GPS 得到经纬度，Cloudflare Worker 调用 Open-Meteo
后端：为天气创建 Worker，不做账号和数据库
联系人、拨号和模型：这轮不做
```

定位被拒绝或天气请求失败时，时钟三行仍然显示。红米中文朗读尚未在 `23013RK75C` 上实测。

## 待确认问题

- 亲人实际手机的型号、MagicOS 版本和 Android 版本；
- 亲人试用离线 APK 后对大字页面、锁屏和星期数字的反馈；
- 天气已进入开发：同一页面、精确定位、Worker 调用 Open-Meteo。红米中文朗读尚未实测，不阻塞天气数据实现。
- 后续功能是否需要账号、云端同步或多设备支持。
