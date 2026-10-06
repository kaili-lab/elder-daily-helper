import { createServer } from "node:http";
import { handleWeather } from "./src/index.mjs";
const upstreamBase = process.env.OPEN_METEO_BASE_URL ?? "https://api.open-meteo.com/v1/forecast";
const port = Number(process.env.PORT ?? 8787);
const server = createServer(async (request, response) => {
  try {
    const origin = `http://${request.headers.host ?? `localhost:${port}`}`;
    const result = await handleWeather(new Request(new URL(request.url, origin)), fetch, new Date(), upstreamBase);
    response.statusCode = result.status;
    for (const [key, value] of result.headers) response.setHeader(key, value);
    response.end(await result.text());
  } catch (error) {
    response.statusCode = 500;
    response.end(error instanceof Error ? error.message : "internal error");
  }
});

server.listen(port, "127.0.0.1", () => {
  console.log(`weather worker listening on http://127.0.0.1:${port}`);
});
