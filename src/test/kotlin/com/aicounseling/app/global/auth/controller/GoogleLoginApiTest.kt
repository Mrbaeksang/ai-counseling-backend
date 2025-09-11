package com.aicounseling.app.global.auth.controller

import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.auth.dto.OAuthLoginRequest
import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import reactor.core.publisher.Mono

/**
 * Google OAuth 로그인 API 테스트
 * POST /api/auth/google
 */
@DisplayName("Google OAuth 로그인 API 테스트")
class GoogleLoginApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: ObjectMapper,
        jwtTokenProvider: JwtTokenProvider,
        userRepository: UserRepository,
    ) : AuthControllerBaseTest(mockMvc, objectMapper, jwtTokenProvider, userRepository) {
        @Test
        @DisplayName("신규 사용자 Google 로그인 성공")
        fun `신규 사용자가 Google OAuth로 로그인하면 새 계정이 생성되고 토큰이 발급된다`() {
            // Given
            val request = OAuthLoginRequest(token = "valid-google-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("구글 로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.email").value(googleUserInfo.email))
                .andExpect(jsonPath("$.data.nickname").value(googleUserInfo.name))

            // 사용자가 DB에 생성되었는지 확인
            val createdUser =
                userRepository.findByProviderIdAndAuthProvider(
                    googleUserInfo.providerId,
                    AuthProvider.GOOGLE,
                )
            assertNotNull(createdUser)
            assertEquals(AuthProvider.GOOGLE, createdUser?.authProvider)
            assertEquals(googleUserInfo.providerId, createdUser?.providerId)
            assertEquals(googleUserInfo.picture, createdUser?.profileImageUrl)
        }

        @Test
        @DisplayName("기존 사용자 Google 로그인 성공")
        fun `기존 사용자가 Google OAuth로 로그인하면 기존 계정 정보와 새 토큰이 반환된다`() {
            // Given
            val existingGoogleUser =
                userRepository.save(
                    com.aicounseling.app.domain.user.entity.User(
                        email = googleUserInfo.email,
                        nickname = "기존닉네임",
                        authProvider = AuthProvider.GOOGLE,
                        providerId = googleUserInfo.providerId,
                        profileImageUrl = "old-profile.jpg",
                    ),
                )

            val request = OAuthLoginRequest(token = "valid-google-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("구글 로그인 성공"))
                .andExpect(jsonPath("$.data.userId").value(existingGoogleUser.id))
                .andExpect(jsonPath("$.data.email").value(existingGoogleUser.email))
                .andExpect(jsonPath("$.data.nickname").value("기존닉네임")) // 기존 닉네임 유지

            // 사용자 수가 증가하지 않았는지 확인
            assertEquals(2, userRepository.count()) // existingUser(BeforeEach) + existingGoogleUser
        }

        @Test
        @DisplayName("유효하지 않은 Google 토큰으로 로그인 실패")
        fun `유효하지 않은 Google OAuth 토큰으로 로그인하면 401 에러가 반환된다`() {
            // Given
            coEvery { googleTokenVerifier.verifyToken(any()) } returns
                Mono.error(IllegalStateException("Invalid token"))

            val request = OAuthLoginRequest(token = "invalid-google-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        @DisplayName("빈 토큰으로 Google 로그인 실패")
        fun `빈 토큰으로 Google 로그인하면 400 에러가 반환된다`() {
            // Given
            val invalidRequest = """{"token": ""}"""

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-400"))
                .andExpect(jsonPath("$.msg").exists())
        }

        @Test
        @DisplayName("다른 OAuth 제공자로 이미 가입한 이메일로 Google 로그인 시도")
        fun `다른 OAuth 제공자로 이미 가입한 이메일로 Google 로그인하면 새 계정이 생성된다`() {
            // Given - Kakao로 이미 가입한 사용자
            val kakaoUser =
                userRepository.save(
                    com.aicounseling.app.domain.user.entity.User(
                        // 같은 이메일
                        email = googleUserInfo.email,
                        nickname = "카카오유저",
                        authProvider = AuthProvider.KAKAO,
                        providerId = "kakao-different-id",
                    ),
                )

            val request = OAuthLoginRequest(token = "valid-google-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("구글 로그인 성공"))
                .andExpect(jsonPath("$.data.userId").exists())

            // 새로운 사용자가 생성되었는지 확인 (providerId와 authProvider로 구분)
            val users = userRepository.findAll()
            assertEquals(3, users.size) // existingUser + kakaoUser + 새로운 Google 사용자

            val newGoogleUser =
                userRepository.findByProviderIdAndAuthProvider(
                    googleUserInfo.providerId,
                    AuthProvider.GOOGLE,
                )
            assertNotNull(newGoogleUser)
            assertNotEquals(kakaoUser.id, newGoogleUser?.id)
        }

        @Test
        @DisplayName("Google OAuth 응답에 이메일이 없는 경우")
        fun `Google OAuth 응답에 이메일이 없으면 401 에러가 반환된다`() {
            // Given
            val noEmailUserInfo =
                OAuthUserInfo(
                    providerId = "google-no-email-id",
                    // 빈 이메일
                    email = "",
                    name = "No Email User",
                    provider = "GOOGLE",
                    picture = null,
                )
            coEvery { googleTokenVerifier.verifyToken(any()) } returns Mono.just(noEmailUserInfo)

            val request = OAuthLoginRequest(token = "valid-google-oauth-token-no-email")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-401"))
        }

        @Test
        @DisplayName("로그인 성공 시 lastLoginAt 업데이트")
        fun `기존 사용자가 로그인하면 lastLoginAt이 업데이트된다`() {
            // Given
            val existingGoogleUser =
                userRepository.save(
                    com.aicounseling.app.domain.user.entity.User(
                        email = googleUserInfo.email,
                        nickname = "기존유저",
                        authProvider = AuthProvider.GOOGLE,
                        providerId = googleUserInfo.providerId,
                    ),
                )

            val originalLastLoginAt = existingGoogleUser.lastLoginAt
            Thread.sleep(100) // 시간 차이를 두기 위해

            val request = OAuthLoginRequest(token = "valid-google-oauth-token")

            // When
            mockMvc
                .perform(
                    post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)

            // Then
            val updatedUser = userRepository.findById(existingGoogleUser.id).get()
            assertNotEquals(originalLastLoginAt, updatedUser.lastLoginAt)
        }
    }
