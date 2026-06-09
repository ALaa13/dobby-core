package com.example.dobby.util

import org.slf4j.LoggerFactory

object Logging {

    private val logger = LoggerFactory.getLogger(Logging::class.java)

    fun logInfo(message: String) {
        logger.info(message)
    }

    fun logError(message: String, error: Throwable? = null) {
        logger.error(message, error)
    }
}