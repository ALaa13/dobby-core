package com.example.dobby.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineScopeConfig {
    @Bean
    fun ioScope(): CoroutineScope {
        // This application-owned scope isolates blocking I/O work from request coroutine lifecycles.
        return CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    @Bean
    fun defaultScope(): CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
}
