package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.session.entity.ChatSession
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * GET /sessions API 테스트
 * 세션 목록 조회 기능 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("GET /sessions - 세션 목록 조회")
class GetUserSessionsApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
        jwtTokenProvider: com.aicounseling.app.global.security.JwtTokenProvider,
        userRepository: com.aicounseling.app.domain.user.repository.UserRepository,
        counselorRepository: com.aicounseling.app.domain.counselor.repository.CounselorRepository,
        sessionRepository: com.aicounseling.app.domain.session.repository.ChatSessionRepository,
        messageRepository: com.aicounseling.app.domain.session.repository.MessageRepository,
    ) : ChatSessionControllerBaseTest(
            mockMvc,
            objectMapper,
            jwtTokenProvider,
            userRepository,
            counselorRepository,
            sessionRepository,
            messageRepository,
        ) {
        companion object {
            private val dotenv =
                io.github.cdimascio.dotenv.dotenv {
                    ignoreIfMissing = true
                }

            @JvmStatic
            @org.springframework.test.context.DynamicPropertySource
            fun properties(registry: org.springframework.test.context.DynamicPropertyRegistry) {
                val apiKey =
                    System.getenv("OPENROUTER_API_KEY")
                        ?: dotenv["OPENROUTER_API_KEY"]
                        ?: "test-api-key"

                registry.add("openrouter.api-key") { apiKey }
                registry.add("jwt.secret") {
                    System.getenv("JWT_SECRET")
                        ?: dotenv["JWT_SECRET"]
                        ?: "test-jwt-secret-key-for-jwt-auth-512-bits-long-2025-with-extra-characters-for-security"
                }
            }
        }

        @Test
        @DisplayName("인증된 사용자가 세션 목록을 조회할 수 있다")
        fun `should return session list for authenticated user`() {
            // Given: 테스트용 세션 3개 생성
            (1..3).forEach { i ->
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "세션 $i",
                        lastMessageAt = LocalDateTime.now().minusHours(i.toLong()),
                    ),
                )
            }

            // When & Then
            mockMvc.perform(
                get("/api/sessions")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("세션 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(3))
        }

        @Test
        @DisplayName("북마크된 세션만 필터링하여 조회할 수 있다")
        fun `should filter bookmarked sessions when requested`() {
            // Given: 북마크된 세션 2개, 일반 세션 1개
            sessionRepository.save(
                ChatSession(
                    userId = testUser.id,
                    counselorId = testCounselor.id,
                    title = "북마크 세션 1",
                    isBookmarked = true,
                ),
            )
            sessionRepository.save(
                ChatSession(
                    userId = testUser.id,
                    counselorId = testCounselor.id,
                    title = "북마크 세션 2",
                    isBookmarked = true,
                ),
            )
            sessionRepository.save(
                ChatSession(
                    userId = testUser.id,
                    counselorId = testCounselor.id,
                    title = "일반 세션",
                    isBookmarked = false,
                ),
            )

            // When & Then
            mockMvc.perform(
                get("/api/sessions")
                    .param("bookmarked", "true")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.msg").value("북마크된 세션 조회 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
        }

        @Test
        @DisplayName("페이지네이션이 정상 작동한다")
        fun `should support pagination`() {
            // Given: 25개 세션 생성
            repeat(25) {
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                    ),
                )
            }

            // When & Then: 첫 페이지 (20개)
            mockMvc.perform(
                get("/api/sessions")
                    .param("page", "0")
                    .param("size", "20")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content.length()").value(20))

            // When & Then: 두 번째 페이지 (5개)
            mockMvc.perform(
                get("/api/sessions")
                    .param("page", "1")
                    .param("size", "20")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content.length()").value(5))
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 에러를 반환한다")
        fun `should return 401 for unauthenticated request`() {
            mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다"))
        }
    }
