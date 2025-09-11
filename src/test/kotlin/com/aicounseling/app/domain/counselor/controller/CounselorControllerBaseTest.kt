package com.aicounseling.app.domain.counselor.controller

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.counselor.entity.FavoriteCounselor
import com.aicounseling.app.domain.counselor.repository.CounselorRatingRepository
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.counselor.repository.FavoriteCounselorRepository
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.cdimascio.dotenv.dotenv
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * CounselorController 테스트 기본 클래스
 * 공통 설정과 헬퍼 메서드 제공
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class CounselorControllerBaseTest(
    protected val mockMvc: MockMvc,
    protected val objectMapper: ObjectMapper,
    protected val jwtTokenProvider: JwtTokenProvider,
    protected val userRepository: UserRepository,
    protected val counselorRepository: CounselorRepository,
    protected val counselorRatingRepository: CounselorRatingRepository,
    protected val favoriteCounselorRepository: FavoriteCounselorRepository,
    protected val sessionRepository: ChatSessionRepository,
) {
    companion object {
        private val dotenv =
            dotenv {
                ignoreIfMissing = true
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
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

    protected lateinit var testUser: User
    protected lateinit var testUser2: User
    protected lateinit var testCounselor1: Counselor
    protected lateinit var testCounselor2: Counselor
    protected lateinit var testCounselor3: Counselor
    protected lateinit var authToken: String
    protected lateinit var authToken2: String

    @BeforeEach
    fun setupTestData() {
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

        testUser2 =
            userRepository.save(
                User(
                    email = "test2@example.com",
                    nickname = "테스트유저2",
                    authProvider = AuthProvider.KAKAO,
                    providerId = "kakao-test-id",
                ),
            )

        // 테스트 상담사들 생성 (생성 시간 차이를 두어 순서 보장)
        testCounselor1 =
            counselorRepository.save(
                Counselor(
                    name = "소크라테스",
                    title = "고대 그리스의 철학자",
                    description = "대화법과 산파술의 창시자",
                    basePrompt = "당신은 소크라테스입니다.",
                    avatarUrl = "https://example.com/socrates.jpg",
                    isActive = true,
                ),
            )

        // 생성 시간 차이를 보장하기 위해 flush
        counselorRepository.flush()
        Thread.sleep(10)

        testCounselor2 =
            counselorRepository.save(
                Counselor(
                    name = "공자",
                    title = "동양 철학의 아버지",
                    description = "인仁과 예禮를 강조한 사상가",
                    basePrompt = "당신은 공자입니다.",
                    avatarUrl = "https://example.com/confucius.jpg",
                    isActive = true,
                ),
            )

        testCounselor3 =
            counselorRepository.save(
                Counselor(
                    name = "니체",
                    title = "실존주의 철학자",
                    description = "초인 사상과 영원회귀의 철학자",
                    basePrompt = "당신은 니체입니다.",
                    avatarUrl = "https://example.com/nietzsche.jpg",
                    // 비활성 상담사
                    isActive = false,
                ),
            )

        // JWT 토큰 생성
        authToken = jwtTokenProvider.createToken(testUser.id, testUser.email)
        authToken2 = jwtTokenProvider.createToken(testUser2.id, testUser2.email)
    }

    @AfterEach
    fun cleanupTestData() {
        counselorRatingRepository.deleteAll()
        favoriteCounselorRepository.deleteAll()
        sessionRepository.deleteAll()
        counselorRepository.deleteAll()
        userRepository.deleteAll()
    }

    /**
     * 테스트용 세션과 평가 데이터 생성
     */
    protected fun createTestSessionWithRating(
        user: User,
        counselor: Counselor,
        rating: Int,
    ): ChatSession {
        val session =
            sessionRepository.save(
                ChatSession(
                    userId = user.id,
                    counselorId = counselor.id,
                    title = "테스트 세션",
                    closedAt = LocalDateTime.now(),
                ),
            )

        counselorRatingRepository.save(
            CounselorRating(
                user = user,
                counselor = counselor,
                session = session,
                rating = rating,
                review = "좋았습니다",
            ),
        )

        return session
    }

    /**
     * 테스트용 즐겨찾기 생성
     */
    protected fun createFavoriteCounselor(
        user: User,
        counselor: Counselor,
    ): FavoriteCounselor {
        return favoriteCounselorRepository.save(
            FavoriteCounselor(
                user = user,
                counselor = counselor,
            ),
        )
    }
}
