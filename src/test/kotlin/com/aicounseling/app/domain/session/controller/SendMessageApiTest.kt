package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.cdimascio.dotenv.dotenv
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * POST /api/sessions/{id}/messages - 메시지 전송 API 테스트
 * 실제 OpenRouter API를 호출하는 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("POST /api/sessions/{id}/messages - 메시지 전송")
@org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
class SendMessageApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: ObjectMapper,
        jwtTokenProvider: JwtTokenProvider,
        userRepository: UserRepository,
        counselorRepository: CounselorRepository,
        sessionRepository: ChatSessionRepository,
        messageRepository: MessageRepository,
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
                dotenv {
                    ignoreIfMissing = true
                }

            @JvmStatic
            @DynamicPropertySource
            fun properties(registry: DynamicPropertyRegistry) {
                // CI 환경에서는 환경변수, 로컬에서는 .env 파일 사용
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
        @DisplayName("인증된 사용자가 세션에 메시지를 전송하면 AI 응답을 받을 수 있다")
        fun `should send message and receive AI response`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "상담 세션",
                    ),
                )

            val userMessageContent = "안녕하세요, 상담을 받고 싶습니다."
            val request = mapOf("content" to userMessageContent)

            // When & Then - 실제 OpenRouter API 호출
            val result =
                mockMvc.perform(
                    post("/api/sessions/${session.id}/messages")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                    .andDo { result ->
                        // CI 환경에서 실패 원인 디버깅
                        println("=== CI 환경 테스트 디버깅 ===")
                        println("Response Status: ${result.response.status}")
                        println("Response Content: ${result.response.contentAsString}")
                        println("============================")
                    }
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.resultCode").value("S-1"))
                    .andExpect(jsonPath("$.msg").value("메시지 전송 성공"))
                    .andExpect(jsonPath("$.data.userMessage").value(userMessageContent))
                    .andExpect(jsonPath("$.data.aiMessage").exists()) // AI 응답이 있는지만 확인
                    .andReturn()

            // 실제 AI 응답 내용 출력 (확인용)
            val responseContent = result.response.contentAsString
            val responseJson = objectMapper.readTree(responseContent)
            val aiMessage = responseJson.path("data").path("aiMessage").asText()
            println("실제 AI 응답: $aiMessage")

            // DB 확인
            val messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.id)
            println("=== DB 메시지 확인 ===")
            println("메시지 개수: ${messages.size}")
            messages.forEachIndexed { index, msg ->
                println("메시지[$index]: ${msg.senderType} - ${msg.content}")
            }

            assert(messages.size == 2)
            assert(messages[0].content == userMessageContent)
            assert(messages[0].senderType == SenderType.USER)
            assert(messages[1].senderType == SenderType.AI)
            assert(messages[1].content.isNotEmpty()) { "AI 응답이 비어있습니다: '${messages[1].content}'" }
        }

        @Test
        @DisplayName("두 번째 메시지에도 세션 제목이 응답에 포함된다")
        fun `should include session title for non-first message`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "기존 제목",
                    ),
                )

            // 이전 메시지가 있는 상태
            messageRepository.save(
                Message(
                    session = session,
                    content = "이전 메시지",
                    senderType = SenderType.USER,
                    phase = CounselingPhase.ENGAGEMENT,
                ),
            )

            val request = mapOf("content" to "추가 메시지입니다")

            // When & Then - 실제 API 호출
            mockMvc.perform(
                post("/api/sessions/${session.id}/messages")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.sessionTitle").value("기존 제목")) // 세션 제목이 포함됨
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
                    ),
                )

            val request = mapOf("content" to "메시지")

            // When & Then
            mockMvc.perform(
                post("/api/sessions/${session.id}/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isUnauthorized) // Spring Security returns 401 directly
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다"))
        }

        @Test
        @DisplayName("다른 사용자의 세션에 메시지 전송 시 500 에러를 반환한다")
        fun `should return 500 when sending message to other user session`() {
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

            val request = mapOf("content" to "메시지")

            // When & Then
            mockMvc.perform(
                post("/api/sessions/${otherSession.id}/messages")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(
                    jsonPath("$.resultCode").value("F-500"),
                ) // IllegalArgumentException is caught as generic Exception
        }

        @Test
        @DisplayName("빈 메시지 전송 시 400 에러를 반환한다")
        fun `should return 400 for empty message`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                    ),
                )

            val request = mapOf("content" to "")

            // When & Then
            mockMvc.perform(
                post("/api/sessions/${session.id}/messages")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(jsonPath("$.resultCode").value("F-400"))
        }

        @Test
        @DisplayName("종료된 세션에 메시지 전송 시 409 에러를 반환한다")
        fun `should return 409 when sending message to closed session`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        closedAt = java.time.LocalDateTime.now(),
                    ),
                )

            val request = mapOf("content" to "메시지")

            // When & Then
            mockMvc.perform(
                post("/api/sessions/${session.id}/messages")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(jsonPath("$.resultCode").value("F-409")) // IllegalStateException returns 409
        }
    }
