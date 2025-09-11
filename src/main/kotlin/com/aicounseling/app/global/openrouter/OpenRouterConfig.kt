package com.aicounseling.app.global.openrouter

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class OpenRouterConfig {
    companion object {
        private const val TIMEOUT_SECONDS = 60L
    }

    @Bean
    fun openRouterWebClient(properties: OpenRouterProperties): WebClient {
        val httpClient =
            HttpClient.create()
                .responseTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))

        return WebClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .defaultHeader("Authorization", "Bearer ${properties.apiKey}")
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("HTTP-Referer", properties.siteUrl)
            .defaultHeader("X-Title", properties.siteName)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}

@ConfigurationProperties(prefix = "openrouter")
data class OpenRouterProperties(
    val apiKey: String,
    val model: String = "meta-llama/llama-3.2-3b-instruct",
    val siteUrl: String = "http://localhost:8080",
    val siteName: String = "AI Counseling App",
)
