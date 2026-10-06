import assert from "node:assert/strict";
import { buildForecast } from "../src/forecast.mjs";

const payload = {
  timezone: "Asia/Shanghai",
  daily: {
    time: ["2026-10-05", "2026-10-06"],
    weather_code: [0, 61],
    temperature_2m_max: [20.2, 21.4],
    temperature_2m_min: [15.1, 18.2],
  },
};

const shanghai = buildForecast(payload, new Date("2026-10-04T16:35:00Z"));
assert.equal(
  shanghai.speech,
  "今天是2026年10月5日星期一0点35分，晴，气温15到20度。星期二，小雨，气温18到21度。",
);
assert.equal(shanghai.days[0].date, "2026-10-05");
assert.equal(shanghai.days[0].weekday, "星期一");
assert.equal(shanghai.days[1].date, "2026-10-06");

const losAngeles = buildForecast(
  { ...payload, timezone: "America/Los_Angeles", daily: { ...payload.daily, time: ["2026-10-04", "2026-10-05"] } },
  new Date("2026-10-04T16:35:00Z"),
);
assert.equal(losAngeles.days[0].date, "2026-10-04");
assert.match(losAngeles.speech, /^今天是2026年10月4日星期日9点35分/);
assert.doesNotMatch(losAngeles.speech, /10月5日星期日/);

console.log("forecast contract ok");
