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

const post = await handleWeather(new Request(request.url, { method: "POST" }), fakeFetch);
assert.equal(post.status, 405);
assert.equal(await post.text(), "method not allowed");
assert.equal(post.headers.get("access-control-allow-origin"), null);
assert.equal(post.headers.get("cache-control"), "no-store");

const roundedCalls = [];
const rounded = await handleWeather(
  new Request("http://localhost/weather?latitude=31.2349&longitude=121.4761&days=3"),
  async (url, init) => {
    roundedCalls.push({ url: new URL(url), init });
    return new Response(JSON.stringify(openMeteoPayload), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  },
  new Date("2026-10-04T16:35:00Z"),
);
assert.equal(rounded.status, 200);
assert.equal(roundedCalls[0].url.searchParams.get("latitude"), "31.23");
assert.equal(roundedCalls[0].url.searchParams.get("longitude"), "121.48");
assert.equal(roundedCalls[0].init.redirect, "manual");
assert.ok(roundedCalls[0].init.signal);

const edgeCalls = [];
await handleWeather(
  new Request("http://localhost/weather?latitude=89.996&longitude=179.996"),
  async (url) => {
    edgeCalls.push(new URL(url));
    return new Response(JSON.stringify(openMeteoPayload), { status: 200 });
  },
);
assert.equal(edgeCalls[0].searchParams.get("latitude"), "90.00");
assert.equal(edgeCalls[0].searchParams.get("longitude"), "180.00");

const redirected = await handleWeather(
  request,
  async () => new Response(null, { status: 302, headers: { location: "https://evil.example" } }),
);
assert.equal(redirected.status, 502);
assert.equal(redirected.headers.get("cache-control"), "no-store");

async function invalidUpstream(payload) {
  const result = await handleWeather(
    request,
    async () => new Response(JSON.stringify(payload), { status: 200 }),
  );
  assert.equal(result.status, 500);
  const text = await result.text();
  assert.equal(text, "upstream data invalid");
  assert.equal(text.includes("TypeError"), false);
  assert.equal(result.headers.get("cache-control"), "no-store");
}

await invalidUpstream({ timezone: "Asia/Shanghai" });
await invalidUpstream({
  timezone: "Asia/Shanghai",
  daily: { time: [], weather_code: [], temperature_2m_max: [], temperature_2m_min: [] },
});
await invalidUpstream({
  timezone: "Asia/Shanghai",
  daily: {
    time: ["2026-10-05", "2026-10-06"],
    weather_code: [0, 1],
    temperature_2m_max: [20, 21],
    temperature_2m_min: [15],
  },
});
await invalidUpstream({
  timezone: "Asia/Shanghai",
  daily: {
    time: ["2026-10-05"],
    weather_code: [0],
    temperature_2m_max: [20],
    temperature_2m_min: [null],
  },
});

const limitedRequest = new Request(request.url, {
  headers: { "cf-connecting-ip": "203.0.113.10" },
});
let limitedKey;
const callsBeforeLimit = calls.length;
const limited = await handleWeather(limitedRequest, fakeFetch, new Date(), undefined, {
  limit: async ({ key }) => {
    limitedKey = key;
    return { success: false };
  },
});
assert.equal(limitedKey, "203.0.113.10");
assert.equal(limited.status, 429);
assert.equal(await limited.text(), "too many requests");
assert.equal(calls.length, callsBeforeLimit);

const worker = (await import("../src/index.mjs")).default;
const unavailable = await worker.fetch(new Request(request.url));
assert.equal(unavailable.status, 503);
assert.equal(await unavailable.text(), "rate limit unavailable");
assert.equal(unavailable.headers.get("cache-control"), "no-store");
