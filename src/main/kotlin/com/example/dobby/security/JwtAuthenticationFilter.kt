package com.example.dobby.security

import com.example.dobby.service.JWTService
import com.example.dobby.util.logger
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JWTService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        // Check if the header has a valid Bearer token format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }
        val token = authHeader.substring(7)
        try {
            val discordUserId = jwtService.validateTokenAndGetSubject(token)
            if (SecurityContextHolder.getContext().authentication == null) {
                // Create Spring Security authentication token
                val authToken = UsernamePasswordAuthenticationToken(
                    discordUserId, // This becomes authentication.name in your controller
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                // Inject it into the Security Context!
                SecurityContextHolder.getContext().authentication = authToken
            }
        } catch (e: Exception) {
            logger.error("Failed to set user authentication from JWT", e)
        }
        filterChain.doFilter(request, response)
    }
}