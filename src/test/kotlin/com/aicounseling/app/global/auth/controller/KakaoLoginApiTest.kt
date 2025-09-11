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
import org.junit.jupiter.api.Assertions.assertNull
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
import reactor.core.publisher.Mono

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KakaoLoginApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: ObjectMapper,
        jwtTokenProvider: JwtTokenProvider,
        userRepository: UserRepository,
    ) : AuthControllerBaseTest(mockMvc, objectMapper, jwtTokenProvider, userRepository) {
        @Test
        @DisplayName("신규 사용자 Kakao 로그인 성공")
        fun `신규 사용자가 Kakao OAuth로 로그인하면 새 계정이 생성되고 토큰이 발급된다`() {
            // Given
            val request = OAuthLoginRequest(token = "valid-kakao-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("카카오 로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.email").value(kakaoUserInfo.email))
                .andExpect(jsonPath("$.data.nickname").value(kakaoUserInfo.name))

            // 사용자가 DB에 생성되었는지 확인
            val createdUser =
                userRepository.findByProviderIdAndAuthProvider(
                    kakaoUserInfo.providerId,
                    AuthProvider.KAKAO,
                )
            assertNotNull(createdUser)
            assertEquals(AuthProvider.KAKAO, createdUser?.authProvider)
            assertEquals(kakaoUserInfo.providerId, createdUser?.providerId)
            assertEquals(kakaoUserInfo.picture, createdUser?.profileImageUrl)
        }

        @Test
        @DisplayName("기존 사용자 Kakao 로그인 성공")
        fun `기존 사용자가 Kakao OAuth로 로그인하면 기존 계정 정보와 새 토큰이 반환된다`() {
            // Given
            val existingKakaoUser =
                userRepository.save(
                    com.aicounseling.app.domain.user.entity.User(
                        email = kakaoUserInfo.email,
                        nickname = "카카오닉네임",
                        authProvider = AuthProvider.KAKAO,
                        providerId = kakaoUserInfo.providerId,
                        profileImageUrl = "old-kakao-profile.jpg",
                    ),
                )

            val request = OAuthLoginRequest(token = "valid-kakao-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("카카오 로그인 성공"))
                .andExpect(jsonPath("$.data.userId").value(existingKakaoUser.id))
                .andExpect(jsonPath("$.data.email").value(existingKakaoUser.email))
                .andExpect(jsonPath("$.data.nickname").value("카카오닉네임")) // 기존 닉네임 유지

            // 사용자 수가 증가하지 않았는지 확인
            assertEquals(2, userRepository.count()) // existingUser(BeforeEach) + existingKakaoUser
        }

        @Test
        @DisplayName("유효하지 않은 Kakao 토큰으로 로그인 실패")
        fun `유효하지 않은 Kakao OAuth 토큰으로 로그인하면 401 에러가 반환된다`() {
            // Given
            coEvery { kakaoTokenVerifier.verifyToken(any()) } returns
                Mono.error(IllegalStateException("Invalid kakao token"))

            val request = OAuthLoginRequest(token = "invalid-kakao-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        @DisplayName("Kakao 계정에 이메일이 없는 경우 처리")
        fun `Kakao 계정에 이메일이 없으면 providerId@kakao_local 형식으로 생성된다`() {
            // Given
            val noEmailKakaoUserInfo =
                OAuthUserInfo(
                    providerId = "kakao-no-email-id-999",
                    // Kakao는 이메일이 선택사항, 빈 문자열로 처리
                    email = "",
                    name = "이메일없는유저",
                    provider = "KAKAO",
                    picture = "https://example.com/no-email-profile.jpg",
                )
            coEvery { kakaoTokenVerifier.verifyToken(any()) } returns Mono.just(noEmailKakaoUserInfo)

            val request = OAuthLoginRequest(token = "valid-kakao-token-no-email")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("카카오 로그인 성공"))
                .andExpect(jsonPath("$.data.email").value("kakao-no-email-id-999@kakao.local"))

            // DB 확인
            val createdUser =
                userRepository.findByProviderIdAndAuthProvider(
                    noEmailKakaoUserInfo.providerId,
                    AuthProvider.KAKAO,
                )
            assertNotNull(createdUser)
            assertEquals("kakao-no-email-id-999@kakao.local", createdUser?.email)
        }

        @Test
        @DisplayName("Kakao 프로필 이미지 URL 처리")
        fun `Kakao 프로필 이미지 URL이 null인 경우에도 정상 처리된다`() {
            // Given
            val noProfileKakaoUserInfo =
                OAuthUserInfo(
                    providerId = "kakao-no-profile-id",
                    email = "noprofile@kakao.com",
                    name = "프로필없음",
                    provider = "KAKAO",
                    // 프로필 이미지 없음
                    picture = null,
                )
            coEvery { kakaoTokenVerifier.verifyToken(any()) } returns Mono.just(noProfileKakaoUserInfo)

            val request = OAuthLoginRequest(token = "valid-kakao-token-no-profile")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))

            // DB 확인
            val createdUser =
                userRepository.findByProviderIdAndAuthProvider(
                    noProfileKakaoUserInfo.providerId,
                    AuthProvider.KAKAO,
                )
            assertNotNull(createdUser)
            assertNull(createdUser?.profileImageUrl)
        }

        @Test
        @DisplayName("다른 OAuth 제공자와 동일한 Kakao ID를 가진 경우")
        fun `providerId가 같더라도 authProvider가 다르면 별도 계정으로 처리된다`() {
            // Given - Google로 이미 가입한 사용자 (같은 providerId)
            val googleUser =
                userRepository.save(
                    com.aicounseling.app.domain.user.entity.User(
                        email = "google@example.com",
                        nickname = "구글유저",
                        authProvider = AuthProvider.GOOGLE,
                        // 우연히 같은 ID (실제로는 거의 불가능)
                        providerId = kakaoUserInfo.providerId,
                        profileImageUrl = null,
                    ),
                )

            val request = OAuthLoginRequest(token = "valid-kakao-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))

            // 새로운 Kakao 사용자가 생성되었는지 확인
            val kakaoUser =
                userRepository.findByProviderIdAndAuthProvider(
                    kakaoUserInfo.providerId,
                    AuthProvider.KAKAO,
                )
            assertNotNull(kakaoUser)
            assertNotEquals(googleUser.id, kakaoUser?.id) // 다른 사용자
            assertEquals(AuthProvider.KAKAO, kakaoUser?.authProvider)
        }

        @Test
        @DisplayName("Kakao 닉네임이 너무 긴 경우 처리")
        fun `Kakao 닉네임이 20자를 초과하면 20자로 잘라서 저장된다`() {
            // Given
            val longNameKakaoUserInfo =
                OAuthUserInfo(
                    providerId = "kakao-long-name-id",
                    email = "longname@kakao.com",
                    // 23자
                    name = "매우매우매우매우매우매우매우긴닉네임이예요정말로",
                    provider = "KAKAO",
                    picture = null,
                )
            coEvery { kakaoTokenVerifier.verifyToken(any()) } returns Mono.just(longNameKakaoUserInfo)

            val request = OAuthLoginRequest(token = "valid-kakao-token-long-name")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.nickname").value("매우매우매우매우매우매우매우긴닉네임이예")) // 20자로 잘림

            // DB 확인
            val createdUser =
                userRepository.findByProviderIdAndAuthProvider(
                    longNameKakaoUserInfo.providerId,
                    AuthProvider.KAKAO,
                )
            assertNotNull(createdUser)
            assertEquals(20, createdUser?.nickname?.length)
        }
    }
