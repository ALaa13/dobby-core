package com.example.dobby.logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.example.dobby.config.logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LogAppender : AppenderBase<ILoggingEvent>() {
    // Set up the standard Spring Boot console layout string format
    private val layout = PatternLayout().apply {
        // standard Spring format: Timestamp LEVEL PID --- [Thread] Logger: Message
        pattern = "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p %relative --- [%15.15t] %-40.40logger{39} : %m%n"
    }

    companion object {
        private val _logFlow = MutableSharedFlow<String>(
            // Change this value to determine how many old logs are sent when a client is first connected
            replay = 0,
            extraBufferCapacity = 256,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val logFlow = _logFlow.asSharedFlow()
    }

    override fun start() {
        // Connect the layout context to this appender engine and start it
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
        // Skip formatting work entirely if nobody's listening
        if (_logFlow.subscriptionCount.value == 0) return
        // This formats the log object into the exact full console string line layout
        val formattedLog = layout.doLayout(eventObject).trimEnd()
        if (!_logFlow.tryEmit(formattedLog)) {
            logger.warn("Log buffer full, dropping oldest log")
            // Buffer was full even with DROP_OLDEST — extremely unlikely, but worth knowing about
            // (avoid logging here directly to prevent feedback loops into this same appender)
        }
    }
}