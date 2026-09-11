package com.example.dobby.repository.r2dbc

import org.springframework.r2dbc.core.DatabaseClient

internal inline fun <reified T : Any> DatabaseClient.GenericExecuteSpec.bindNullable(
    name: String,
    value: T?,
): DatabaseClient.GenericExecuteSpec {
    return if (value == null) {
        bindNull(name, T::class.java)
    } else {
        bind(name, value)
    }
}
