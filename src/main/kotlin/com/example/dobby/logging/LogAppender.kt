package com.example.dobby.logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.example.dobby.config.log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LogAppender : AppenderBase<ILoggingEvent>() {
    private val layout =
        PatternLayout().apply {
            // Match the console layout so SSE viewers and server operators see equivalent log lines.
            pattern = "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p %relative --- [%15.15t] %-40.40logger{39} : %m%n"
        }

    companion object {
        private val _logFlow =
            MutableSharedFlow<String>(
                // New clients receive only live entries; the bounded buffer prevents logging from blocking application threads.
                replay = 0,
                extraBufferCapacity = 256,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val logFlow = _logFlow.asSharedFlow()
    }

    override fun start() {
        layout.context = this.context
        layout.start()
        super.start()
    }

    override fun stop() {
        layout.stop()
        super.stop()
    }

    override fun append(eventObject: ILoggingEvent?) {
        if (eventObject == null) return
        // Avoid formatting overhead when the dashboard has no active subscribers.
        if (_logFlow.subscriptionCount.value == 0) return
        val formattedLog = layout.doLayout(eventObject).trimEnd()
        if (!_logFlow.tryEmit(formattedLog)) {
            log.warn("Log buffer full, dropping oldest log")
            // Do not retry by logging the failure here; that would feed back into this appender.
        }
    }
}
