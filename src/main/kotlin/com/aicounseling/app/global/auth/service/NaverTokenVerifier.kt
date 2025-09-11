package com.aicounseling.app.global.auth.service

import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import com.aicounseling.app.global.exception.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class NaverTokenVerifier(
    private val webClient: WebClient,
) : OAuthTokenVerifier {
    override fun verifyToken(token: String): Mono<OAuthUserInfo> {
        return webClient.get()
            .uri("https://openapi.naver.com/v1/nid/me")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono(NaverUserResponse::class.java)
            .map { response ->
                val info = response.response
                OAuthUserInfo(
                    providerId = info.id,
                    email = info.email ?: throw UnauthorizedException("이메일 정보가 없습니다"),
                    name = info.name,
                    provider = "NAVER",
                )
            }
            .onErrorMap { UnauthorizedException("유효하지 않은 Naver 토큰입니다") }
    }

    data class NaverUserResponse(
        val response: NaverUserInfo,
    )

    data class NaverUserInfo(
        val id: String,
        val email: String?,
        val name: String?,
    )
}
