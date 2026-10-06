package com.anxinkan.app

import org.json.JSONObject

internal data class WeatherDay(
    val date: String,
    val weekday: String,
    val condition: String,
    val low: Int,
    val high: Int,
)

internal data class WeatherReport(
    val timezone: String,
    val speech: String,
    val days: List<WeatherDay>,
)

internal fun parseWeatherReport(json: String): WeatherReport {
    val root = JSONObject(json)
    val daysJson = root.getJSONArray("days")
    val days = buildList {
        for (index in 0 until daysJson.length()) {
            val day = daysJson.getJSONObject(index)
            add(
                WeatherDay(
                    date = day.getString("date"),
                    weekday = day.getString("weekday"),
                    condition = day.getString("condition"),
                    low = day.getInt("low"),
                    high = day.getInt("high"),
                ),
            )
        }
    }
    return WeatherReport(
        timezone = root.getString("timezone"),
        speech = root.getString("speech"),
        days = days,
    )
}

internal enum class WeatherKind { Clear, Cloud, Rain, Snow, Unknown }

internal fun weatherKind(condition: String): WeatherKind = when (condition) {
    "晴" -> WeatherKind.Clear
    "多云", "阴", "雾" -> WeatherKind.Cloud
    "小雨", "中雨", "大雨", "冻雨", "雷雨" -> WeatherKind.Rain
    "雪" -> WeatherKind.Snow
    else -> WeatherKind.Unknown
}

internal fun spokenWeekdayNumber(weekday: String): Int? = when (weekday) {
    "星期一" -> 1
    "星期二" -> 2
    "星期三" -> 3
    "星期四" -> 4
    "星期五" -> 5
    "星期六" -> 6
    "星期日" -> 7
    else -> null
}
