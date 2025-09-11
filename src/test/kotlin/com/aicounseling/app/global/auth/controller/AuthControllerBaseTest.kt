package com.aicounseling.app.global.auth.controller

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import com.aicounseling.app.global.auth.service.GoogleTokenVerifier
import com.aicounseling.app.global.auth.service.KakaoTokenVerifier
import com.aicounseling.app.global.auth.service.NaverTokenVerifier
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.github.cdimascio.dotenv.dotenv
import io.mockk.coEvery
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

/**
 * AuthController 테스트 기본 클래스
 * OAuth 인증 및 토큰 관리 테스트를 위한 공통 설정
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class AuthControllerBaseTest(
    protected val mockMvc: MockMvc,
    protected val objectMapper: ObjectMapper,
    protected val jwtTokenProvider: JwtTokenProvider,
    protected val userRepository: UserRepository,
) {
    @MockkBean(relaxed = true)
    protected lateinit var googleTokenVerifier: GoogleTokenVerifier

    @MockkBean(relaxed = true)
    protected lateinit var kakaoTokenVerifier: KakaoTokenVerifier

    @MockkBean(relaxed = true)
    protected lateinit var naverTokenVerifier: NaverTokenVerifier

    companion object {
        private val dotenv =
            dotenv {
                ignoreIfMissing = true
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val apiKey = System.getenv("OPENROUTER_API_KEY") ?: dotenv["OPENROUTER_API_KEY"] ?: "test-api-key"
            registry.add("openrouter.api-key") { apiKey }
            registry.add("jwt.secret") {
                System.getenv("JWT_SECRET")
                    ?: dotenv["JWT_SECRET"]
                    ?: "test-jwt-secret-key-for-jwt-auth-512-bits-long-2025-with-extra-characters-for-security"
            }

            // OAuth 설정
            registry.add("oauth2.google.client-id") { "test-google-client-id" }
            registry.add("oauth2.kakao.client-id") { "test-kakao-client-id" }
            registry.add("oauth2.naver.client-id") { "test-naver-client-id" }
            registry.add("oauth2.naver.client-secret") { "test-naver-client-secret" }
        }
    }

    // 테스트용 사용자 데이터
    protected lateinit var existingUser: User
    protected lateinit var authToken: String
    protected lateinit var refreshToken: String

    // Mock OAuth 응답 데이터
    protected val googleUserInfo =
        OAuthUserInfo(
            providerId = "google-test-id-123",
            email = "google.test@example.com",
            name = "Google 테스트",
            provider = "GOOGLE",
            picture = "https://example.com/google-profile.jpg",
        )

    protected val kakaoUserInfo =
        OAuthUserInfo(
            providerId = "kakao-test-id-456",
            email = "kakao.test@example.com",
            name = "카카오 테스트",
            provider = "KAKAO",
            picture = "https://example.com/kakao-profile.jpg",
        )

    protected val naverUserInfo =
        OAuthUserInfo(
            providerId = "naver-test-id-789",
            email = "naver.test@example.com",
            name = "네이버 테스트",
            provider = "NAVER",
            picture = "https://example.com/naver-profile.jpg",
        )

    @BeforeEach
    fun setupTestData() {
        // Mock 설정 - Google
        coEvery { googleTokenVerifier.verifyToken(any()) } returns Mono.just(googleUserInfo)

        // Mock 설정 - Kakao
        coEvery { kakaoTokenVerifier.verifyToken(any()) } returns Mono.just(kakaoUserInfo)

        // Mock 설정 - Naver
        coEvery { naverTokenVerifier.verifyToken(any()) } returns Mono.just(naverUserInfo)

        // 기존 사용자 생성 (토큰 갱신 테스트용)
        existingUser =
            userRepository.save(
                User(
                    email = "existing@example.com",
                    nickname = "기존유저",
                    authProvider = AuthProvider.GOOGLE,
                    providerId = "google-existing-id",
                    profileImageUrl = "https://example.com/existing-profile.jpg",
                ),
            )

        // JWT 토큰 생성
        authToken = jwtTokenProvider.createToken(existingUser.id, existingUser.email)
        refreshToken = jwtTokenProvider.createRefreshToken(existingUser.id)
    }

    @AfterEach
    fun cleanupTestData() {
        userRepository.deleteAll()
    }

    /**
     * 테스트용 유효하지 않은 토큰
     */
    protected val invalidToken = "invalid.jwt.token"

    /**
     * 테스트용 만료된 토큰
     * 실제로는 JwtTokenProvider에 만료 시간을 설정할 수 있는 메서드가 필요
     * 여기서는 간단히 invalid token 사용
     */
    protected val expiredToken = "expired.jwt.token"
}
