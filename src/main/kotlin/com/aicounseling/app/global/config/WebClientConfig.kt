package com.aicounseling.app.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * WebClient 전역 설정
 *
 * OAuth2 Client 활성화 시 자동으로 사용됨
 * 현재는 OpenRouter와 OAuth Token Verifier가 자체 WebClient 사용 중
 * 향후 OAuth2 활성화 후 WebClient 통합 검토 예정
 */
@Configuration
class WebClientConfig {
    companion object {
        private const val MAX_MEMORY_SIZE_MB = 5
        private const val BYTES_PER_KB = 1024
        private const val KB_PER_MB = 1024
        private const val MAX_IN_MEMORY_SIZE = MAX_MEMORY_SIZE_MB * KB_PER_MB * BYTES_PER_KB
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE)
            }
            .build()
    }
}
