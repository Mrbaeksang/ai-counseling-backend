package com.aicounseling.app.global.auth.service

import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import reactor.core.publisher.Mono

interface OAuthTokenVerifier {
    fun verifyToken(token: String): Mono<OAuthUserInfo>
}
