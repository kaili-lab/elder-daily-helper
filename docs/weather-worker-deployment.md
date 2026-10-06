# 天气 Worker 部署与验证

## 当前行为

- 只接受 `GET /weather`。其他方法返回 `405`，正文是 `method not allowed`。
- 缺少坐标、坐标超出范围，或 `days` 不是 `3` 或 `5` 时返回 `400`。
- 其他路径返回 `404`。
- 合法坐标先通过范围校验，再四舍五入到小数点后 2 位，然后才请求 Open-Meteo。大约 1 公里精度，不发送街道级坐标。
- 上游超时、网络失败、非 2xx 或 300–399 重定向都返回 `502`，不跟随重定向，也不返回上游正文。
- 上游 JSON 无法解析、天数为空，或温度不是有限数字时返回 `500`，正文固定为 `upstream data invalid`。
- 同一个 IP 每 60 秒最多 30 次。超出返回 `429`。生产环境缺少限流配置时返回 `503`，不会变成无限放行。
- 以上错误响应都带 `Cache-Control: no-store`。
- 不缓存整份天气响应，避免把播报里的当前时间冻住。
- Worker 配置关闭了带完整 URL 的调用日志。仪表盘仍需人工确认没有新增这类记录。免费版已经产生的日志大约保留 3 天，不能手动删除，只能等它过期。
- Worker 地址视为公开。本文件不记录真实地址，源码也不写死地址。

## 部署

在 `worker/` 目录执行。首次可用 `npx wrangler login`。Worker 已经存在后，后续部署可以使用只含该 Worker 编辑权限的 API token。不要把 token 写入仓库。

```bash
cd worker
npx wrangler deploy
```

部署输出应包含 `WEATHER_IP_LIMIT`。调用日志配置必须同时写 `observability.logs.enabled = true` 和 `invocation_logs = false`：前者保留排障日志，后者关闭带完整 URL 的调用记录。不要改成 `observability.enabled = false`。

## 浏览器验证

把部署得到的地址代入下面的链接。示例坐标是上海，不是某个人的精确位置。

```text
https://<worker-host>/weather?latitude=31.2304&longitude=121.4737&days=3
```

浏览器应显示 JSON，其中有 `timezone`、`speech` 和 `days`。`days[0]` 对应当天。再试：

```text
https://<worker-host>/weather?latitude=31.23&longitude=121.47&days=4
```

这一条应显示 `bad request`。不要用循环请求验证 429，以免占满限额。

## Android

App 在坐标离开手机前也取整到小数点后 2 位。网络失败、非 200 和无法解析的 JSON 都让天气为空，时钟继续显示。

在仪表盘确认调用日志不再记录带坐标的 URL 之前，不要用真机的精确位置请求这个服务，也不要把地址注入 release APK。

## 仍需人工确认

- Cloudflare 仪表盘里，这次部署之后没有新增带坐标的调用日志。
- Cloudflare 账号已开启两步验证。
- 国内运营商访问 `workers.dev` 的稳定性。
- 红米或荣耀手机上的定位权限和天气显示。
