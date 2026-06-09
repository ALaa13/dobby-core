package com.example.dobby.controller

import com.example.dobby.service.LogService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RequestMapping("/logs")
@RestController
class LogController(
    private val logService: LogService
) {
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamLogs(): SseEmitter {
        return logService.emitLogs()
    }
}