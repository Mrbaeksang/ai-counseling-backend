package com.aicounseling.app.global.auth.service

import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import com.aicounseling.app.global.exception.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class KakaoTokenVerifier(
    private val webClient: WebClient,
) : OAuthTokenVerifier {
    override fun verifyToken(token: String): Mono<OAuthUserInfo> {
        return webClient.get()
            .uri("https://kapi.kakao.com/v2/user/me")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono(KakaoUserInfo::class.java)
            .map { info ->
                OAuthUserInfo(
                    providerId = info.id.toString(),
                    // 이메일이 없을 경우 빈 문자열
                    email = info.kakao_account?.email ?: "",
                    name = info.kakao_account?.profile?.nickname,
                    picture = info.kakao_account?.profile?.profile_image_url,
                    provider = "KAKAO",
                )
            }
            .onErrorMap { UnauthorizedException("유효하지 않은 Kakao 토큰입니다") }
    }

    data class KakaoUserInfo(
        val id: Long,
        @Suppress("ConstructorParameterNaming") val kakao_account: KakaoAccount?,
    )

    data class KakaoAccount(
        val email: String?,
        val profile: KakaoProfile?,
    )

    data class KakaoProfile(
        val nickname: String?,
        @Suppress("ConstructorParameterNaming") val profile_image_url: String?,
    )
}
