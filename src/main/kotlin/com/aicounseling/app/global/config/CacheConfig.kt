package com.aicounseling.app.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.lettuce.core.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

private const val DEFAULT_CACHE_TTL_MINUTES = 10L
private const val USER_CACHE_TTL_HOURS = 6L
private const val CHARACTER_LIST_CACHE_TTL_MINUTES = 5L
private const val CHARACTER_DETAIL_CACHE_TTL_MINUTES = 10L
private const val USER_SESSION_CACHE_TTL_SECONDS = 60L
private const val SESSION_MESSAGES_CACHE_TTL_SECONDS = 30L
private const val OAUTH_TOKEN_CACHE_TTL_SECONDS = 60L

private val DEFAULT_CACHE_TTL = Duration.ofMinutes(DEFAULT_CACHE_TTL_MINUTES)
private val USER_CACHE_TTL = Duration.ofHours(USER_CACHE_TTL_HOURS)
private val CHARACTER_LIST_CACHE_TTL = Duration.ofMinutes(CHARACTER_LIST_CACHE_TTL_MINUTES)
private val CHARACTER_DETAIL_CACHE_TTL = Duration.ofMinutes(CHARACTER_DETAIL_CACHE_TTL_MINUTES)
private val USER_SESSION_CACHE_TTL = Duration.ofSeconds(USER_SESSION_CACHE_TTL_SECONDS)
private val SESSION_MESSAGES_CACHE_TTL = Duration.ofSeconds(SESSION_MESSAGES_CACHE_TTL_SECONDS)
private val OAUTH_TOKEN_CACHE_TTL = Duration.ofSeconds(OAUTH_TOKEN_CACHE_TTL_SECONDS)

@Configuration
@ConditionalOnProperty(value = ["redis.enabled"], havingValue = "true", matchIfMissing = true)
class CacheConfig(
    @Value("\${REDIS_URL:}") private val redisUrl: String,
) {
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        if (redisUrl.isBlank()) {
            return LettuceConnectionFactory()
        }

        val uri = RedisURI.create(redisUrl)
        val configuration = RedisStandaloneConfiguration(uri.host, uri.port)

        @Suppress("DEPRECATION")
        val passwordChars: CharArray? = uri.password
        if (passwordChars != null && passwordChars.isNotEmpty()) {
            val password = String(passwordChars)
            configuration.password = RedisPassword.of(password)
        }
        return LettuceConnectionFactory(configuration)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val serializer = createRedisSerializer()
        return RedisTemplate<String, Any>().apply {
            this.connectionFactory = connectionFactory
            keySerializer = StringRedisSerializer()
            valueSerializer = serializer
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = serializer
            afterPropertiesSet()
        }
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val serializer = createRedisSerializer()
        val defaultConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(DEFAULT_CACHE_TTL)
                .disableCachingNullValues()

        val cacheConfigurations =
            mapOf(
                "user" to defaultConfig.entryTtl(USER_CACHE_TTL),
                "character:list" to defaultConfig.entryTtl(CHARACTER_LIST_CACHE_TTL),
                "character:detail" to defaultConfig.entryTtl(CHARACTER_DETAIL_CACHE_TTL),
                "user-sessions" to defaultConfig.entryTtl(USER_SESSION_CACHE_TTL),
                "session-messages" to defaultConfig.entryTtl(SESSION_MESSAGES_CACHE_TTL),
                "oauth:google-token" to defaultConfig.entryTtl(OAUTH_TOKEN_CACHE_TTL),
                "oauth:kakao-token" to defaultConfig.entryTtl(OAUTH_TOKEN_CACHE_TTL),
            )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    private fun createRedisSerializer(): GenericJackson2JsonRedisSerializer {
        val objectMapper: ObjectMapper =
            Jackson2ObjectMapperBuilder.json()
                .modulesToInstall(JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build()

        // 기본 GenericJackson2JsonRedisSerializer 사용 (타입 정보 자동 처리)
        return GenericJackson2JsonRedisSerializer(objectMapper)
    }
}
