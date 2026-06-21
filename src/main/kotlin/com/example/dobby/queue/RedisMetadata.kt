package com.example.dobby.queue

import java.time.Duration


object RedisChannels {
    const val ROAST_DELIVERY = "roast-delivery"
}

object RedisKeys {
    const val USER_PROFILE = "discord:user:profile"
}

object RedisKeyTimeout {
    val USER_PROFILE: Duration = Duration.ofMinutes(15)
}