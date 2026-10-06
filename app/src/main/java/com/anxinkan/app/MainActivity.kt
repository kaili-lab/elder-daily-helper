package com.anxinkan.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LargeClockScreen()
        }
    }
}

@Composable
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private fun LargeClockScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val refreshEvents = remember { Channel<ClockRefreshTrigger>(Channel.CONFLATED) }
    var display by remember { mutableStateOf(ClockDisplayModel.current()) }
    var weather by remember { mutableStateOf<WeatherReport?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch { weather = loadWeatherIfPrecise(context) }
        }
    }
    LaunchedEffect(BuildConfig.WEATHER_WORKER_BASE_URL) {
        if (BuildConfig.WEATHER_WORKER_BASE_URL.isBlank()) return@LaunchedEffect
        val precise = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (precise) weather = loadWeatherIfPrecise(context)
        else permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    DisposableEffect(lifecycleOwner, context, refreshEvents) {
        var receiverRegistered = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val trigger = when (intent.action) {
                    Intent.ACTION_TIME_CHANGED -> ClockRefreshTrigger.TimeChanged
                    Intent.ACTION_DATE_CHANGED -> ClockRefreshTrigger.DateChanged
                    Intent.ACTION_TIMEZONE_CHANGED -> ClockRefreshTrigger.TimezoneChanged
                    else -> null
                }
                if (trigger != null) refreshEvents.trySend(trigger)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (!receiverRegistered) {
                        context.registerReceiver(receiver, timeRefreshFilter())
                        receiverRegistered = true
                    }
                    refreshEvents.trySend(ClockRefreshTrigger.Foreground)
                }
                Lifecycle.Event.ON_RESUME -> refreshEvents.trySend(ClockRefreshTrigger.Foreground)
                Lifecycle.Event.ON_STOP -> {
                    if (receiverRegistered) {
                        context.unregisterReceiver(receiver)
                        receiverRegistered = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            observer.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_START)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (receiverRegistered) context.unregisterReceiver(receiver)
            refreshEvents.close()
        }
    }

    LaunchedEffect(lifecycleOwner, refreshEvents) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var trigger = ClockRefreshTrigger.Foreground
            while (isActive) {
                val instant = Instant.now()
                val zoneId = ZoneId.systemDefault()
                val result = ClockDisplayModel.refresh(trigger, instant, zoneId, started = true)
                result.display?.let { display = it }
                val delayMillis = result.nextDelayMillis ?: break
                trigger = select {
                    onTimeout(delayMillis) { ClockRefreshTrigger.MinuteElapsed }
                    refreshEvents.onReceive { it }
                }
            }
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0B))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ClockText(display.weekday, 72.dp, Color(0xFFFFC857))
                ClockText(display.time, 52.dp, Color(0xFFFFF4D6))
                ClockText(display.date, 32.dp, Color(0xFFFFF4D6))
                weather?.days?.forEach { day ->
                    WeatherLine(day)
                }
            }
        }
    }
}

private val timeRefreshActions = setOf(
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_DATE_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
)

private fun timeRefreshFilter(): IntentFilter = IntentFilter().apply {
    timeRefreshActions.forEach(::addAction)
}

@Composable
private fun ClockText(
    text: String,
    fontSize: androidx.compose.ui.unit.Dp,
    color: Color,
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = color,
        fontSize = fontSize.value.sp / androidx.compose.ui.platform.LocalDensity.current.fontScale,
        maxLines = 1,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WeatherLine(day: WeatherDay) {
    val weekday = spokenWeekdayNumber(day.weekday)?.let { "星期 $it" } ?: day.weekday
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WeatherMark(weatherKind(day.condition))
        Text(
            text = weekday,
            color = Color(0xFFFFF4D6),
            fontSize = 28.dp.value.sp / androidx.compose.ui.platform.LocalDensity.current.fontScale,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            text = "${day.low}℃到${day.high}℃",
            color = Color(0xFFFFF4D6),
            fontSize = 28.dp.value.sp / androidx.compose.ui.platform.LocalDensity.current.fontScale,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WeatherMark(kind: WeatherKind) {
    val color = when (kind) {
        WeatherKind.Rain -> Color(0xFF7EC8FF)
        WeatherKind.Snow -> Color(0xFFF5F5F5)
        WeatherKind.Clear -> Color(0xFFFFC857)
        WeatherKind.Cloud, WeatherKind.Unknown -> Color(0xFF8A8A8A)
    }
    androidx.compose.foundation.Canvas(modifier = Modifier.size(36.dp)) {
        val canvasSize = size
        when (kind) {
            WeatherKind.Rain -> listOf(0.25f, 0.5f, 0.75f).forEach { x ->
                drawCircle(color, canvasSize.minDimension / 12f, androidx.compose.ui.geometry.Offset(canvasSize.width * x, canvasSize.height * 0.55f))
            }
            WeatherKind.Snow -> listOf(0.3f, 0.7f).forEach { x ->
                drawCircle(color, canvasSize.minDimension / 10f, androidx.compose.ui.geometry.Offset(canvasSize.width * x, canvasSize.height * 0.4f))
            }
            WeatherKind.Clear -> drawCircle(color, canvasSize.minDimension / 3f)
            WeatherKind.Cloud -> drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
            WeatherKind.Unknown -> drawCircle(color, canvasSize.minDimension / 3f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        }
    }
}
