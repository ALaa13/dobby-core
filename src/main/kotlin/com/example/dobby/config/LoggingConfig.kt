package com.example.dobby.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T : Any> T.log: Logger
    get() = LoggerFactory.getLogger(T::class.java)