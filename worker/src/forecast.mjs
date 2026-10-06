const WEEKDAYS = ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"];

export function conditionFromCode(code) {
  if (code === 0 || code === 1) return "晴";
  if (code === 2) return "多云";
  if (code === 3) return "阴";
  if (code === 45 || code === 48) return "雾";
  if (code === 51 || code === 53 || code === 55 || code === 61 || code === 80) return "小雨";
  if (code === 63 || code === 81) return "中雨";
  if (code === 65 || code === 82) return "大雨";
  if (code === 56 || code === 57 || code === 66 || code === 67) return "冻雨";
  if (code === 71 || code === 73 || code === 75 || code === 77 || code === 85 || code === 86) return "雪";
  if (code === 95 || code === 96 || code === 99) return "雷雨";
  return "天气不明";
}

export function weekdayName(isoDate) {
  const [year, month, day] = isoDate.split("-").map(Number);
  return WEEKDAYS[new Date(Date.UTC(year, month - 1, day)).getUTCDay()];
}

function roundTemperature(value) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new TypeError("invalid temperature");
  }
  return Math.round(value);
}

export function formatTemperature(low, high) {
  const format = (value) => (value < 0 ? `零下${Math.abs(value)}` : String(value));
  if (low === high) return `气温${format(low)}度`;
  return `气温${format(low)}到${format(high)}度`;
}

function zonedNow(now, timeZone) {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(now);
  const value = (type) => parts.find((part) => part.type === type).value;
  return {
    date: `${value("year")}-${value("month")}-${value("day")}`,
    year: Number(value("year")),
    month: Number(value("month")),
    day: Number(value("day")),
    hour: Number(value("hour")),
    minute: Number(value("minute")),
  };
}

export function buildForecast(payload, now) {
  const localNow = zonedNow(now, payload.timezone);
  const days = payload.daily.time.map((date, index) => ({
    date,
    weekday: weekdayName(date),
    condition: conditionFromCode(payload.daily.weather_code[index]),
    low: roundTemperature(payload.daily.temperature_2m_min[index]),
    high: roundTemperature(payload.daily.temperature_2m_max[index]),
  }));
  const today = days.find((day) => day.date === localNow.date);
  const ordered = today ? [today, ...days.filter((day) => day !== today)] : days;
  const lines = ordered.map((day, index) => {
    const temperature = formatTemperature(day.low, day.high);
    if (index === 0 && today) {
      return `今天是${localNow.year}年${localNow.month}月${localNow.day}日${day.weekday}${localNow.hour}点${localNow.minute}分，${day.condition}，${temperature}。`;
    }
    return `${day.weekday}，${day.condition}，${temperature}。`;
  });
  return { timezone: payload.timezone, speech: lines.join(""), days: ordered };
}
