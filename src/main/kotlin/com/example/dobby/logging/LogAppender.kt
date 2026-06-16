package com.example.dobby.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LogAppender : AppenderBase<ILoggingEvent>() {

    companion object {
        private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
        val logFlow = _logFlow.asSharedFlow()
    }

    override fun append(eventObject: ILoggingEvent?) {
        if (eventObject == null) return
        val formattedLog = "[${eventObject.level}] ${eventObject.loggerName} - ${eventObject.formattedMessage}"

        _logFlow.tryEmit(formattedLog)
    }
}