package com.example.dobby.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApiKeyFilter(
    @Value($$"${app.security.api-key-header}") private val apiKeyHeader: String,
    @Value($$"${app.security.api-key-secret}") private val apiKeySecret: String
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestKey = request.getHeader(apiKeyHeader)

        // If the header matches our secret, authenticate the system
        if (requestKey != null && requestKey == apiKeySecret) {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_INTERNAL_SYSTEM"))

            // "system-bot" acts as the principal username for logging/auditing
            val authentication = UsernamePasswordAuthenticationToken("system-bot", null, authorities)
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

            // Inject into Spring Security Context
            SecurityContextHolder.getContext().authentication = authentication
        }
        // Always continue down the filter chain (so JWT filter can run if this wasn't an API key request)
        filterChain.doFilter(request, response)
    }
}