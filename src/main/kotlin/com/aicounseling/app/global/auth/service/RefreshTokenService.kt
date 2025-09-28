package com.aicounseling.app.global.auth.service

import com.aicounseling.app.global.exception.UnauthorizedException
import com.aicounseling.app.global.security.JwtTokenProvider
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshTokenService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    private val valueOps = stringRedisTemplate.opsForValue()
    private val setOps = stringRedisTemplate.opsForSet()

    fun save(
        userId: Long,
        refreshToken: String,
    ) {
        val ttl = Duration.ofMillis(jwtTokenProvider.getRefreshExpirationMillis())
        val tokenKey = tokenKey(refreshToken)
        valueOps.set(tokenKey, userId.toString(), ttl)

        val userKey = userKey(userId)
        setOps.add(userKey, refreshToken)
        stringRedisTemplate.expire(userKey, ttl)
    }

    fun rotate(
        userId: Long,
        oldToken: String,
        newToken: String,
    ) {
        val tokenKey = tokenKey(oldToken)
        val storedUserId = valueOps.get(tokenKey) ?: throw UnauthorizedException("만료된 리프레시 토큰입니다")
        if (storedUserId != userId.toString()) {
            throw UnauthorizedException("잘못된 리프레시 토큰입니다")
        }

        // 기존 토큰 제거
        stringRedisTemplate.delete(tokenKey)
        setOps.remove(userKey(userId), oldToken)

        // 새 토큰 저장
        save(userId, newToken)
    }

    fun deleteAllByUserId(userId: Long) {
        val userKey = userKey(userId)
        val tokens = setOps.members(userKey) ?: emptySet()
        if (tokens.isNotEmpty()) {
            tokens.forEach { stringRedisTemplate.delete(tokenKey(it)) }
        }
        stringRedisTemplate.delete(userKey)
    }

    private fun tokenKey(token: String) = "refresh-token:$token"

    private fun userKey(userId: Long) = "refresh-token:user:$userId"
}
