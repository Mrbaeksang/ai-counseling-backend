package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.character.entity.Character
import com.aicounseling.app.domain.character.repository.CharacterRepository
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.domain.session.report.repository.MessageReportRepository
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.github.cdimascio.dotenv.dotenv
import io.mockk.every
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional

/**
 * ChatSessionController 테스트 기본 클래스
 * 공통 설정과 헬퍼 메서드 제공
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ChatSessionControllerBaseTest(
    protected val mockMvc: MockMvc,
    protected val objectMapper: ObjectMapper,
    protected val jwtTokenProvider: JwtTokenProvider,
    protected val userRepository: UserRepository,
    protected val characterRepository: CharacterRepository,
    protected val sessionRepository: ChatSessionRepository,
    protected val messageRepository: MessageRepository,
    protected val messageReportRepository: MessageReportRepository,
) {
    @MockkBean(relaxed = true)
    protected lateinit var chatClient: ChatClient

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
                    ?: "test-api-key" // CI에서는 실제 API를 호출하지 않도록 더미 키 사용

            registry.add("spring.ai.openai.api-key") { apiKey }
            registry.add("jwt.secret") {
                System.getenv("JWT_SECRET")
                    ?: dotenv["JWT_SECRET"]
                    ?: "test-jwt-secret-key-for-jwt-auth-512-bits-long-2025-with-extra-characters-for-security"
            }
        }
    }

    protected lateinit var testUser: User
    protected lateinit var testCharacter: Character
    protected lateinit var authToken: String

    @BeforeEach
    fun setupTestData() {
        every { chatClient.prompt() } throws IllegalStateException("Mock ChatClient is not configured for this test.")

        // 테스트 사용자 생성
        testUser =
            userRepository.save(
                User(
                    email = "test@example.com",
                    nickname = "테스트유저",
                    authProvider = AuthProvider.GOOGLE,
                    providerId = "google-test-id",
                ),
            )

        // 테스트 캐릭터 생성
        testCharacter =
            characterRepository.save(
                Character(
                    name = "아리스토텔레스",
                    title = "고대 그리스의 철학자",
                    description = "실용적 윤리학과 행복론의 대가",
                    basePrompt = "당신은 아리스토텔레스입니다.",
                    avatarUrl = "https://example.com/aristotle.jpg",
                ),
            )

        // JWT 토큰 생성
        authToken = jwtTokenProvider.createToken(testUser.id, testUser.email)
    }

    @AfterEach
    fun cleanupTestData() {
        messageReportRepository.deleteAll()
        messageRepository.deleteAll()
        sessionRepository.deleteAll()
        characterRepository.deleteAll()
        userRepository.deleteAll()
    }
}
