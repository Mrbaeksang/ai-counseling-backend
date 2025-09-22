package com.aicounseling.app.global.auth.service

import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class OAuthTokenCacheService(
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val kakaoTokenVerifier: KakaoTokenVerifier,
) {
    @Cacheable(cacheNames = ["oauth:google-token"], key = "#token")
    fun getGoogleUserInfo(token: String): OAuthUserInfo =
        runBlocking {
            googleTokenVerifier.verifyToken(token).awaitSingle()
        }

    @Cacheable(cacheNames = ["oauth:kakao-token"], key = "#token")
    fun getKakaoUserInfo(token: String): OAuthUserInfo =
        runBlocking {
            kakaoTokenVerifier.verifyToken(token).awaitSingle()
        }
}
