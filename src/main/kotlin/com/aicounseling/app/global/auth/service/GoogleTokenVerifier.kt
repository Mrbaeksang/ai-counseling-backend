package com.aicounseling.app.global.auth.service

import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import com.aicounseling.app.global.exception.UnauthorizedException
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class GoogleTokenVerifier(
    private val webClient: WebClient,
) : OAuthTokenVerifier {
    override fun verifyToken(token: String): Mono<OAuthUserInfo> {
        return webClient.get()
            .uri("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=$token")
            .retrieve()
            .bodyToMono(GoogleTokenInfo::class.java)
            .map { info ->
                if (info.emailVerified != true) {
                    throw UnauthorizedException("이메일 인증이 필요합니다")
                }
                OAuthUserInfo(
                    providerId = info.sub,
                    email = info.email,
                    name = info.name,
                    provider = "GOOGLE",
                    picture = info.picture,
                )
            }
            .onErrorMap { UnauthorizedException("유효하지 않은 Google 토큰입니다") }
    }

    data class GoogleTokenInfo(
        val sub: String,
        val email: String,
        @JsonProperty("email_verified")
        val emailVerified: Boolean?,
        val name: String?,
        val picture: String? = null,
    )
}
