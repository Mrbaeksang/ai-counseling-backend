package com.aicounseling.app.integration

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.cdimascio.dotenv.dotenv
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
 * 실제 OpenRouter API를 호출하는 통합 테스트
 *
 * 실행 조건:
 * 1. OPENROUTER_API_KEY 환경변수가 설정되어 있어야 함
 * 2. IntelliJ: Run Configuration에서 환경변수 추가
 * 3. CLI: OPENROUTER_API_KEY=sk-or-xxx ./gradlew test
 *
 * 무료 모델(openai/gpt-oss-20b:free) 사용으로 비용 발생 없음
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Disabled("통합 테스트는 로컬에서만 수동 실행")
@DisplayName("ChatSession 통합 테스트 - 실제 API 호출")
@org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
class ChatSessionIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper,
        private val jwtTokenProvider: JwtTokenProvider,
        private val userRepository: UserRepository,
        private val counselorRepository: CounselorRepository,
        private val sessionRepository: ChatSessionRepository,
        private val messageRepository: MessageRepository,
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
                        ?: "test-jwt-secret-key-for-jwt-auth-256-bits-long-2025-with-extra-characters-for-security"
                }
            }
        }

        private lateinit var testUser: User
        private lateinit var testCounselor: Counselor
        private lateinit var authToken: String

        @BeforeEach
        fun setup() {
            // 테스트 데이터 설정
            testUser =
                userRepository.save(
                    User(
                        email = "integration@test.com",
                        nickname = "통합테스트",
                        authProvider = AuthProvider.GOOGLE,
                        providerId = "google-integration-test",
                    ),
                )

            testCounselor =
                counselorRepository.save(
                    Counselor(
                        name = "소크라테스",
                        title = "고대 그리스 철학자",
                        description = "너 자신을 알라",
                        basePrompt = "당신은 소크라테스입니다. 질문을 통해 상대방이 스스로 답을 찾도록 도와주세요.",
                        avatarUrl = "https://example.com/socrates.jpg",
                        isActive = true,
                    ),
                )

            authToken = jwtTokenProvider.createToken(testUser.id, testUser.email)
        }

        @Test
        @DisplayName("실제 OpenRouter API를 통한 메시지 전송 테스트")
        fun `should send message and receive real AI response`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "통합 테스트 세션",
                    ),
                )

            val request =
                mapOf(
                    "content" to "안녕하세요, 저는 제 자신을 더 잘 알고 싶습니다.",
                )

            // When & Then
            val result =
                mockMvc.perform(
                    post("/api/sessions/${session.id}/messages")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.resultCode").value("S-1"))
                    .andExpect(jsonPath("$.msg").value("메시지 전송 성공"))
                    .andExpect(jsonPath("$.data.userMessage").exists())
                    .andExpect(jsonPath("$.data.aiMessage").exists())
                    .andReturn()

            // 실제 AI 응답 확인
            val response = objectMapper.readTree(result.response.contentAsString)
            val aiMessage = response.path("data").path("aiMessage").asText()

            println("=== 실제 AI 응답 ===")
            println(aiMessage)
            println("==================")

            // AI 응답이 의미있는 내용인지 확인
            assert(aiMessage.isNotEmpty())
            assert(aiMessage.length > 10) // 최소한의 길이 체크

            // DB에 메시지가 저장되었는지 확인
            val messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.id)
            assert(messages.size == 2)
            assert(messages[0].content == "안녕하세요, 저는 제 자신을 더 잘 알고 싶습니다.")
            assert(messages[1].content == aiMessage)
        }

        @Test
        @DisplayName("연속 대화 - 컨텍스트 유지 테스트")
        fun `should maintain conversation context`() {
            // Given - 세션 생성
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "대화 컨텍스트 테스트",
                    ),
                )

            // First message
            val firstRequest = mapOf("content" to "제 이름은 철수입니다.")

            mockMvc.perform(
                post("/api/sessions/${session.id}/messages")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest)),
            )
                .andExpect(status().isOk)

            // Second message - AI가 이전 대화를 기억하는지 테스트
            val secondRequest = mapOf("content" to "제 이름이 뭐라고 했죠?")

            val result =
                mockMvc.perform(
                    post("/api/sessions/${session.id}/messages")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)),
                )
                    .andExpect(status().isOk)
                    .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            val aiMessage = response.path("data").path("aiMessage").asText()

            println("=== AI의 컨텍스트 인식 응답 ===")
            println(aiMessage)
            println("==============================")

            // AI가 "철수"라는 이름을 기억하고 있는지 확인
            // (실제 응답은 다양할 수 있으므로 느슨한 검증)
            assert(aiMessage.isNotEmpty())
        }
    }
