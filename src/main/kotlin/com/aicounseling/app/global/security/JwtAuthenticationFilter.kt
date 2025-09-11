package com.aicounseling.app.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractToken(request)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            val userId = jwtTokenProvider.getUserIdFromToken(token)
            // email은 현재 사용하지 않지만, 추후 권한 처리 시 필요할 수 있음
            // val email = jwtTokenProvider.getEmailFromToken(token)

            val authentication =
                UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    emptyList(),
                )

            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        val bearerPrefix = "Bearer "
        return if (bearerToken != null && bearerToken.startsWith(bearerPrefix)) {
            bearerToken.substring(bearerPrefix.length)
        } else {
            null
        }
    }
}
