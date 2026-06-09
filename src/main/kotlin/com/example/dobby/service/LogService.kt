package com.example.dobby.service

import com.example.dobby.exception.DobbyException
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class LogService {

    fun emitLogs(): SseEmitter {
        val emitter = SseEmitter(0L) // No timeout
        val logFile = File("logs/dobby.log")
        var lastPosition = 0L

        val executor = Executors.newSingleThreadScheduledExecutor()

        // Send initial event so client knows connection is alive
        executor.submit {
            try {
                emitter.send(SseEmitter.event().data("Log stream started"))
            } catch (e: Exception) {
                throw DobbyException.LogStreamException("Failed to send initial log stream event", e)
            }
        }

        executor.scheduleAtFixedRate({
            try {
                val currentLength = logFile.length()
                if (currentLength > lastPosition) {
                    logFile.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                        reader.skip(lastPosition)
                        reader.lineSequence().forEach { line ->
                            emitter.send(SseEmitter.event().data(line))
                        }
                    }
                    lastPosition = currentLength
                }
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }, 0, 1, TimeUnit.SECONDS)

        emitter.onCompletion { executor.shutdown() }
        emitter.onTimeout {
            executor.shutdown()
            emitter.complete()
        }

        return emitter
    }
}
