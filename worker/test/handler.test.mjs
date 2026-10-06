import assert from "node:assert/strict";
import { handleWeather } from "../src/index.mjs";

const openMeteoPayload = {
  timezone: "Asia/Shanghai",
  daily: {
    time: ["2026-10-05", "2026-10-06", "2026-10-07"],
    weather_code: [0, 61, 71],
    temperature_2m_max: [20.2, 21.4, 10],
    temperature_2m_min: [15.1, 18.2, 3],
  },
};

const calls = [];
const fakeFetch = async (url) => {
  calls.push(new URL(url));
  return new Response(JSON.stringify(openMeteoPayload), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
};

const request = new Request("http://localhost/weather?latitude=31.23&longitude=121.47&days=3");
const response = await handleWeather(request, fakeFetch, new Date("2026-10-04T16:35:00Z"));
assert.equal(response.status, 200);
const body = await response.json();
assert.equal(calls.length, 1);
assert.equal(calls[0].hostname, "api.open-meteo.com");
assert.equal(calls[0].pathname, "/v1/forecast");
assert.equal(calls[0].searchParams.get("latitude"), "31.23");
assert.equal(calls[0].searchParams.get("longitude"), "121.47");
assert.equal(calls[0].searchParams.get("daily"), "weather_code,temperature_2m_max,temperature_2m_min");
assert.equal(calls[0].searchParams.get("timezone"), "auto");
assert.equal(calls[0].searchParams.get("forecast_days"), "3");
assert.equal(body.timezone, "Asia/Shanghai");
assert.equal(body.days.length, 3);
assert.equal(body.days[0].weekday, "星期一");
assert.equal(body.days[1].condition, "小雨");
assert.equal(body.days[2].condition, "雪");
assert.match(body.speech, /^今天是2026年10月5日星期一0点35分/);

for (const url of [
  "http://localhost/weather",
  "http://localhost/weather?latitude=x&longitude=121.47",
  "http://localhost/weather?latitude=31.23&longitude=121.47&days=4",
  "http://localhost/weather?latitude=90.001&longitude=121.47",
  "http://localhost/weather?latitude=-90.001&longitude=121.47",
  "http://localhost/weather?latitude=31.23&longitude=180.001",
  "http://localhost/weather?latitude=31.23&longitude=-180.001",
  "http://localhost/weather?latitude=%20&longitude=121.47",
  "http://localhost/weather?latitude=31.23&longitude=%20",
]) {
  const bad = await handleWeather(new Request(url), fakeFetch);
  assert.equal(bad.status, 400);
}

const missing = await handleWeather(new Request("http://localhost/other"), fakeFetch);
assert.equal(missing.status, 404);

const upstreamFailure = await handleWeather(
  request,
  async () => new Response("no", { status: 503 }),
);
assert.equal(upstreamFailure.status, 502);

const thrownUpstream = await handleWeather(
  request,
  async () => { throw new Error("network down"); },
);
assert.equal(thrownUpstream.status, 502);

console.log("worker handler contract ok");
