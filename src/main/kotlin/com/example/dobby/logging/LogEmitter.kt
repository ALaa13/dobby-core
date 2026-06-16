package com.example.dobby.logging

import com.example.dobby.config.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class LogEmitter(
    @Qualifier("defaultScope") private val serviceScope: CoroutineScope
) {

    fun emitLogs(): SseEmitter {
        val emitter = SseEmitter(0L)


        // Start collecting logs from our appender stream asynchronously
        serviceScope.launch {
            try {
                emitter.send(SseEmitter.event().data("Log stream started via Appender Pipeline"))

                LogAppender.logFlow.collect { logLine ->
                    try {
                        emitter.send(SseEmitter.event().data(logLine))
                    } catch (e: Exception) {
                        logger.error("Error sending log line to client: ${e.message}")
                        // Client disconnected or closed their tab
                        this.cancel()
                    }
                }
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }

        // Clean up coroutines cleanly when the request finishes or times out
        emitter.onCompletion { serviceScope.cancel() }
        emitter.onTimeout { serviceScope.cancel(); emitter.complete() }

        return emitter
    }
}