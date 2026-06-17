package com.example.dobby.logging

import com.example.dobby.config.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlin.coroutines.cancellation.CancellationException

@Service
class LogEmitter(
    @Qualifier("defaultScope") private val serviceScope: CoroutineScope
) {
    fun emitLogs(): SseEmitter {
        val emitter = SseEmitter(0L)
        val job = serviceScope.launch {
            try {
                emitter.send(SseEmitter.event().data("Log stream started via Appender Pipeline"))
                LogAppender.logFlow.collect { logLine ->
                    try {
                        emitter.send(SseEmitter.event().data(logLine))
                    } catch (e: java.io.IOException) {
                        logger.info("Client closed the tab or disconnected from log stream.")
                        emitter.complete()
                        currentCoroutineContext().cancel()
                        return@collect
                    }
                }
            } catch (e: CancellationException) {
                logger.debug("Log stream coroutine shut down cleanly.")
            } catch (e: Exception) {
                logger.error("Unexpected error in log pipeline: ${e.message}")
                emitter.completeWithError(e)
            }
        }
        emitter.onCompletion { job.cancel() }
        emitter.onTimeout { job.cancel(); emitter.complete() }
        return emitter
    }
}