package com.aicounseling.app.global.config

import com.aicounseling.app.global.auth.handler.OAuth2AuthenticationSuccessHandler
import com.aicounseling.app.global.security.JwtAuthenticationFilter
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper,
    private val corsConfigurationSource: CorsConfigurationSource,
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/actuator/health").permitAll()
                    // SpringDoc/Swagger endpoints
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/swagger-ui.html").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/dev/auth/**").permitAll() // 개발 환경 테스트용 인증
                    .requestMatchers("/api/health/**").permitAll() // 헬스체크 엔드포인트
                    .requestMatchers("/h2-console/**").permitAll()
                    // OAuth2 endpoints
                    .requestMatchers("/oauth2/**").permitAll()
                    .requestMatchers("/login/oauth2/**").permitAll()
                    // Public character endpoints (목록, 상세 조회는 인증 불필요)
                    .requestMatchers(HttpMethod.GET, "/api/characters").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/characters/*").permitAll()
                    // Protected endpoints (나머지 /api/** 는 인증 필요)
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler { _, response, exception ->
                        // OAuth2 로그인 실패 시 에러 페이지로 리다이렉트
                        val errorMessage = exception.message ?: "OAuth2 로그인 실패"
                        response.sendRedirect("/oauth2/error?message=$errorMessage")
                    }
            }
            .exceptionHandling { exception ->
                exception
                    .authenticationEntryPoint { _, response, _ ->
                        response.status = HttpStatus.UNAUTHORIZED.value()
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "resultCode" to "F-401",
                                    "msg" to "로그인이 필요합니다",
                                    "data" to null,
                                ),
                            ),
                        )
                    }
                    .accessDeniedHandler { _, response, _ ->
                        response.status = HttpStatus.FORBIDDEN.value()
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "resultCode" to "F-403",
                                    "msg" to "권한이 없습니다",
                                    "data" to null,
                                ),
                            ),
                        )
                    }
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() } // H2 Console을 위해
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
