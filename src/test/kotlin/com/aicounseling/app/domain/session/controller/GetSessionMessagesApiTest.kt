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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * GET /api/sessions/{id}/messages - 세션 메시지 목록 조회 API 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("GET /api/sessions/{id}/messages - 메시지 목록 조회")
class GetSessionMessagesApiTest
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
        @DisplayName("인증된 사용자가 세션의 메시지 목록을 조회할 수 있다")
        fun `should return message list for authenticated user`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "철학 상담",
                    ),
                )

            // 메시지 생성
            val messages =
                listOf(
                    messageRepository.save(
                        Message(
                            session = session,
                            content = "안녕하세요, 상담을 받고 싶습니다.",
                            senderType = SenderType.USER,
                            phase = CounselingPhase.ENGAGEMENT,
                        ),
                    ),
                    messageRepository.save(
                        Message(
                            session = session,
                            content = "안녕하세요! 무엇을 도와드릴까요?",
                            senderType = SenderType.AI,
                            phase = CounselingPhase.ENGAGEMENT,
                        ),
                    ),
                    messageRepository.save(
                        Message(
                            session = session,
                            content = "최근에 삶의 의미에 대해 고민이 많습니다.",
                            senderType = SenderType.USER,
                            phase = CounselingPhase.EXPLORATION,
                        ),
                    ),
                    messageRepository.save(
                        Message(
                            session = session,
                            content = "삶의 의미에 대한 고민은 인간의 본질적인 질문입니다.",
                            senderType = SenderType.AI,
                            phase = CounselingPhase.EXPLORATION,
                        ),
                    ),
                )

            // When & Then
            mockMvc.perform(
                get("/api/sessions/${session.id}/messages")
                    .header("Authorization", "Bearer $authToken")
                    .param("page", "0")
                    .param("size", "10"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("메시지 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(4))
                // 첫 번째 메시지 검증
                .andExpect(jsonPath("$.data.content[0].content").value(messages[0].content))
                .andExpect(jsonPath("$.data.content[0].senderType").value("USER"))
                // 두 번째 메시지 검증
                .andExpect(jsonPath("$.data.content[1].content").value(messages[1].content))
                .andExpect(jsonPath("$.data.content[1].senderType").value("AI"))
                // 나머지 메시지 검증
                .andExpect(jsonPath("$.data.content[2].content").value(messages[2].content))
                .andExpect(jsonPath("$.data.content[3].content").value(messages[3].content))
        }

        @Test
        @DisplayName("페이징 파라미터가 정상 작동한다")
        fun `should support pagination for messages`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                    ),
                )

            // 15개의 메시지 생성 - 각 메시지마다 약간의 지연을 두어 생성 시간 차이 보장
            repeat(15) { index ->
                messageRepository.save(
                    Message(
                        session = session,
                        content = "메시지 ${index + 1}",
                        senderType = if (index % 2 == 0) SenderType.USER else SenderType.AI,
                        phase = CounselingPhase.ENGAGEMENT,
                    ),
                )
                // 생성 시간 차이를 보장하기 위해 flush
                messageRepository.flush()
                Thread.sleep(1) // 1ms 지연으로 타임스탬프 차이 보장
            }

            // When & Then - 첫 페이지 (10개)
            mockMvc.perform(
                get("/api/sessions/${session.id}/messages")
                    .header("Authorization", "Bearer $authToken")
                    .param("page", "0")
                    .param("size", "10"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.content.length()").value(10))
                .andExpect(jsonPath("$.data.content[0].content").value("메시지 1"))
                .andExpect(jsonPath("$.data.content[9].content").value("메시지 10"))

            // When & Then - 두 번째 페이지 (5개)
            mockMvc.perform(
                get("/api/sessions/${session.id}/messages")
                    .header("Authorization", "Bearer $authToken")
                    .param("page", "1")
                    .param("size", "10"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.content[0].content").value("메시지 11"))
                .andExpect(jsonPath("$.data.content[4].content").value("메시지 15"))
        }

        @Test
        @DisplayName("메시지가 없는 세션은 빈 배열을 반환한다")
        fun `should return empty array for session without messages`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                    ),
                )

            // When & Then
            mockMvc.perform(
                get("/api/sessions/${session.id}/messages")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("메시지 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(0))
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

            // When & Then
            mockMvc.perform(
                get("/api/sessions/${session.id}/messages"),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다"))
        }

        @Test
        @DisplayName("다른 사용자의 세션 메시지 조회 시 500 에러를 반환한다")
        fun `should return 500 when accessing other user session messages`() {
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

            messageRepository.save(
                Message(
                    session = otherSession,
                    content = "다른 사용자의 메시지",
                    senderType = SenderType.USER,
                    phase = CounselingPhase.ENGAGEMENT,
                ),
            )

            // When & Then
            mockMvc.perform(
                get("/api/sessions/${otherSession.id}/messages")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(
                    jsonPath("$.resultCode").value("F-500"),
                ) // IllegalArgumentException is caught as generic Exception
        }

        @Test
        @DisplayName("존재하지 않는 세션 ID로 조회 시 500 에러를 반환한다")
        fun `should return 500 for non-existent session`() {
            // Given
            val nonExistentSessionId = 99999L

            // When & Then
            mockMvc.perform(
                get("/api/sessions/$nonExistentSessionId/messages")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk) // ResponseAspect disabled in test profile
                .andExpect(
                    jsonPath("$.resultCode").value("F-500"),
                ) // NoSuchElementException is caught as generic Exception
        }

        @Test
        @DisplayName("기본 페이징 파라미터로 조회가 가능하다")
        fun `should use default paging parameters`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                    ),
                )

            repeat(25) { index ->
                messageRepository.save(
                    Message(
                        session = session,
                        content = "메시지 ${index + 1}",
                        senderType = SenderType.USER,
                        phase = CounselingPhase.ENGAGEMENT,
                    ),
                )
            }

            // When & Then - 파라미터 없이 요청 (기본값: page=0, size=20)
            mockMvc.perform(
                get("/api/sessions/${session.id}/messages")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.content.length()").value(20)) // 기본 size=20
        }
    }
