# 安心看

> 一个从真实家庭需求出发的 Android 原型：用大字显示星期、日期和时间，并在同一页面提供天气。

[![Android](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Cloudflare Workers](https://img.shields.io/badge/Cloudflare-Workers-F38020?logo=cloudflare&logoColor=white)](https://workers.cloudflare.com/)

这是我的一个端到端个人工程作品：从需求澄清、验收条件、Android 原生实现，到小型天气服务、接口边界、隐私取舍、签名 APK 和发布验证，都在同一个仓库里完成。

它不是一个已经适配所有手机的商业产品。当前更准确的定位是：**已经完成主机和服务端验证、等待目标荣耀手机现场测试的可安装原型**。

作者：[kaili-lab](https://github.com/kaili-lab)

## 我在这个项目里解决什么

很多手机的系统锁屏信息对视力较弱的老人不够醒目。这个项目把最重要的信息收缩成一个直接可读的页面：

```text
星期 7
14:35
2026-10-04
天气图形  星期 1  15℃到20℃
```

页面不依赖天气也能显示星期、日期和时间。天气是附加能力：需要精确定位和网络，失败时不遮挡时钟。

## 当前状态

**已完成并验证：**

- Kotlin + Jetpack Compose 单 Activity 大字页面。
- 星期数字、日期和时间按系统时区实时刷新。
- Android 天气客户端，以及同一页面的天气行显示。
- Cloudflare Worker 天气接口，固定调用 Open-Meteo。
- Worker 的参数校验、GET-only、上游超时、手动重定向处理、错误降级和按 IP 限流。
- 客户端和服务端都把经纬度取整到小数点后 2 位，约为 1 公里级精度。
- Worker 的本地契约测试、Android JVM 单元测试、release 构建、APK 签名和权限审计。

**尚未声称完成：**

- 目标荣耀手机上的定位、天气布局和实际可辨认性。
- 所有 Android 厂商的兼容性。
- 锁屏自动显示。当前构建不包含锁屏显示和通知降级能力。
- 中文语音播报、语音识别、联系人、拨号和大语言模型。

## 这个项目展示什么

我关注的不是把功能列表堆满，而是把一个小问题做成可以验证的工程：

- 把模糊的“给老人看清星期”拆成页面内容、数字映射、失败行为和现场验收条件。
- 使用 Kotlin 直接处理 Android 权限、生命周期、定位和 Compose 页面，而不是先引入跨平台封装。
- 把天气服务从 Android 客户端分离出来，固定上游地址，限制参数范围，并为上游失败设计客户端降级。
- 把坐标隐私当成接口设计的一部分：坐标在离开手机前和到达上游前各取整一次。
- 把“锁屏可能被厂商限制”记录成验证结果，而不是把 API 调用写成兼容性承诺。
- 保留失败证据和未验证边界，避免把“构建成功”写成“真实用户已经可用”。

## 技术取舍

| 问题 | 当前决定 | 原因 |
| --- | --- | --- |
| Android 技术栈 | Kotlin + Jetpack Compose | 需要直接处理 Android 权限、生命周期和系统行为。 |
| 天气服务 | Android -> Cloudflare Worker -> Open-Meteo | 客户端不直接依赖第三方天气接口，服务端可以统一校验和降级。 |
| 身份认证 | 暂不做登录、静态 token 或 Cloudflare Access | 当前是单个家庭场景；静态 token 会随 APK 暴露，登录墙会增加使用负担。 |
| 请求保护 | 参数校验、固定上游、超时、错误不回显、IP 限流 | 先控制开放接口的滥用和错误扩散，不伪装成强身份认证系统。 |
| 缓存 | 暂不做 | 天气播报包含当前时间，直接缓存整份响应会冻结时间。 |
| 锁屏 | 暂不承诺 | Android 和厂商系统限制不能只靠 `setShowWhenLocked` 推断。 |

Worker 是按公开服务设计的：没有用户登录，也没有把天气接口包装成“只有本 App 能访问”。真实地址通过构建环境变量注入，不写进仓库源码。

## 隐私与安全边界

- App 只在天气请求中使用精确位置；坐标离开手机前先取整。
- Worker 再次取整，并且只向固定的 Open-Meteo 地址发请求。
- Worker 不接受任意上游地址，因此不是开放代理。
- 上游错误、解析错误和内部异常不会直接返回异常文本或上游正文。
- Worker 只接受 `GET /weather`，每个 IP 每 60 秒最多 30 次。
- Cloudflare invocation logs 已关闭，避免把带查询参数的完整调用网址保存到日志中。
- 仓库不包含真实 Worker 地址、API token、签名文件或签名密码。

这些措施降低了位置暴露和接口滥用风险，但不等于强认证。知道线上地址的人仍然可以发起请求。

## 运行与验证

### Worker 测试

```bash
cd worker
pnpm test
```

测试覆盖参数校验、坐标取整、天气结果形状、上游错误、重定向、限流和固定错误响应。

### Android JVM 测试

```bash
./gradlew :app:testDebugUnitTest
```

### Debug 构建

```bash
./gradlew :app:assembleDebug
```

Debug 构建默认不注入天气 Worker 地址，因此只适合验证页面和本地逻辑。天气版 release 必须显式提供 HTTPS 地址。

### Release 构建

签名文件和密码必须放在仓库外，通过环境变量提供：

```bash
export ANXINKAN_RELEASE_KEYSTORE="/path/to/anxinkan-release.jks"
export ANXINKAN_RELEASE_STORE_PASSWORD="..."
export ANXINKAN_RELEASE_KEY_PASSWORD="..."
export WEATHER_WORKER_BASE_URL="https://<your-worker-host>"
./gradlew :app:assembleRelease
```

构建脚本会拒绝没有 HTTPS Worker 地址的 release 构建。不要把真实地址、密码或签名文件写进 Git。

## 仓库结构

```text
app/       Android 应用、天气客户端和 JVM 测试
worker/    Cloudflare Worker、本地服务和接口测试
docs/      需求、架构、验收和部署过程记录
```

## 下一步

下一步不是继续堆功能，而是在真实 Android 设备上验证当前交付物：安装 release APK，确认大字页面、精确定位权限和天气行的实际表现；如果天气失败，确认时钟仍然可用。

现场验证通过后，再决定是否值得继续做语音、锁屏增强或其他功能。当前不把这些方向写成已经承诺的产品能力。

## 许可证

当前仓库尚未声明开源许可证。除非另有书面许可，公开仓库不自动授予复制、修改或再分发代码的权利。
