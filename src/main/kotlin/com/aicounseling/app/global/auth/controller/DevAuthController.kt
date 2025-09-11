package com.aicounseling.app.global.auth.controller

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.auth.dto.AuthResponse
import com.aicounseling.app.global.rsData.RsData
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 개발 환경 전용 인증 컨트롤러
 * OAuth 없이 테스트용 토큰을 발급받을 수 있습니다.
 */
@RestController
@RequestMapping("/api/dev/auth")
@Profile("dev", "local") // 개발 환경에서만 활성화
class DevAuthController(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    /**
     * 테스트용 JWT 토큰 발급
     * @param email 테스트 사용자 이메일 (기본값: test@example.com)
     */
    @PostMapping("/test-token")
    fun getTestToken(
        @RequestParam(defaultValue = "test@example.com") email: String,
    ): RsData<AuthResponse> {
        // 테스트 사용자 찾기 - providerId를 email 기반으로 생성
        val testProviderId = "test-${email.substringBefore("@")}"
        val user =
            userRepository.findByProviderIdAndAuthProvider(
                testProviderId,
                AuthProvider.GOOGLE,
            ) ?: run {
                val newUser =
                    User(
                        email = email,
                        nickname =
                            when (email) {
                                "test@example.com" -> "테스트유저"
                                "demo@example.com" -> "데모유저"
                                "admin@example.com" -> "관리자"
                                else -> "테스트유저"
                            },
                        authProvider = AuthProvider.GOOGLE,
                        providerId = testProviderId,
                    )
                return@run userRepository.saveAndFlush(newUser)
            }

        // JWT 토큰 생성
        val accessToken =
            jwtTokenProvider.createToken(
                userId = user.id,
                email = user.email,
            )
        val refreshToken = jwtTokenProvider.createRefreshToken(user.id)

        return RsData.of(
            "S-1",
            "테스트용 토큰이 발급되었습니다.",
            AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = user.id,
                email = user.email,
                nickname = user.nickname,
            ),
        )
    }

    /**
     * 사용 가능한 테스트 사용자 목록 조회
     */
    @GetMapping("/test-users")
    fun getTestUsers(): RsData<List<Map<String, Any?>>> {
        // InitDataConfig에서 생성한 테스트 사용자들 조회
        val users = mutableListOf<Map<String, Any?>>()

        // Google 사용자
        userRepository.findByProviderIdAndAuthProvider("google-test-123", AuthProvider.GOOGLE)?.let {
            users.add(
                mapOf(
                    "id" to it.id,
                    "email" to it.email,
                    "nickname" to it.nickname,
                    "provider" to "GOOGLE",
                ),
            )
        }

        // Kakao 사용자
        userRepository.findByProviderIdAndAuthProvider("kakao-demo-456", AuthProvider.KAKAO)?.let {
            users.add(
                mapOf(
                    "id" to it.id,
                    "email" to it.email,
                    "nickname" to it.nickname,
                    "provider" to "KAKAO",
                ),
            )
        }

        // Naver 사용자
        userRepository.findByProviderIdAndAuthProvider("naver-admin-789", AuthProvider.NAVER)?.let {
            users.add(
                mapOf(
                    "id" to it.id,
                    "email" to it.email,
                    "nickname" to it.nickname,
                    "provider" to "NAVER",
                ),
            )
        }

        return RsData.of(
            "S-1",
            "테스트 사용자 목록",
            users,
        )
    }
}
