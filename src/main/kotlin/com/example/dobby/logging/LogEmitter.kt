package com.example.dobby.logging

import com.example.dobby.config.log
import kotlinx.coroutines.*
import kotlinx.io.IOException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

@Service
class LogEmitter(
    @Qualifier("ioScope") private val serviceScope: CoroutineScope
) {
    private val activeConnections = AtomicInteger(0)

    fun emitLogs(): SseEmitter {
        val emitter = SseEmitter(0L)

        val job = serviceScope.launch {
            try {

                val currentCount = activeConnections.incrementAndGet()
                log.info("New client dashboard connected. Active stream counts: $currentCount")

                // send an immediate heartbeat to keep the connection alive while we wait for the first log line to arrive.
                safeHeartbeat(emitter)
                launch {
                    // send a heartbeat every 15 seconds to keep the connection alive and detect if the client has disconnected.
                    // If the client is gone, complete the emitter and cancel this coroutine.
                    while (isActive) {
                        delay(15_000.milliseconds)

                        if (!safeHeartbeat(emitter)) {
                            emitter.complete()
                            coroutineContext.cancel()
                        }
                    }
                }
                // collect log lines from the LogAppender and send them to the client.
                LogAppender.logFlow.collect { logLine ->
                    if (!safeSend(emitter, logLine)) {
                        emitter.complete()
                        coroutineContext.cancel()
                    }
                }

            } catch (_: CancellationException) {
                // normal shutdown
            } catch (e: Exception) {
                log.error("Unexpected error in log stream", e)
                runCatching {
                    emitter.completeWithError(e)
                }
            }
        }
        emitter.onCompletion {
            job.cancel()
            val remaining = activeConnections.decrementAndGet()
            log.info("Client dashboard disconnected. Remaining active streams: $remaining")
        }
        emitter.onTimeout {
            job.cancel()
            emitter.complete()
        }
        return emitter
    }

    /**
     * Sends a single SSE event. Returns false if the client is gone (IOException),
     * true on success. Runs on this scope's dispatcher (ioScope -> Dispatchers.IO),
     * so the blocking emitter.send() call is safe here.
     */
    private fun safeSend(emitter: SseEmitter, data: String): Boolean {
        return try {
            emitter.send(SseEmitter.event().data(data))
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun safeHeartbeat(emitter: SseEmitter): Boolean {
        return try {
            emitter.send(SseEmitter.event().comment("heartbeat"))
            true
        } catch (_: IOException) {
            false
        }
    }
}