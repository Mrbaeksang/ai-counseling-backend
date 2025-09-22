package com.aicounseling.app.global.auth.service

import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import com.aicounseling.app.global.exception.UnauthorizedException
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class GoogleTokenVerifier(
    private val webClient: WebClient,
    @Value("\${GOOGLE_CLIENT_ID:}") private val googleClientId: String,
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
                if (googleClientId.isNotBlank() && info.aud != null && info.aud != googleClientId) {
                    throw UnauthorizedException("클라이언트 정보가 일치하지 않습니다")
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
        val aud: String? = null,
    )
}
