package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.session.entity.ChatSession
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * PATCH /api/sessions/{id}/bookmark - 세션 북마크 토글 API 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PATCH /api/sessions/{id}/bookmark - 세션 북마크 토글")
class ToggleBookmarkApiTest
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
        @DisplayName("인증된 사용자가 세션을 북마크할 수 있다")
        fun `should bookmark session for authenticated user`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "북마크 테스트 세션",
                        isBookmarked = false,
                    ),
                )

            // When & Then
            mockMvc.perform(
                patch("/api/sessions/${session.id}/bookmark")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("북마크 추가 성공"))
                .andExpect(jsonPath("$.data.sessionId").value(session.id))
                .andExpect(jsonPath("$.data.isBookmarked").value(true))

            // DB 확인
            val updatedSession = sessionRepository.findById(session.id).orElseThrow()
            assert(updatedSession.isBookmarked)
        }

        @Test
        @DisplayName("이미 북마크된 세션의 북마크를 해제할 수 있다")
        fun `should remove bookmark from bookmarked session`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "북마크 해제 테스트 세션",
                        isBookmarked = true,
                    ),
                )

            // When & Then
            mockMvc.perform(
                patch("/api/sessions/${session.id}/bookmark")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("북마크 제거 성공"))
                .andExpect(jsonPath("$.data.sessionId").value(session.id))
                .andExpect(jsonPath("$.data.isBookmarked").value(false))

            // DB 확인
            val updatedSession = sessionRepository.findById(session.id).orElseThrow()
            assert(!updatedSession.isBookmarked)
        }

        @Test
        @DisplayName("다른 사용자의 세션은 북마크할 수 없다")
        fun `should return 500 when bookmarking other user session`() {
            // Given
            val otherUser =
                userRepository.save(
                    com.aicounseling.app.domain.user.entity.User(
                        email = "other@example.com",
                        nickname = "다른유저",
                        authProvider = com.aicounseling.app.global.security.AuthProvider.GOOGLE,
                        providerId = "google-other-id",
                    ),
                )

            val otherSession =
                sessionRepository.save(
                    ChatSession(
                        userId = otherUser.id,
                        counselorId = testCounselor.id,
                        title = "다른 사용자의 세션",
                        isBookmarked = false,
                    ),
                )

            // When & Then
            mockMvc.perform(
                patch("/api/sessions/${otherSession.id}/bookmark")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(jsonPath("$.resultCode").value("F-500"))
                .andExpect(jsonPath("$.msg").exists())

            // DB 확인 - 북마크되지 않아야 함
            val unchangedSession = sessionRepository.findById(otherSession.id).orElseThrow()
            assert(!unchangedSession.isBookmarked)
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 에러를 반환한다")
        fun `should return 401 for unauthenticated request`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "북마크 테스트 세션",
                        isBookmarked = false,
                    ),
                )

            // When & Then
            mockMvc.perform(
                patch("/api/sessions/${session.id}/bookmark"),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다"))
        }
    }
