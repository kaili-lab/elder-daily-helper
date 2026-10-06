import { buildForecast } from "./forecast.mjs";

const UPSTREAM_BASE = "https://api.open-meteo.com/v1/forecast";

function parseCoordinate(value, minimum, maximum) {
  if (value === null || value.trim() === "") return null;
  const number = Number(value);
  return Number.isFinite(number) && number >= minimum && number <= maximum ? number : null;
}

function roundCoordinate(number) {
  return Math.round(number * 100) / 100;
}

function textResponse(body, status) {
  return new Response(body, {
    status,
    headers: { "cache-control": "no-store" },
  });
}

function forecastIsValid(forecast) {
  return Array.isArray(forecast?.days)
    && forecast.days.length >= 1
    && forecast.days.every((day) => Number.isFinite(day.low) && Number.isFinite(day.high));
}

export async function handleWeather(
  request,
  fetchImpl = fetch,
  now = new Date(),
  upstreamBase = UPSTREAM_BASE,
  rateLimit = null,
) {
  if (request.method !== "GET") return textResponse("method not allowed", 405);
  const url = new URL(request.url);
  if (url.pathname !== "/weather") return textResponse("not found", 404);
  if (rateLimit) {
    const ip = request.headers.get("cf-connecting-ip") || "unknown";
    const { success } = await rateLimit.limit({ key: ip });
    if (!success) return textResponse("too many requests", 429);
  }
  const latitude = url.searchParams.get("latitude");
  const longitude = url.searchParams.get("longitude");
  const days = url.searchParams.get("days") ?? "3";
  if (!latitude || !longitude || (days !== "3" && days !== "5")) {
    return textResponse("bad request", 400);
  }
  const latitudeNumber = parseCoordinate(latitude, -90, 90);
  const longitudeNumber = parseCoordinate(longitude, -180, 180);
  if (latitudeNumber === null || longitudeNumber === null) {
    return textResponse("bad request", 400);
  }
  const upstream = new URL(upstreamBase);
  upstream.searchParams.set("latitude", roundCoordinate(latitudeNumber).toFixed(2));
  upstream.searchParams.set("longitude", roundCoordinate(longitudeNumber).toFixed(2));
  upstream.searchParams.set("daily", "weather_code,temperature_2m_max,temperature_2m_min");
  upstream.searchParams.set("timezone", "auto");
  upstream.searchParams.set("forecast_days", days);
  let response;
  try {
    response = await fetchImpl(upstream, {
      signal: AbortSignal.timeout(8000),
      redirect: "manual",
    });
  } catch {
    return textResponse("upstream unavailable", 502);
  }
  if (response.status >= 300 && response.status < 400) return textResponse("upstream failed", 502);
  if (!response.ok) return textResponse("upstream failed", 502);
  try {
    const forecast = buildForecast(await response.json(), now);
    if (!forecastIsValid(forecast)) return textResponse("upstream data invalid", 500);
    return Response.json(forecast);
  } catch {
    return textResponse("upstream data invalid", 500);
  }
}

export default {
  async fetch(request, env) {
    if (!env?.WEATHER_IP_LIMIT) return textResponse("rate limit unavailable", 503);
    return handleWeather(request, fetch, new Date(), UPSTREAM_BASE, env.WEATHER_IP_LIMIT);
  },
};
