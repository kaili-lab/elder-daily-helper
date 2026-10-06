package com.anxinkan.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal object WeatherWorkerClient {
    fun fetch(baseUrl: String, latitude: Double, longitude: Double): WeatherReport? {
        if (baseUrl.isBlank()) return null
        val latitudeText = formatCoordinate(latitude)
        val longitudeText = formatCoordinate(longitude)
        val url = URL("${baseUrl.trimEnd('/')}/weather?latitude=$latitudeText&longitude=$longitudeText&days=3")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val status = connection.responseCode
            if (status != 200) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return weatherReportOrNull(status, body)
        } catch (_: IOException) {
            return null
        } finally {
            connection.disconnect()
        }
    }
}

internal suspend fun loadWeatherIfPrecise(context: Context): WeatherReport? {
    val precise = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    if (!precise || BuildConfig.WEATHER_WORKER_BASE_URL.isBlank()) return null
    val location = currentGps(context) ?: return null
    return try {
        withContext(Dispatchers.IO) {
            WeatherWorkerClient.fetch(
                BuildConfig.WEATHER_WORKER_BASE_URL,
                location.latitude,
                location.longitude,
            )
        }
    } catch (error: Exception) {
        if (error is kotlinx.coroutines.CancellationException) throw error
        null
    }
}

private suspend fun currentGps(context: Context): Location? = suspendCancellableCoroutine { continuation ->
    val manager = context.getSystemService(LocationManager::class.java)
    val cancel = CancellationSignal()
    continuation.invokeOnCancellation { cancel.cancel() }
    manager.getCurrentLocation(LocationManager.GPS_PROVIDER, cancel, context.mainExecutor) { location ->
        if (continuation.isActive) continuation.resume(location)
    }
}
