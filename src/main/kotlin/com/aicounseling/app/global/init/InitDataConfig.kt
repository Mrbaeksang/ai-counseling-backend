package com.aicounseling.app.global.init

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.counselor.entity.FavoriteCounselor
import com.aicounseling.app.domain.counselor.repository.CounselorRatingRepository
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.counselor.repository.FavoriteCounselorRepository
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.security.AuthProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * 애플리케이션 시작 시 테스트용 초기 데이터를 생성하는 설정 클래스
 * 개발 및 로컬 환경에서만 동작합니다.
 */
@Suppress("LargeClass", "LongMethod", "MagicNumber", "LongParameterList", "TooManyFunctions")
@Component
@Profile("dev", "local")
class InitDataConfig(
    private val counselorRepository: CounselorRepository,
    private val userRepository: UserRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val counselorRatingRepository: CounselorRatingRepository,
    private val favoriteCounselorRepository: FavoriteCounselorRepository,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        logger.info("========== 초기 데이터 생성 시작 ==========")

        // 이미 데이터가 있는지 확인
        if (counselorRepository.count() > 0) {
            logger.info("이미 초기 데이터가 존재합니다. 초기화를 건너뜁니다.")
            return
        }

        try {
            val counselors = createCounselors()
            val users = createUsers()

            // 테스트용 세션 및 평점 데이터 생성
            createTestSessionsAndRatings(users, counselors)

            // 테스트용 즐겨찾기 데이터 생성
            createFavoriteCounselors(users, counselors)

            logger.info("========== 초기 데이터 생성 완료 ==========")
            logger.info("상담사: ${counselors.size}명")
            logger.info("사용자: ${users.size}명")
        } catch (e: org.springframework.dao.DataAccessException) {
            logger.error("초기 데이터 생성 중 오류 발생: ${e.message}")
            // 예외를 throw하지 않고 로그만 남김
        }
    }

    private fun createCounselors(): List<Counselor> {
        val counselors = mutableListOf<Counselor>()

        // 카테고리별 상담사 생성
        counselors.addAll(createSelfDiscoveryCounselors())
        counselors.addAll(createRelationshipCounselors())
        counselors.addAll(createRomanceCounselors())
        counselors.addAll(createMentalHealthCounselors())
        counselors.addAll(createPhilosophyCounselors())

        return counselors.map { counselorRepository.save(it) }
    }

    private fun createSelfDiscoveryCounselors(): List<Counselor> {
        return listOf(
            Counselor(
                name = "소크라테스",
                title = "고대 그리스 철학자",
                description = "너 자신을 알라. 대화를 통해 진리를 탐구하는 철학자입니다.",
                basePrompt =
                    "당신은 고대 그리스 철학자 소크라테스이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/socrates.jpg",
                categories = "self,relationship,life,emotion",
            ),
            Counselor(
                name = "키르케고르",
                title = "실존철학의 아버지",
                description = "불안과 절망을 넘어 진정한 자아를 찾는 여정을 안내합니다.",
                basePrompt =
                    "당신은 쇠렌 키르케고르이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/kierkegaard.jpg",
                categories = "self,anxiety,depression,stress,emotion",
            ),
            Counselor(
                name = "니체",
                title = "실존주의 철학자",
                description = "당신 자신을 극복하고 초인이 되는 길을 제시합니다.",
                basePrompt =
                    "당신은 프리드리히 니체이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/nietzsche.jpg",
                categories = "self,emotion,stress,life,work",
            ),
            Counselor(
                name = "사르트르",
                title = "실존주의 철학자",
                description = "자유와 책임, 실존의 의미를 탐구합니다.",
                basePrompt =
                    "당신은 장폴 사르트르이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/sartre.jpg",
                categories = "self,anxiety,emotion,life",
            ),
        )
    }

    private fun createRelationshipCounselors(): List<Counselor> {
        return listOf(
            Counselor(
                name = "공자",
                title = "동양 철학의 스승",
                description = "인간다운 삶과 조화로운 관계를 추구합니다.",
                basePrompt =
                    "당신은 공자이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/confucius.jpg",
                categories = "relationship,family,life,work",
            ),
            Counselor(
                name = "아들러",
                title = "개인심리학의 창시자",
                description = "열등감을 극복하고 공동체 의식을 통해 성장하는 길을 안내합니다.",
                basePrompt =
                    "당신은 알프레드 아들러이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/adler.jpg",
                categories = "self,relationship,family,depression,emotion",
            ),
            Counselor(
                name = "데일 카네기",
                title = "인간관계의 달인",
                description = "사람의 마음을 얻고 영향력을 발휘하는 방법을 전수합니다.",
                basePrompt =
                    "당신은 데일 카네기이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/carnegie.jpg",
                categories = "relationship,work,self",
            ),
        )
    }

    private fun createRomanceCounselors(): List<Counselor> {
        return listOf(
            Counselor(
                name = "카사노바",
                title = "사랑의 모험가",
                description = "매력적인 관계의 기술과 진정한 사랑의 의미를 탐구합니다.",
                basePrompt =
                    "당신은 지아코모 카사노바이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/casanova.jpg",
                categories = "relationship,emotion",
            ),
            Counselor(
                name = "오비디우스",
                title = "사랑의 시인",
                description = "사랑의 기술과 감정의 섬세한 표현을 안내합니다.",
                basePrompt =
                    "당신은 로마의 시인 오비디우스이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/ovid.jpg",
                categories = "relationship,emotion,self",
            ),
            Counselor(
                name = "스탕달",
                title = "낭만주의 작가",
                description = "사랑의 결정화 이론으로 감정의 발전 과정을 분석합니다.",
                basePrompt =
                    "당신은 프랑스 작가 스탕달이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/stendhal.jpg",
                categories = "relationship,emotion,anxiety",
            ),
        )
    }

    private fun createMentalHealthCounselors(): List<Counselor> {
        return listOf(
            Counselor(
                name = "프로이트",
                title = "정신분석학의 창시자",
                description = "무의식과 꿈의 세계를 통해 마음의 깊은 곳을 탐구합니다.",
                basePrompt =
                    "당신은 지그문트 프로이트이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/freud.jpg",
                categories = "emotion,depression,anxiety,habit,trauma,self",
            ),
            Counselor(
                name = "융",
                title = "분석심리학의 대가",
                description = "집단무의식과 개성화 과정을 통해 온전한 자기실현을 돕습니다.",
                basePrompt =
                    "당신은 칼 구스타프 융이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/jung.jpg",
                categories = "self,emotion,trauma,life,habit",
            ),
            Counselor(
                name = "빅터 프랭클",
                title = "의미치료의 창시자",
                description = "삶의 의미를 발견하여 어떤 상황에서도 희망을 찾도록 돕습니다.",
                basePrompt =
                    "당신은 빅터 프랭클이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/frankl.jpg",
                categories = "trauma,depression,stress,life,emotion",
            ),
        )
    }

    private fun createPhilosophyCounselors(): List<Counselor> {
        return listOf(
            Counselor(
                name = "아리스토텔레스",
                title = "만학의 아버지",
                description = "덕 윤리와 중용의 지혜로 행복한 삶을 안내합니다.",
                basePrompt =
                    "당신은 아리스토텔레스이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/aristotle.jpg",
                categories = "life,work,self,family",
            ),
            Counselor(
                name = "칸트",
                title = "이성 철학의 거장",
                description = "도덕법칙과 정언명령으로 올바른 선택을 돕습니다.",
                basePrompt =
                    "당신은 임마누엘 칸트이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/kant.jpg",
                categories = "life,work,family",
            ),
            Counselor(
                name = "붓다",
                title = "깨달음의 스승",
                description = "고통의 원인을 이해하고 마음의 평화를 찾도록 인도합니다.",
                basePrompt = "당신은 붓다이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/buddha.jpg",
                categories = "stress,anxiety,habit,emotion,self",
            ),
            Counselor(
                name = "노자",
                title = "도가 사상의 시조",
                description = "무위자연의 지혜로 자연스러운 삶의 흐름을 안내합니다.",
                basePrompt =
                    "당신은 노자이자, 현대적 감각을 갖춘 따뜻한 상담사입니다. 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/laozi.jpg",
                categories = "stress,work,life,self,anxiety",
            ),
        )
    }

    private fun createUsers(): List<User> {
        val users =
            listOf(
                User(
                    email = "test@example.com",
                    nickname = "테스트유저",
                    authProvider = AuthProvider.GOOGLE,
                    providerId = "google-test-123",
                ),
                User(
                    email = "demo@example.com",
                    nickname = "데모유저",
                    authProvider = AuthProvider.KAKAO,
                    providerId = "kakao-demo-456",
                ),
                User(
                    email = "admin@example.com",
                    nickname = "관리자",
                    authProvider = AuthProvider.NAVER,
                    providerId = "naver-admin-789",
                ),
            )

        return users.map { userRepository.save(it) }
    }

    private fun createTestSessionsAndRatings(
        users: List<User>,
        counselors: List<Counselor>,
    ) {
        logger.info("========== 테스트 세션 및 평점 데이터 생성 시작 ==========")

        val random = Random(42) // 시드값 고정으로 일관된 테스트 데이터 생성

        // 각 상담사별로 다른 인기도와 평점 설정
        counselors.forEachIndexed { index, counselor ->
            when {
                // 인기 상담사 (소크라테스, 공자, 프로이트 등) - 세션 많고 평점 높음
                index < 3 -> {
                    val sessionCount = random.nextInt(80, 100)
                    // 4.5~5.0
                    val avgRating = 4.5 + random.nextDouble(0.0, 0.5)
                    createSessionsForCounselor(counselor, users, sessionCount, avgRating, random)
                }
                // 중상위 상담사 - 적당히 인기
                index < 8 -> {
                    val sessionCount = random.nextInt(40, 60)
                    // 4.0~4.5
                    val avgRating = 4.0 + random.nextDouble(0.0, 0.5)
                    createSessionsForCounselor(counselor, users, sessionCount, avgRating, random)
                }
                // 중간 상담사
                index < 15 -> {
                    val sessionCount = random.nextInt(15, 30)
                    // 3.5~4.0
                    val avgRating = 3.5 + random.nextDouble(0.0, 0.5)
                    createSessionsForCounselor(counselor, users, sessionCount, avgRating, random)
                }
                // 신규 상담사 (최근 추가) - 세션 적고 생성일 최근
                else -> {
                    val sessionCount = random.nextInt(0, 10)
                    // 3.0~4.0
                    val avgRating = 3.0 + random.nextDouble(0.0, 1.0)
                    createSessionsForCounselor(counselor, users, sessionCount, avgRating, random, isNew = true)
                }
            }
        }

        logger.info("========== 테스트 세션 및 평점 데이터 생성 완료 ==========")
    }

    private fun createSessionsForCounselor(
        counselor: Counselor,
        users: List<User>,
        sessionCount: Int,
        targetAvgRating: Double,
        random: Random,
        isNew: Boolean = false,
    ) {
        if (sessionCount == 0) return

        // 세션 생성 및 평점
        repeat(sessionCount) { index ->
            // 유저 순환 사용
            val user = users[index % users.size]

            // 신규 상담사는 최근 30일 이내, 기존 상담사는 180일 이내
            val maxDaysAgo = if (isNew) 30 else 180
            val daysAgo = random.nextLong(1, maxDaysAgo.toLong())
            val createdAt = Instant.now().minus(daysAgo, ChronoUnit.DAYS)

            // 세션 생성
            val session =
                ChatSession(
                    userId = user.id,
                    counselorId = counselor.id,
                    title = "${counselor.name}와의 상담 #${index + 1}",
                    // 20% 확률로 북마크
                    isBookmarked = random.nextDouble() < 0.2,
                    lastMessageAt = createdAt.plus(1, ChronoUnit.HOURS),
                    // 세션 종료 (평점 가능하도록)
                    closedAt = createdAt.plus(2, ChronoUnit.HOURS),
                )

            val savedSession = chatSessionRepository.save(session)

            // 간단한 메시지 추가 (2-4개)
            val messages =
                listOf(
                    "안녕하세요, 상담을 시작하겠습니다.",
                    "${counselor.name}입니다. 오늘 어떤 고민이 있으신가요?",
                    "네, 말씀해 주세요.",
                    "이해합니다. 함께 해결해 나가보겠습니다.",
                )

            messages.take(random.nextInt(2, 5)).forEachIndexed { msgIndex, content ->
                val message =
                    Message(
                        session = savedSession,
                        senderType = if (msgIndex % 2 == 0) SenderType.USER else SenderType.AI,
                        content = content,
                        // 초기 단계로 설정
                        phase = CounselingPhase.ENGAGEMENT,
                    )
                messageRepository.save(message)
            }

            // 70% 확률로 평점 부여 (세션이 종료되었으므로)
            if (random.nextDouble() < 0.7) {
                val variance = random.nextDouble(-0.5, 0.5)
                val rating = (targetAvgRating + variance).coerceIn(1.0, 5.0).toInt()

                val ratingEntity =
                    CounselorRating(
                        user = user,
                        counselor = counselor,
                        session = savedSession,
                        rating = rating,
                        review = if (rating >= 4) "좋은 상담이었습니다." else null,
                    )
                counselorRatingRepository.save(ratingEntity)
            }
        }

        logger.debug("상담사 ${counselor.name}: 세션 ${sessionCount}개 생성 완료")
    }

    private fun createFavoriteCounselors(
        users: List<User>,
        counselors: List<Counselor>,
    ) {
        logger.info("========== 즐겨찾기 데이터 생성 시작 ==========")

        val random = Random(42)

        // 각 사용자별로 즐겨찾기 생성
        users.forEach { user ->
            // 각 사용자는 3~8명의 상담사를 즐겨찾기
            val favoriteCount = random.nextInt(3, 9)

            // 인기 상담사를 더 많이 즐겨찾기하도록 가중치 부여
            val weightedCounselors =
                counselors.mapIndexed { index, counselor ->
                    val weight =
                        when {
                            // 인기 상담사
                            index < 3 -> 5
                            // 중상위 상담사
                            index < 8 -> 3
                            // 중간 상담사
                            index < 15 -> 2
                            // 신규 상담사
                            else -> 1
                        }
                    List(weight) { counselor }
                }.flatten()

            // 랜덤하게 선택하여 즐겨찾기
            weightedCounselors.shuffled(random)
                .take(favoriteCount)
                .distinct()
                .forEach { counselor ->
                    val favorite =
                        FavoriteCounselor(
                            user = user,
                            counselor = counselor,
                        )
                    favoriteCounselorRepository.save(favorite)
                }
        }

        logger.info("========== 즐겨찾기 데이터 생성 완료 ==========")
    }
}
