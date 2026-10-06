# 天气 Worker 部署与真机验证

## 本地已完成

- Worker 入口：`worker/src/index.mjs`
- 本地服务：`worker/local-server.mjs`
- 契约测试：`worker/test/forecast.test.mjs`、`worker/test/handler.test.mjs`
- Cloudflare 配置：`worker/wrangler.toml`
- 本地模拟上游和两个经纬度 HTTP 请求已验证。
- 真实 Open-Meteo 在当前执行环境请求超时；Cloudflare 部署后的真实链路仍未验证。

## 你执行的部署步骤

在仓库的 `worker/` 目录执行。需要先安装并登录 Wrangler；本项目没有替你登录 Cloudflare，也没有自动部署。

```bash
cd worker
npx wrangler login
npx wrangler deploy
```

部署成功后，Wrangler 会返回一个 Worker URL，形式类似：

```text
https://anxinkan-weather.<你的账户>.workers.dev
```

先用浏览器或 curl 验证接口。经纬度示例：

```bash
curl 'https://anxinkan-weather.<你的账户>.workers.dev/weather?latitude=31.2304&longitude=121.4737&days=3'
curl 'https://anxinkan-weather.<你的账户>.workers.dev/weather?latitude=34.0522&longitude=-118.2437&days=3'
```

预期 JSON 包含：`timezone`、`speech`、`days`；`days[0]` 应与 speech 中的“今天”对应。真实接口返回后，再把完整 Worker URL 注入 Android release 构建的 `WEATHER_WORKER_BASE_URL` 环境变量。不要把未验证地址写死进源码。

## 真机验证步骤

1. 在红米或荣耀手机上打开 App。
2. 首屏先确认日期、时间、星期仍立即显示。
3. 系统定位权限弹窗出现时，由照护者授予“精确位置”；只授予大致位置或拒绝时，时钟仍可用，天气不可用。
4. 确认天气行显示星期、天气图形和完整摄氏温度，不应截断核心温度。
5. 保存原日期、时间和时区后，再做时间/日期/时区测试；结束后必须恢复原值。
6. TTS 暂不作为本任务完成条件；中文朗读另开任务，设备验证前不宣称可用。

## 当前未验证

- Cloudflare 真实部署是否成功。
- 国内运营商网络到 Worker 和 Open-Meteo 的实际耗时/失败率。
- Android 精确定位权限和 GPS 在红米/荣耀上的实际行为。
- 单页天气布局在真实屏幕上的可读性。
- 红米/荣耀系统中文 TTS 是否安装并可用。
