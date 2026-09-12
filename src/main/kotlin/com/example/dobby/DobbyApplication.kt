package com.example.dobby

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

@SpringBootApplication(
    exclude = [UserDetailsServiceAutoConfiguration::class],
)
@EnableConfigurationProperties(AppProperties::class)
class DobbyApplication

fun main(args: Array<String>) {
    runApplication<DobbyApplication>(*args)
}
