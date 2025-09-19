package com.aicounseling.app.global.auth.handler

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@Component
class OAuth2AuthenticationSuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
) : SimpleUrlAuthenticationSuccessHandler() {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oauth2Token = authentication as OAuth2AuthenticationToken
        val oauth2User = oauth2Token.principal as OAuth2User
        val provider = oauth2Token.authorizedClientRegistrationId.uppercase()

        // OAuth2 사용자 정보 추출
        val email = extractEmail(oauth2User, provider)
        val nickname = extractName(oauth2User, provider)
        val providerId = extractProviderId(oauth2User, provider)
        val authProvider = AuthProvider.valueOf(provider)

        // 사용자 조회 또는 생성
        val user =
            userRepository.findByEmailAndAuthProvider(email, authProvider) ?: userRepository.save(
                User(
                    email = email,
                    nickname = nickname,
                    authProvider = authProvider,
                    providerId = providerId,
                    profileImageUrl = extractProfileImage(oauth2User, provider),
                    lastLoginAt = LocalDateTime.now(),
                ),
            )

        // 로그인 시간 업데이트
        user.lastLoginAt = LocalDateTime.now()
        userRepository.save(user)

        // JWT 토큰 생성
        val accessToken = jwtTokenProvider.createToken(user.id, user.email)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.id)

        // 모바일 앱으로 리다이렉트 (딥링크)
        val redirectUrl =
            if (isLocalDevelopment(request)) {
                // 개발 환경: Expo 앱으로 리다이렉트
                UriComponentsBuilder.fromUriString("exp://localhost:8081/--/auth/callback")
                    .queryParam("accessToken", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                    .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                    .queryParam("provider", provider)
                    .build().toUriString()
            } else {
                // 프로덕션: 실제 앱 스킴으로 리다이렉트
                UriComponentsBuilder.fromUriString("drmind://auth/callback")
                    .queryParam("accessToken", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                    .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                    .queryParam("provider", provider)
                    .build().toUriString()
            }

        redirectStrategy.sendRedirect(request, response, redirectUrl)
    }

    private fun extractEmail(
        oauth2User: OAuth2User,
        provider: String,
    ): String {
        val email =
            when (provider) {
                "GOOGLE" -> oauth2User.getAttribute<String>("email")
                "KAKAO" -> {
                    val kakaoAccount = oauth2User.getAttribute<Map<String, Any>>("kakao_account")
                    kakaoAccount?.get("email") as? String
                }
                else -> null
            }

        return email ?: throw IllegalArgumentException(
            when (provider) {
                "GOOGLE" -> "이메일 정보를 가져올 수 없습니다"
                "KAKAO" -> "카카오 이메일 정보를 가져올 수 없습니다"
                else -> "지원하지 않는 OAuth 제공자입니다: $provider"
            },
        )
    }

    private fun extractName(
        oauth2User: OAuth2User,
        provider: String,
    ): String {
        return when (provider) {
            "GOOGLE" -> oauth2User.getAttribute<String>("name") ?: "사용자"
            "KAKAO" -> {
                val properties = oauth2User.getAttribute<Map<String, Any>>("properties")
                properties?.get("nickname") as? String ?: "카카오 사용자"
            }
            else -> "사용자"
        }
    }

    private fun extractProviderId(
        oauth2User: OAuth2User,
        provider: String,
    ): String {
        return when (provider) {
            "GOOGLE" -> oauth2User.getAttribute<String>("sub") ?: ""
            "KAKAO" -> oauth2User.getAttribute<String>("id") ?: ""
            else -> ""
        }
    }

    private fun extractProfileImage(
        oauth2User: OAuth2User,
        provider: String,
    ): String? {
        return when (provider) {
            "GOOGLE" -> oauth2User.getAttribute<String>("picture")
            "KAKAO" -> {
                val properties = oauth2User.getAttribute<Map<String, Any>>("properties")
                properties?.get("profile_image") as? String
            }
            else -> null
        }
    }

    private fun isLocalDevelopment(request: HttpServletRequest): Boolean {
        val host = request.serverName
        return host == "localhost" || host == "127.0.0.1" || host.startsWith("192.168")
    }
}
