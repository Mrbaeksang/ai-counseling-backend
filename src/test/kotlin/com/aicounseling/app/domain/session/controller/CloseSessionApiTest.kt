package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.session.entity.ChatSession
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * DELETE /api/sessions/{id} - 세션 종료 API 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("DELETE /api/sessions/{id} - 세션 종료")
class CloseSessionApiTest
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
        @DisplayName("인증된 사용자가 자신의 세션을 종료할 수 있다")
        fun `should close session for authenticated user`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "종료할 세션",
                    ),
                )

            // When & Then
            mockMvc.perform(
                delete("/api/sessions/${session.id}")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("세션 종료 성공"))

            // DB 확인
            val closedSession = sessionRepository.findById(session.id).orElseThrow()
            assert(closedSession.closedAt != null)
        }

        @Test
        @DisplayName("이미 종료된 세션을 다시 종료하려고 하면 409 에러를 반환한다")
        fun `should return 409 when closing already closed session`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "이미 종료된 세션",
                        closedAt = LocalDateTime.now(),
                    ),
                )

            // When & Then
            mockMvc.perform(
                delete("/api/sessions/${session.id}")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(jsonPath("$.resultCode").value("F-409"))
                .andExpect(jsonPath("$.msg").exists()) // 실제 메시지는 check 실패 시 나옴
        }

        @Test
        @DisplayName("다른 사용자의 세션을 종료하려고 하면 500 에러를 반환한다")
        fun `should return 500 when closing other user session`() {
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
                    ),
                )

            // When & Then
            mockMvc.perform(
                delete("/api/sessions/${otherSession.id}")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(jsonPath("$.resultCode").value("F-500"))
        }
    }
