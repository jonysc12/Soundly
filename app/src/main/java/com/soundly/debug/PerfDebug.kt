package com.soundly.debug

import android.os.SystemClock
import android.os.Trace
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.soundly.BuildConfig

const val PERF_TAG = "SoundlyPerf"
private const val PERF_LOGGING_ENABLED = false

val isPerfLoggingEnabled: Boolean
    get() = BuildConfig.DEBUG && PERF_LOGGING_ENABLED

inline fun <T> perfTrace(section: String, block: () -> T): T {
    if (!isPerfLoggingEnabled) return block()

    val start = SystemClock.elapsedRealtimeNanos()
    Trace.beginSection(section)
    return try {
        block()
    } finally {
        Trace.endSection()
        val durationMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
        Log.d(PERF_TAG, "$section took ${"%.2f".format(durationMs)} ms")
    }
}

fun perfMark(message: String) {
    if (!isPerfLoggingEnabled) return
    Log.d(PERF_TAG, message)
}

@Composable
fun DebugRecompose(tag: String, logEvery: Int = 25) {
    if (!isPerfLoggingEnabled) return

    val count = remember { mutableIntStateOf(0) }
    SideEffect {
        count.intValue += 1
        if (count.intValue == 1 || count.intValue % logEvery == 0) {
            Log.d(PERF_TAG, "Recompose[$tag] = ${count.intValue}")
        }
    }
}
