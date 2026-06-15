package com.example.dobby

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class DobbyApplication

fun main(args: Array<String>) {
    runApplication<DobbyApplication>(*args)
}
