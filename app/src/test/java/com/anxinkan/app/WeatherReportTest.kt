package com.anxinkan.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherReportTest {
    @Test
    fun parsesWorkerContractWithoutCallingOpenMeteo() {
        val report = parseWeatherReport(
            """
            {
              "timezone": "Asia/Shanghai",
              "speech": "今天是2026年10月5日星期一0点35分，晴，气温15到20度。星期二，小雨，气温18到21度。",
              "days": [
                {"date": "2026-10-05", "weekday": "星期一", "condition": "晴", "low": 15, "high": 20},
                {"date": "2026-10-06", "weekday": "星期二", "condition": "小雨", "low": 18, "high": 21}
              ]
            }
            """.trimIndent(),
        )

        assertEquals("Asia/Shanghai", report.timezone)
        assertEquals("星期一", report.days[0].weekday)
        assertEquals(15, report.days[0].low)
        assertEquals("星期二", report.days[1].weekday)
    }

    @Test
    fun classifiesWeatherWithoutTreatingUnknownAsClear() {
        assertEquals(WeatherKind.Rain, weatherKind("小雨"))
        assertEquals(WeatherKind.Rain, weatherKind("雷雨"))
        assertEquals(WeatherKind.Snow, weatherKind("雪"))
        assertEquals(WeatherKind.Clear, weatherKind("晴"))
        assertEquals(WeatherKind.Cloud, weatherKind("阴"))
        assertEquals(WeatherKind.Cloud, weatherKind("雾"))
        assertEquals(WeatherKind.Unknown, weatherKind("天气不明"))
        assertEquals(1, spokenWeekdayNumber("星期一"))
        assertEquals(7, spokenWeekdayNumber("星期日"))
    }
}
