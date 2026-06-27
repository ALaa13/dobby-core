package com.example.dobby.security

import com.example.dobby.AppProperties
import com.example.dobby.service.RateLimitingService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApiKeyFilter(
    private val appProperties: AppProperties,
    private val rateLimitingService: RateLimitingService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val apiKeyHeader = appProperties.app.security.apiKeyHeader
        val apiKeySecret = appProperties.app.security.apiKeySecret
        val requestKey = request.getHeader(apiKeyHeader)

        if (requestKey != null && requestKey == apiKeySecret) {
            val isAllowed = rateLimitingService.tryConsume(requestKey)

            if (!isAllowed) {
                sendRateLimitErrorResponse(response)
                return
            }

            val authorities = listOf(SimpleGrantedAuthority("ROLE_INTERNAL_SYSTEM"))
            val authentication = UsernamePasswordAuthenticationToken("system-bot", null, authorities)
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun sendRateLimitErrorResponse(response: HttpServletResponse) {
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            """
            {
                "status": 429,
                "error": "Too Many Requests",
                "message": "API Key rate limit exceeded. Please throttle your requests."
            }
            """.trimIndent(),
        )
        response.writer.flush()
    }
}
