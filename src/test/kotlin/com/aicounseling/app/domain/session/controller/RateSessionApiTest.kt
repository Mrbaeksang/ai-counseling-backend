package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.counselor.dto.RateSessionRequest
import com.aicounseling.app.domain.counselor.repository.CounselorRatingRepository
import com.aicounseling.app.domain.session.entity.ChatSession
import org.junit.jupiter.api.AfterEach
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
import java.time.LocalDateTime

/**
 * POST /api/sessions/{id}/rate - 세션 평가 API 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("POST /api/sessions/{id}/rate - 세션 평가")
class RateSessionApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
        jwtTokenProvider: com.aicounseling.app.global.security.JwtTokenProvider,
        userRepository: com.aicounseling.app.domain.user.repository.UserRepository,
        counselorRepository: com.aicounseling.app.domain.counselor.repository.CounselorRepository,
        sessionRepository: com.aicounseling.app.domain.session.repository.ChatSessionRepository,
        messageRepository: com.aicounseling.app.domain.session.repository.MessageRepository,
        private val ratingRepository: CounselorRatingRepository,
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

        @AfterEach
        override fun cleanupTestData() {
            // CounselorRating 먼저 삭제 (세션 참조하므로)
            ratingRepository.deleteAll()
            // 그 다음 부모 클래스의 cleanup 호출
            super.cleanupTestData()
        }

        @Test
        @DisplayName("종료된 세션에 평점과 피드백을 등록할 수 있다")
        fun `should rate closed session with feedback`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "평가할 세션",
                        closedAt = LocalDateTime.now(),
                    ),
                )

            // 1~10 범위 (별 5개 = 10)
            val request =
                RateSessionRequest(
                    rating = 10,
                    feedback = "매우 유익한 상담이었습니다. 니체의 철학을 잘 설명해주셨어요.",
                )

            // When & Then
            mockMvc.perform(
                post("/api/sessions/${session.id}/rate")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("평가가 등록되었습니다"))
                .andExpect(jsonPath("$.data").value("평가 등록 성공"))

            // DB 확인
            val ratings = ratingRepository.findAll()
            assert(ratings.size == 1)
            val rating = ratings[0]
            assert(rating.rating == 10)
            assert(rating.review == "매우 유익한 상담이었습니다. 니체의 철학을 잘 설명해주셨어요.")
            assert(rating.session.id == session.id)
        }

        @Test
        @DisplayName("잘못된 평점 범위(1-10 외)로 요청하면 400 에러를 반환한다")
        fun `should return 400 for invalid rating range`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        closedAt = LocalDateTime.now(),
                    ),
                )

            // 범위 초과 (1~10)
            val request =
                mapOf(
                    "rating" to 11,
                    "feedback" to "테스트",
                )

            // When & Then
            mockMvc.perform(
                post("/api/sessions/${session.id}/rate")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                // ResponseAspect disabled in test profile
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-400"))
        }

        @Test
        @DisplayName("종료되지 않은 세션은 평가할 수 없다")
        fun `should return 409 when rating active session`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "진행중인 세션",
                        // closedAt = null (진행중)
                    ),
                )

            val request =
                RateSessionRequest(
                    rating = 8,
                    feedback = "좋아요",
                )

            // When & Then
            mockMvc.perform(
                post("/api/sessions/${session.id}/rate")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                // ResponseAspect disabled in test profile
                .andExpect(status().isOk)
                .andExpect(
                    // IllegalStateException -> F-409 in GlobalExceptionHandler
                    jsonPath("$.resultCode").value("F-409"),
                )
                // "진행 중인 세션은 평가할 수 없습니다"
                .andExpect(jsonPath("$.msg").exists())
        }

        @Test
        @DisplayName("이미 평가한 세션은 재평가할 수 없다")
        fun `should not allow re-rating already rated session`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "이미 평가한 세션",
                        closedAt = LocalDateTime.now(),
                    ),
                )

            // 첫 번째 평가 생성
            val firstRating =
                com.aicounseling.app.domain.counselor.entity.CounselorRating(
                    user = testUser,
                    counselor = testCounselor,
                    session = session,
                    // 별 3개
                    rating = 6,
                    review = "보통이었습니다",
                )
            ratingRepository.save(firstRating)

            // 별 5개로 변경 시도
            val request =
                RateSessionRequest(
                    rating = 10,
                    feedback = "다시 생각해보니 정말 좋았어요!",
                )

            // When & Then - 재평가 시도는 실패해야 함
            mockMvc.perform(
                post("/api/sessions/${session.id}/rate")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                // ResponseAspect disabled in test profile
                .andExpect(status().isOk)
                // 이미 평가된 세션
                .andExpect(jsonPath("$.resultCode").value("F-400"))
                .andExpect(jsonPath("$.msg").value("이미 평가가 완료된 세션입니다"))

            // DB 확인 - 기존 평가가 그대로 유지되어야 함
            val ratings = ratingRepository.findAll()
            assert(ratings.size == 1)
            assert(ratings[0].rating == 6)
            assert(ratings[0].review == "보통이었습니다")
        }
    }
