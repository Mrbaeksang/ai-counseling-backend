package com.aicounseling.app.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class CorsConfig(
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOriginPatterns = corsProperties.allowedOrigins
                allowedMethods = corsProperties.allowedMethods
                allowedHeaders = corsProperties.allowedHeaders
                allowCredentials = corsProperties.allowCredentials
                maxAge = corsProperties.maxAge
            }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    // OAuth2 리다이렉트를 위해 구체적인 origin 필요
    // "*"와 allowCredentials=true는 함께 사용 불가
    val allowedOrigins: List<String> = listOf("http://localhost:3000"),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("*"),
    // OAuth2 쿠키 전송에 필요
    val allowCredentials: Boolean = true,
    val maxAge: Long = 3600,
)
