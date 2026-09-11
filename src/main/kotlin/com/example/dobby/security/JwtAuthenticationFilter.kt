package com.example.dobby.security

import com.example.dobby.service.JWTService
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
    private val jwtService: JWTService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }
        val token = authHeader.substring(7)
        try {
            val discordUserId = jwtService.validateTokenAndGetSubject(token)
            if (SecurityContextHolder.getContext().authentication == null) {
                // Downstream controllers use authentication.name as the trusted Discord user ID.
                val authToken =
                    UsernamePasswordAuthenticationToken(
                        discordUserId,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USER")),
                    )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        } catch (e: Exception) {
            logger.error("Failed to set user authentication from JWT", e)
        }
        filterChain.doFilter(request, response)
    }
}
