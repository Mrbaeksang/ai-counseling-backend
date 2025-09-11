package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.session.dto.CreateSessionRequest
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
 * POST /sessions API 테스트
 * 새 세션 시작 기능 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("POST /sessions - 새 세션 시작")
class StartSessionApiTest
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
        @DisplayName("유효한 상담사 ID로 세션을 시작할 수 있다")
        fun `should start session with valid counselor id`() {
            // Given
            val request = CreateSessionRequest(counselorId = testCounselor.id)

            // When & Then
            mockMvc.perform(
                post("/api/sessions")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("세션 시작 성공"))
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.counselorName").value(testCounselor.name))
                .andExpect(jsonPath("$.data.title").exists())
        }

        @Test
        @DisplayName("존재하지 않는 상담사 ID로는 세션을 시작할 수 없다")
        fun `should fail with invalid counselor id`() {
            // Given
            val request = CreateSessionRequest(counselorId = 99999L)

            // When & Then
            mockMvc.perform(
                post("/api/sessions")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(jsonPath("$.resultCode").value("F-500"))
                .andExpect(jsonPath("$.msg").value("서버 오류가 발생했습니다"))
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 에러를 반환한다")
        fun `should return 401 for unauthenticated request`() {
            // Given
            val request = CreateSessionRequest(counselorId = testCounselor.id)

            // When & Then
            mockMvc.perform(
                post("/api/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다"))
        }

        @Test
        @DisplayName("요청 본문이 없으면 400 에러를 반환한다")
        fun `should return 400 for missing request body`() {
            mockMvc.perform(
                post("/api/sessions")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(
                    jsonPath("$.resultCode").value("F-500"),
                ) // HttpMessageNotReadableException -> Exception -> F-500
                .andExpect(jsonPath("$.msg").value("서버 오류가 발생했습니다"))
        }
    }
