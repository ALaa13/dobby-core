package com.example.dobby.util

object Logging {
    fun logInfo(message: String) {
        val timestamp = java.time.LocalDateTime.now()
        println("[$timestamp] [INFO] $message")
    }

    fun logError(message: String, error: Throwable? = null) {
        val timestamp = java.time.LocalDateTime.now()
        println("[$timestamp] [ERROR] $message")
        error?.printStackTrace()
    }
}