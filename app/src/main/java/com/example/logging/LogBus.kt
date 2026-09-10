package com.example.logging

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

enum class LogCategory {
    PLAYBACK_DETECTION,
    ENGINE,
    PERMISSIONS,
    SYSTEM,
    HEADPHONES,
    UI
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val category: LogCategory,
    val message: String
)

object LogBus {
    private const val MAX_ENTRIES = 500
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun add(level: LogLevel, category: LogCategory, message: String) {
        val entry = LogEntry(timestamp = System.currentTimeMillis(), level = level, category = category, message = message)
        val next = (_entries.value + entry).takeLast(MAX_ENTRIES)
        _entries.value = next

        val tag = category.name
        val logMessage = message
        when (level) {
            LogLevel.INFO -> Log.i(tag, logMessage)
            LogLevel.SUCCESS -> Log.i(tag, "SUCCESS: $logMessage")
            LogLevel.WARNING -> Log.w(tag, logMessage)
            LogLevel.ERROR -> Log.e(tag, logMessage)
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}

object AppLog {
    fun i(category: LogCategory, message: String) = LogBus.add(LogLevel.INFO, category, message)
    fun success(category: LogCategory, message: String) = LogBus.add(LogLevel.SUCCESS, category, message)
    fun w(category: LogCategory, message: String) = LogBus.add(LogLevel.WARNING, category, message)
    fun e(category: LogCategory, message: String) = LogBus.add(LogLevel.ERROR, category, message)
}
