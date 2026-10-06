import { buildForecast } from "./forecast.mjs";

function parseCoordinate(value, minimum, maximum) {
  if (value === null || value.trim() === "") return null;
  const number = Number(value);
  return Number.isFinite(number) && number >= minimum && number <= maximum ? number : null;
}
export async function handleWeather(request, fetchImpl = fetch, now = new Date(), upstreamBase = "https://api.open-meteo.com/v1/forecast") {
  const url = new URL(request.url);
  if (url.pathname !== "/weather") return new Response("not found", { status: 404 });
  const latitude = url.searchParams.get("latitude");
  const longitude = url.searchParams.get("longitude");
  const days = url.searchParams.get("days") ?? "3";
  if (!latitude || !longitude || (days !== "3" && days !== "5")) {
    return new Response("bad request", { status: 400 });
  }
  const latitudeNumber = parseCoordinate(latitude, -90, 90);
  const longitudeNumber = parseCoordinate(longitude, -180, 180);
  if (latitudeNumber === null || longitudeNumber === null) {
    return new Response("bad request", { status: 400 });
  }
  const upstream = new URL(upstreamBase);
  upstream.searchParams.set("latitude", String(latitudeNumber));
  upstream.searchParams.set("longitude", String(longitudeNumber));
  upstream.searchParams.set("daily", "weather_code,temperature_2m_max,temperature_2m_min");
  upstream.searchParams.set("timezone", "auto");
  upstream.searchParams.set("forecast_days", days);
  let response;
  try {
    response = await fetchImpl(upstream);
  } catch {
    return new Response("upstream unavailable", { status: 502 });
  }
  if (!response.ok) return new Response("upstream failed", { status: 502 });
  const forecast = buildForecast(await response.json(), now);
  return Response.json(forecast);
}

export default { fetch: (request) => handleWeather(request) };
