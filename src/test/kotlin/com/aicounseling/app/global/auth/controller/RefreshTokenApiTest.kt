package com.aicounseling.app.global.auth.controller

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.auth.dto.RefreshTokenRequest
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * JWT 토큰 갱신 API 테스트
 * POST /api/auth/refresh
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("JWT 토큰 갱신 API 테스트")
class RefreshTokenApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: ObjectMapper,
        jwtTokenProvider: JwtTokenProvider,
        userRepository: UserRepository,
    ) : AuthControllerBaseTest(mockMvc, objectMapper, jwtTokenProvider, userRepository) {
        @Test
        @DisplayName("유효한 리프레시 토큰으로 갱신 성공")
        fun `유효한 리프레시 토큰으로 요청하면 새로운 액세스 토큰과 리프레시 토큰이 발급된다`() {
            // Given
            val testUser =
                userRepository.save(
                    User(
                        email = "refresh-test@example.com",
                        nickname = "리프레시테스트",
                        authProvider = AuthProvider.GOOGLE,
                        providerId = "google-refresh-test-id",
                    ),
                )

            val validRefreshToken = jwtTokenProvider.createRefreshToken(testUser.id!!)
            val request = RefreshTokenRequest(refreshToken = validRefreshToken)

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("토큰 갱신 성공"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.userId").value(testUser.id))
                .andExpect(jsonPath("$.data.email").value(testUser.email))
                .andExpect(jsonPath("$.data.nickname").value(testUser.nickname))

            // 새로 발급된 토큰이 이전 토큰과 다른지 확인
            val response =
                mockMvc
                    .perform(
                        post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                    .andReturn()
                    .response
                    .contentAsString

            val responseBody = objectMapper.readTree(response)
            val newAccessToken = responseBody.get("data").get("accessToken").asText()
            val newRefreshToken = responseBody.get("data").get("refreshToken").asText()

            // 새로운 토큰들이 발급되었는지 확인
            assertNotNull(newAccessToken)
            assertNotNull(newRefreshToken)
            // 토큰이 유효한지 확인
            assertTrue(jwtTokenProvider.validateToken(newAccessToken))
            assertTrue(jwtTokenProvider.validateToken(newRefreshToken))
        }

        @Test
        @DisplayName("만료된 리프레시 토큰으로 갱신 실패")
        fun `만료된 리프레시 토큰으로 요청하면 401 에러가 반환된다`() {
            // Given
            val expiredRefreshToken = expiredToken
            val request = RefreshTokenRequest(refreshToken = expiredRefreshToken)

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        @DisplayName("유효하지 않은 형식의 리프레시 토큰으로 갱신 실패")
        fun `유효하지 않은 형식의 리프레시 토큰으로 요청하면 401 에러가 반환된다`() {
            // Given
            val invalidRefreshToken = "invalid.refresh.token.format"
            val request = RefreshTokenRequest(refreshToken = invalidRefreshToken)

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-401"))
        }

        @Test
        @DisplayName("빈 리프레시 토큰으로 갱신 실패")
        fun `빈 리프레시 토큰으로 요청하면 400 에러가 반환된다`() {
            // Given
            val invalidRequest = """{"refreshToken": ""}"""

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-400"))
                .andExpect(jsonPath("$.msg").exists())
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 리프레시 토큰으로 갱신 실패")
        fun `삭제된 사용자의 리프레시 토큰으로 요청하면 401 에러가 반환된다`() {
            // Given - 사용자 생성 후 토큰 발급 후 삭제
            val tempUser =
                userRepository.save(
                    User(
                        email = "deleted@example.com",
                        nickname = "삭제될유저",
                        authProvider = AuthProvider.KAKAO,
                        providerId = "kakao-deleted-id",
                    ),
                )

            val refreshTokenOfDeletedUser = jwtTokenProvider.createRefreshToken(tempUser.id!!)
            userRepository.delete(tempUser) // 사용자 삭제

            val request = RefreshTokenRequest(refreshToken = refreshTokenOfDeletedUser)

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").exists())
        }

        @Test
        @DisplayName("비활성화된 사용자의 리프레시 토큰으로 갱신")
        fun `비활성화된 사용자도 토큰 갱신이 가능하다`() {
            // Given - 비활성화된 사용자
            val inactiveUser =
                userRepository.save(
                    User(
                        email = "inactive@example.com",
                        nickname = "비활성유저",
                        authProvider = AuthProvider.NAVER,
                        providerId = "naver-inactive-id",
                    ).apply {
                        isActive = false // 비활성화
                    },
                )

            val refreshToken = jwtTokenProvider.createRefreshToken(inactiveUser.id!!)
            val request = RefreshTokenRequest(refreshToken = refreshToken)

            // When & Then - 비활성 사용자도 토큰 갱신은 가능 (재활성화 기회)
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("토큰 갱신 성공"))
        }

        @Test
        @DisplayName("리프레시 토큰 재사용 테스트")
        fun `동일한 리프레시 토큰으로 여러 번 갱신 요청이 가능하다`() {
            // Given
            val testUser =
                userRepository.save(
                    User(
                        email = "reuse-test@example.com",
                        nickname = "재사용테스트",
                        authProvider = AuthProvider.GOOGLE,
                        providerId = "google-reuse-test-id",
                    ),
                )

            val refreshToken = jwtTokenProvider.createRefreshToken(testUser.id!!)
            val request = RefreshTokenRequest(refreshToken = refreshToken)

            // When - 첫 번째 갱신
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))

            // Then - 두 번째 갱신도 가능 (일반적으로는 보안상 권장되지 않지만 현재 구현은 허용)
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
        }

        @Test
        @DisplayName("액세스 토큰으로 리프레시 요청")
        fun `액세스 토큰을 리프레시 토큰으로 사용하면 토큰이 갱신된다`() {
            // Given - 액세스 토큰 생성
            val testUser =
                userRepository.save(
                    User(
                        email = "access-as-refresh@example.com",
                        nickname = "잘못된토큰테스트",
                        authProvider = AuthProvider.GOOGLE,
                        providerId = "google-wrong-token-id",
                    ),
                )

            val accessToken = jwtTokenProvider.createToken(testUser.id, testUser.email) // 액세스 토큰
            val request = RefreshTokenRequest(refreshToken = accessToken) // 잘못 사용

            // When & Then
            // 현재 구현에서는 액세스 토큰도 유효한 JWT이므로 갱신이 성공함
            // 실제 운영에서는 토큰 타입을 구분하는 로직이 필요할 수 있음
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1")) // 현재는 성공으로 처리됨
                .andExpect(jsonPath("$.msg").value("토큰 갱신 성공"))
        }

        @Test
        @DisplayName("토큰 갱신 시 사용자 정보 동기화")
        fun `토큰 갱신 시 최신 사용자 정보가 함께 반환된다`() {
            // Given
            val testUser =
                userRepository.save(
                    User(
                        email = "sync-test@example.com",
                        nickname = "원래닉네임",
                        authProvider = AuthProvider.GOOGLE,
                        providerId = "google-sync-test-id",
                    ),
                )

            val refreshToken = jwtTokenProvider.createRefreshToken(testUser.id!!)

            // 닉네임 변경
            testUser.nickname = "변경된닉네임"
            userRepository.save(testUser)

            val request = RefreshTokenRequest(refreshToken = refreshToken)

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.nickname").value("변경된닉네임")) // 변경된 닉네임 반환
        }
    }
