package com.aicounseling.app.global.auth.controller

import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.auth.dto.OAuthLoginRequest
import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import org.junit.jupiter.api.Assertions.assertEquals
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
import reactor.core.publisher.Mono

/**
 * Naver OAuth 로그인 API 테스트
 * POST /api/auth/naver
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Naver OAuth 로그인 API 테스트")
class NaverLoginApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: ObjectMapper,
        jwtTokenProvider: JwtTokenProvider,
        userRepository: UserRepository,
    ) : AuthControllerBaseTest(mockMvc, objectMapper, jwtTokenProvider, userRepository) {
        @Test
        @DisplayName("신규 사용자 Naver 로그인 성공")
        fun `신규 사용자가 Naver OAuth로 로그인하면 새 계정이 생성되고 토큰이 발급된다`() {
            // Given
            val request = OAuthLoginRequest(token = "valid-naver-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("네이버 로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.email").value(naverUserInfo.email))
                .andExpect(jsonPath("$.data.nickname").value(naverUserInfo.name))

            // 사용자가 DB에 생성되었는지 확인
            val createdUser =
                userRepository.findByProviderIdAndAuthProvider(
                    naverUserInfo.providerId,
                    AuthProvider.NAVER,
                )
            assertNotNull(createdUser)
            assertEquals(AuthProvider.NAVER, createdUser?.authProvider)
            assertEquals(naverUserInfo.providerId, createdUser?.providerId)
            assertEquals(naverUserInfo.picture, createdUser?.profileImageUrl)
        }

        @Test
        @DisplayName("기존 사용자 Naver 로그인 성공")
        fun `기존 사용자가 Naver OAuth로 로그인하면 기존 계정 정보와 새 토큰이 반환된다`() {
            // Given
            val existingNaverUser =
                userRepository.save(
                    com.aicounseling.app.domain.user.entity.User(
                        email = naverUserInfo.email,
                        nickname = "네이버기존닉네임",
                        authProvider = AuthProvider.NAVER,
                        providerId = naverUserInfo.providerId,
                        profileImageUrl = "old-naver-profile.jpg",
                    ),
                )

            val request = OAuthLoginRequest(token = "valid-naver-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("네이버 로그인 성공"))
                .andExpect(jsonPath("$.data.userId").value(existingNaverUser.id))
                .andExpect(jsonPath("$.data.email").value(existingNaverUser.email))
                .andExpect(jsonPath("$.data.nickname").value("네이버기존닉네임")) // 기존 닉네임 유지

            // 사용자 수가 증가하지 않았는지 확인
            assertEquals(2, userRepository.count()) // existingUser(BeforeEach) + existingNaverUser
        }

        @Test
        @DisplayName("유효하지 않은 Naver 토큰으로 로그인 실패")
        fun `유효하지 않은 Naver OAuth 토큰으로 로그인하면 401 에러가 반환된다`() {
            // Given
            coEvery { naverTokenVerifier.verifyToken(any()) } returns
                Mono.error(IllegalStateException("Invalid naver token"))

            val request = OAuthLoginRequest(token = "invalid-naver-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        @DisplayName("Naver API 응답 에러 처리")
        fun `Naver API가 에러 응답을 반환하면 401 에러가 반환된다`() {
            // Given
            coEvery { naverTokenVerifier.verifyToken(any()) } returns
                Mono.error(IllegalStateException("resultcode: 024, message: Authentication failed"))

            val request = OAuthLoginRequest(token = "invalid-naver-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-401"))
        }

        @Test
        @DisplayName("Naver 계정 연령대 제한 테스트")
        fun `Naver 계정의 연령대 정보는 저장되지 않지만 로그인은 정상 처리된다`() {
            // Given - Naver는 age, age_range 등 추가 정보를 제공하지만 우리는 사용하지 않음
            val request = OAuthLoginRequest(token = "valid-naver-oauth-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))

            // 연령 정보는 저장되지 않음을 확인
            val createdUser =
                userRepository.findByProviderIdAndAuthProvider(
                    naverUserInfo.providerId,
                    AuthProvider.NAVER,
                )
            assertNotNull(createdUser)
            // User 엔티티에는 age 필드가 없음
        }

        @Test
        @DisplayName("세 가지 OAuth 제공자로 동일 이메일 가입 테스트")
        fun `동일한 이메일이라도 다른 OAuth 제공자면 각각 별도 계정으로 생성된다`() {
            // Given - Google과 Kakao로 이미 가입
            val commonEmail = "common@example.com"

            // Google 계정 생성
            userRepository.save(
                com.aicounseling.app.domain.user.entity.User(
                    email = commonEmail,
                    nickname = "구글계정",
                    authProvider = AuthProvider.GOOGLE,
                    providerId = "google-common-id",
                ),
            )

            // Kakao 계정 생성
            userRepository.save(
                com.aicounseling.app.domain.user.entity.User(
                    email = commonEmail,
                    nickname = "카카오계정",
                    authProvider = AuthProvider.KAKAO,
                    providerId = "kakao-common-id",
                ),
            )

            // Naver로 같은 이메일 사용
            val naverCommonUserInfo =
                OAuthUserInfo(
                    providerId = "naver-common-id",
                    email = commonEmail,
                    name = "네이버계정",
                    provider = "NAVER",
                    picture = null,
                )
            coEvery { naverTokenVerifier.verifyToken(any()) } returns Mono.just(naverCommonUserInfo)

            val request = OAuthLoginRequest(token = "valid-naver-common-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))

            // 세 개의 별도 계정이 존재하는지 확인
            val allUsers = userRepository.findAll()
            val usersWithCommonEmail = allUsers.filter { it.email == commonEmail }
            assertEquals(3, usersWithCommonEmail.size)

            // 각각 다른 OAuth 제공자인지 확인
            val providers = usersWithCommonEmail.map { it.authProvider }.toSet()
            assertTrue(providers.contains(AuthProvider.GOOGLE))
            assertTrue(providers.contains(AuthProvider.KAKAO))
            assertTrue(providers.contains(AuthProvider.NAVER))
        }

        @Test
        @DisplayName("Naver 프로필 동기화 테스트")
        fun `기존 Naver 사용자의 프로필 이미지가 변경되어도 기존 정보를 유지한다`() {
            // Given - 기존 Naver 사용자
            val existingNaverUser =
                userRepository.save(
                    com.aicounseling.app.domain.user.entity.User(
                        email = naverUserInfo.email,
                        nickname = "기존닉네임",
                        authProvider = AuthProvider.NAVER,
                        providerId = naverUserInfo.providerId,
                        profileImageUrl = "https://old-profile.jpg",
                    ),
                )

            // OAuth에서는 새 프로필 이미지가 왔지만
            val updatedNaverUserInfo =
                OAuthUserInfo(
                    providerId = naverUserInfo.providerId,
                    email = naverUserInfo.email,
                    name = "새닉네임",
                    provider = "NAVER",
                    // 새 프로필
                    picture = "https://new-profile.jpg",
                )
            coEvery { naverTokenVerifier.verifyToken(any()) } returns Mono.just(updatedNaverUserInfo)

            val request = OAuthLoginRequest(token = "valid-naver-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.nickname").value("기존닉네임")) // 기존 닉네임 유지

            // 프로필 이미지도 기존 것 유지
            val updatedUser = userRepository.findById(existingNaverUser.id).get()
            assertEquals("https://old-profile.jpg", updatedUser.profileImageUrl)
        }

        @Test
        @DisplayName("Naver 토큰 만료 처리")
        fun `만료된 Naver 토큰으로 로그인 시도하면 401 에러가 반환된다`() {
            // Given
            coEvery { naverTokenVerifier.verifyToken(any()) } returns
                Mono.error(IllegalStateException("token expired"))

            val request = OAuthLoginRequest(token = "expired-naver-token")

            // When & Then
            mockMvc
                .perform(
                    post("/api/auth/login/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk) // ResponseAspect가 200으로 변환
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").exists())
        }
    }
