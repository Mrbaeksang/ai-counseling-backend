package com.aicounseling.app.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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

@Configuration
@ConditionalOnProperty(value = ["redis.enabled"], havingValue = "true", matchIfMissing = true)
class CacheConfig(
    @Value("\${REDIS_URL:}") private val redisUrl: String,
    private val objectMapper: ObjectMapper,
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
        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)
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
        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)
        val defaultConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()

        val cacheConfigurations = mapOf(
            "user" to defaultConfig.entryTtl(Duration.ofHours(6)),
            "counselor:list" to defaultConfig.entryTtl(Duration.ofMinutes(5)),
            "counselor:detail" to defaultConfig.entryTtl(Duration.ofMinutes(10)),
            "user-sessions" to defaultConfig.entryTtl(Duration.ofSeconds(60)),
            "session-messages" to defaultConfig.entryTtl(Duration.ofSeconds(30)),
            "oauth:google-token" to defaultConfig.entryTtl(Duration.ofSeconds(60)),
            "oauth:kakao-token" to defaultConfig.entryTtl(Duration.ofSeconds(60)),
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
