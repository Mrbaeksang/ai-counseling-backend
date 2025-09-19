package com.aicounseling.app.global.auth.controller

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.auth.dto.AuthResponse
import com.aicounseling.app.global.rsData.RsData
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Google Play 심사용 테스트 인증 컨트롤러
 * 프로덕션 환경에서만 활성화됨
 */
@Tag(name = "Test Auth", description = "Google Play 심사용 테스트 인증 API")
@RestController
@RequestMapping("/api/auth")
@Profile("!test") // 테스트 환경에서는 비활성화
class TestAuthController(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    @Operation(
        summary = "Google Play 심사용 Google OAuth 테스트 로그인",
        description = "Google Play 심사팀을 위한 Google OAuth 테스트 계정 자동 로그인"
    )
    @PostMapping("/test-login-google")
    @Transactional
    fun testLoginGoogle(): ResponseEntity<RsData<AuthResponse>> {
        // Google OAuth 테스트 계정 찾기 또는 생성
        val testEmail = "test.google@drmind.com"
        val testUser = userRepository.findByEmailAndAuthProvider(testEmail, AuthProvider.GOOGLE)
            ?: run {
                val newUser = User(
                    email = testEmail,
                    nickname = "Google Play Test (Google)",
                    authProvider = AuthProvider.GOOGLE,
                    providerId = "google-play-test-google-001",
                    profileImageUrl = null,
                    isActive = true
                )
                userRepository.save(newUser)
            }

        // ID 검증
        val userId = testUser.id.takeIf { it != 0L }
            ?: throw IllegalStateException("User ID not generated")

        // JWT 토큰 생성
        val accessToken = jwtTokenProvider.createToken(userId, testUser.email)
        val refreshToken = jwtTokenProvider.createRefreshToken(userId)

        val authResponse = AuthResponse(
            userId = userId,
            email = testUser.email,
            nickname = testUser.nickname,
            accessToken = accessToken,
            refreshToken = refreshToken
        )

        return ResponseEntity.ok(
            RsData.of("S-1", "Google OAuth 테스트 로그인 성공", authResponse)
        )
    }

    @Operation(
        summary = "Google Play 심사용 Kakao OAuth 테스트 로그인",
        description = "Google Play 심사팀을 위한 Kakao OAuth 테스트 계정 자동 로그인"
    )
    @PostMapping("/test-login-kakao")
    @Transactional
    fun testLoginKakao(): ResponseEntity<RsData<AuthResponse>> {
        // Kakao OAuth 테스트 계정 찾기 또는 생성
        val testEmail = "test.kakao@drmind.com"
        val testUser = userRepository.findByEmailAndAuthProvider(testEmail, AuthProvider.KAKAO)
            ?: run {
                val newUser = User(
                    email = testEmail,
                    nickname = "Google Play Test (Kakao)",
                    authProvider = AuthProvider.KAKAO,
                    providerId = "google-play-test-kakao-001",
                    profileImageUrl = null,
                    isActive = true
                )
                userRepository.save(newUser)
            }

        // ID 검증
        val userId = testUser.id.takeIf { it != 0L }
            ?: throw IllegalStateException("User ID not generated")

        // JWT 토큰 생성
        val accessToken = jwtTokenProvider.createToken(userId, testUser.email)
        val refreshToken = jwtTokenProvider.createRefreshToken(userId)

        val authResponse = AuthResponse(
            userId = userId,
            email = testUser.email,
            nickname = testUser.nickname,
            accessToken = accessToken,
            refreshToken = refreshToken
        )

        return ResponseEntity.ok(
            RsData.of("S-1", "Kakao OAuth 테스트 로그인 성공", authResponse)
        )
    }
}