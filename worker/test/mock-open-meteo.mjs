import { createServer } from "node:http";

const payloads = new Map([
  ["31.2304", {
    timezone: "Asia/Shanghai",
    daily: {
      time: ["2026-10-05", "2026-10-06", "2026-10-07"],
      weather_code: [0, 61, 71],
      temperature_2m_max: [20, 21, 10],
      temperature_2m_min: [15, 18, 3],
    },
  }],
  ["34.0522", {
    timezone: "America/Los_Angeles",
    daily: {
      time: ["2026-10-04", "2026-10-05", "2026-10-06"],
      weather_code: [3, 95, 0],
      temperature_2m_max: [22, 19, 24],
      temperature_2m_min: [15, 13, 16],
    },
  }],
]);

const server = createServer((request, response) => {
  const url = new URL(request.url, "http://127.0.0.1");
  const payload = payloads.get(url.searchParams.get("latitude"));
  if (!payload) {
    response.statusCode = 404;
    response.end("fixture not found");
    return;
  }
  response.setHeader("content-type", "application/json");
  response.end(JSON.stringify(payload));
});

server.listen(8788, "127.0.0.1", () => {
  console.log("mock Open-Meteo listening on http://127.0.0.1:8788/v1/forecast");
});
