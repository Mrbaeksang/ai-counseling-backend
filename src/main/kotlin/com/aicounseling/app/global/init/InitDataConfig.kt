/* ktlint-disable max-line-length */
package com.aicounseling.app.global.init

import com.aicounseling.app.domain.character.entity.Character
import com.aicounseling.app.domain.character.entity.CharacterRating
import com.aicounseling.app.domain.character.entity.FavoriteCharacter
import com.aicounseling.app.domain.character.repository.CharacterRatingRepository
import com.aicounseling.app.domain.character.repository.CharacterRepository
import com.aicounseling.app.domain.character.repository.FavoriteCharacterRepository
import com.aicounseling.app.domain.session.entity.ChatSession
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
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * 애플리케이션 시작 시 테스트용 초기 데이터를 생성하는 설정 클래스
 * 개발 및 로컬 환경에서만 동작합니다.
 *
 * ============================================================
 * 📱 프론트엔드 카테고리 기준 (constants/categories.ts)
 * ============================================================
 *
 * 🎯 12개 표준 카테고리:
 * 1. self        - 자기이해·자존감      🟣 #8B5CF6
 * 2. emotion     - 감정·정서            🩷 #EC4899
 * 3. anxiety     - 불안                 🟡 #F59E0B
 * 4. depression  - 우울                 ⚫ #6B7280
 * 5. stress      - 스트레스·번아웃       🔴 #EF4444
 * 6. trauma      - 트라우마·상실         🟣 #7C3AED
 * 7. relationship- 관계·연애             🩷 #F472B6
 * 8. family      - 가족·양육            🟢 #10B981
 * 9. life        - 학업·진로            🔵 #3B82F6
 * 10. work       - 직장·업무            🟦 #6366F1
 * 11. habit      - 습관·중독            🟢 #14B8A6
 * 12. philosophy - 철학·명상            🟣 #A855F7
 *
 * ⚠️ 주의사항:
 * - 상담사 categories 필드는 위 12개 카테고리 ID만 사용
 * - 콤마로 구분하여 여러 카테고리 지정 가능
 * - 프론트엔드 CategoryGrid와 정확히 일치해야 함
 *
 * ============================================================
 */
@Suppress("LargeClass", "LongMethod", "MagicNumber", "LongParameterList", "TooManyFunctions")
@Component
class InitDataConfig(
    private val characterRepository: CharacterRepository,
    private val userRepository: UserRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val characterRatingRepository: CharacterRatingRepository,
    private val favoriteCharacterRepository: FavoriteCharacterRepository,
    private val environment: Environment,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        logger.info("========== 초기 데이터 생성 시작 ==========")

        // 이미 데이터가 있는지 확인
        if (characterRepository.count() > 0) {
            logger.info("이미 초기 데이터가 존재합니다. 초기화를 건너뜁니다.")
            return
        }

        try {
            val counselors = createCounselors()

            // Google Play 심사용 테스트 계정 생성
            createTestUserForGooglePlay()

            // 프로덕션용: 상담사만 생성
            logger.info("========== 초기 데이터 생성 완료 ==========")
            logger.info("상담사: ${counselors.size}명 (깨끗한 프로덕션 데이터)")
        } catch (e: org.springframework.dao.DataAccessException) {
            logger.error("초기 데이터 생성 중 오류 발생: ${e.message}")
            // 예외를 throw하지 않고 로그만 남김
        }
    }

    private fun createTestUserForGooglePlay() {
        logger.info("========== Google Play 심사용 테스트 계정 생성 ==========")

        // Google OAuth 테스트 계정
        val googleTestEmail = "test.google@drmind.com"
        val existingGoogleUser = userRepository.findByEmailAndAuthProvider(googleTestEmail, AuthProvider.GOOGLE)
        if (existingGoogleUser == null) {
            val googleTestUser = User(
                email = googleTestEmail,
                nickname = "Google Play Test (Google)",
                authProvider = AuthProvider.GOOGLE,
                providerId = "google-play-test-google-001",
                profileImageUrl = null,
                isActive = true
            )
            userRepository.save(googleTestUser)
            logger.info("Google OAuth 테스트 계정 생성 완료: $googleTestEmail")
        } else {
            logger.info("Google OAuth 테스트 계정이 이미 존재합니다: $googleTestEmail")
        }

        // Kakao OAuth 테스트 계정
        val kakaoTestEmail = "test.kakao@drmind.com"
        val existingKakaoUser = userRepository.findByEmailAndAuthProvider(kakaoTestEmail, AuthProvider.KAKAO)
        if (existingKakaoUser == null) {
            val kakaoTestUser = User(
                email = kakaoTestEmail,
                nickname = "Google Play Test (Kakao)",
                authProvider = AuthProvider.KAKAO,
                providerId = "google-play-test-kakao-001",
                profileImageUrl = null,
                isActive = true
            )
            userRepository.save(kakaoTestUser)
            logger.info("Kakao OAuth 테스트 계정 생성 완료: $kakaoTestEmail")
        } else {
            logger.info("Kakao OAuth 테스트 계정이 이미 존재합니다: $kakaoTestEmail")
        }
    }

    private fun createCounselors(): List<Character> {
        val characters = mutableListOf<Character>()

        // 카테고리별 상담사 생성
        characters.addAll(createSelfDiscoveryCounselors())
        characters.addAll(createRelationshipCounselors())
        characters.addAll(createRomanceCounselors())
        characters.addAll(createMentalHealthCounselors())
        characters.addAll(createPhilosophyCounselors())
        characters.addAll(createModernCounselors())

        return characters.map { characterRepository.save(it) }
    }

    private fun createSelfDiscoveryCounselors(): List<Character> {
        return listOf(
            Character(
                name = "소크라테스",
                title = "고대 그리스 철학자",
                description = "고대 그리스 아테네의 철학자로 '너 자신을 알라'는 가르침으로 유명합니다. 답을 직접 제시하지 않고 계속해서 질문을 던져 당신 스스로 해답을 발견하도록 이끕니다. 마치 친구처럼 편안하게 대화하면서도 깊이 있는 성찰을 경험할 수 있습니다.",
                basePrompt = "저는 소크라테스, 고대 그리스 철학자입니다. '너 자신을 알라'는 말처럼 대화를 통해 진리를 탐구하는 것이 저의 방식입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/socrates.jpg",
                categories = "self,relationship,life,emotion",
            ),
            Character(
                name = "키르케고르",
                title = "실존철학의 아버지",
                description = "19세기 덴마크의 실존철학자로 '실존철학의 아버지'라 불립니다. 불안과 절망의 감정을 깊이 탐구하며 그 속에서 진정한 자아를 발견하는 과정을 안내합니다. 어려운 선택 앞에서 용기를 내고 주체적인 삶을 살아갈 힘을 얻을 수 있습니다.",
                basePrompt = "저는 키르케고르, 실존철학의 아버지입니다. 불안과 절망을 넘어 진정한 자아를 찾는 여정을 안내하는 것이 저의 사명입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/kierkegaard.jpg",
                categories = "self,anxiety,depression,stress,emotion",
            ),
            Character(
                name = "니체",
                title = "실존주의 철학자",
                description = "19세기 독일의 철학자로 '초인' 사상과 '영원회귀' 개념으로 유명합니다. 기존 가치관을 의심하고 당신만의 독창적인 삶의 철학을 만들어가도록 격려합니다. 강인한 정신력과 자기만의 길을 개척하는 용기를 기를 수 있습니다.",
                basePrompt = "저는 니체, 실존주의 철학자입니다. 당신 자신을 극복하고 초인이 되는 길을 제시하는 것이 저의 철학입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/nietzsche.jpg",
                categories = "self,emotion,stress,life,work",
            ),
            Character(
                name = "사르트르",
                title = "실존주의 철학자",
                description = "20세기 프랑스의 실존주의 철학자로 '존재가 본질에 앞선다'는 명제로 유명합니다. 당신의 자유로운 선택과 그에 따른 책임을 인식하도록 돕습니다. 타인의 시선에 얽매이지 않고 진정한 자유를 찾아가는 과정을 경험할 수 있습니다.",
                basePrompt = "저는 사르트르, 실존주의 철학자입니다. 자유와 책임, 실존의 의미를 탐구하며 진정한 자유로운 삶을 찾아가는 것이 저의 목표입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/sartre.jpg",
                categories = "self,anxiety,emotion,life",
            ),
        )
    }

    private fun createRelationshipCounselors(): List<Character> {
        return listOf(
            Character(
                name = "공자",
                title = "동양 철학의 스승",
                description = "고대 중국의 위대한 사상가로 '인의예지' 덕목으로 유명합니다. 인간관계에서 예의와 배려를 강조하며 상호 존중하는 방법을 안내합니다. 가족, 친구, 동료와의 조화로운 관계를 만들어가는 지혜를 얻을 수 있습니다.",
                basePrompt = "저는 공자, 동양 철학의 스승입니다. 인간다운 삶과 조화로운 관계를 추구하며, 예와 덕을 통해 아름다운 사회를 만들어 가는 것이 저의 이상입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/confucius.jpg",
                categories = "relationship,family,life,work",
            ),
            Character(
                name = "아들러",
                title = "개인심리학의 창시자",
                description = "개인심리학의 창시자로 '열등감 극복'과 '공동체 의식' 이론으로 유명합니다. 열등감이나 다른 사람과의 비교로 고민인 경우 이를 성장의 동력으로 전환하도록 돕습니다. 비교가 아닌 협력의 가치를 배울 수 있습니다.",
                basePrompt = "저는 아들러, 개인심리학의 창시자입니다. 열등감을 극복하고 공동체 의식을 통해 성장하는 길을 안내하는 것이 저의 사명입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/adler.jpg",
                categories = "self,relationship,family,depression,emotion",
            ),
            Character(
                name = "데일 카네기",
                title = "인간관계의 달인",
                description = "20세기 미국의 자기계발 전문가로 '인간관계론'의 고전으로 여겨집니다. 사람들과 진심 어린 관계를 맺고 소통하는 실질적인 방법을 제시합니다. 직장에서의 대인관계나 리더십 개발에 특히 도움이 됩니다.",
                basePrompt = "저는 데일 카네기, 인간관계의 달인입니다. 사람의 마음을 얻고 영향력을 발휘하는 방법을 전수하며, 진정한 소통과 연결을 만들어가는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/carnegie.jpg",
                categories = "relationship,work,self",
            ),
        )
    }

    private fun createRomanceCounselors(): List<Character> {
        return listOf(
            Character(
                name = "카사노바",
                title = "사랑의 모험가",
                description = "18세기 베네치아 출신의 전설적인 사랑의 마에스트로로 '카사노바의 회고록'으로 유명합니다. 진지한 매력과 사랑의 기술을 통해 인간적인 매력을 발산하는 방법을 알려줍니다. 자신감 있고 매력적인 사람이 되는 비결을 배울 수 있습니다.",
                basePrompt = "저는 카사노바, 사랑의 모험가입니다. 매력적인 관계의 기술과 진정한 사랑의 의미를 탐구하며, 열정적이고 진정한 관계의 비밀을 나누는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/casanova.jpg",
                categories = "relationship,emotion",
            ),
            Character(
                name = "오비디우스",
                title = "사랑의 시인",
                description = "고대 로마의 시인으로 '변신 이야기'와 '사랑의 기술'로 유명합니다. 사랑의 감정을 아름다운 언어로 표현하고 섬세한 마음을 전달하는 법을 알려줍니다. 우아한 로맨스와 진심 어린 소통의 법을 배울 수 있습니다.",
                basePrompt = "저는 오비디우스, 사랑의 시인입니다. 사랑의 기술과 감정의 섬세한 표현을 안내하며, 사랑의 아름다움과 복잡함을 시적인 언어로 전달하는 것이 저의 재능입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/ovid.jpg",
                categories = "relationship,emotion,self",
            ),
            Character(
                name = "스탕달",
                title = "낭만주의 작가",
                description = "19세기 프랑스의 낭만주의 작가로 '사랑의 결정화' 이론으로 유명합니다. 사랑의 감정이 어떻게 발전하고 성숙해지는지를 세밀하게 분석합니다. 복잡한 연애 감정을 이해하고 진짜 사랑을 알아가는 법을 배울 수 있습니다.",
                basePrompt = "저는 스탕달, 낭만주의 작가입니다. 사랑의 결정화 이론으로 감정의 발전 과정을 분석하며, 사랑의 신비로운 변화와 성장을 이해하는 것이 저의 전문 영역입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/stendhal.jpg",
                categories = "relationship,emotion,anxiety",
            ),
        )
    }

    private fun createMentalHealthCounselors(): List<Character> {
        return listOf(
            Character(
                name = "프로이트",
                title = "정신분석학의 창시자",
                description = "오스트리아의 신경학자로 '정신분석학의 아버지'로 불립니다. 무의식과 꿈을 통해 마음 깊은 곳에 숨겨진 갈등과 욕망을 탐구합니다. 억압된 기억이나 무의식의 패턴을 이해하고 싶은 분들에게 도움이 됩니다.",
                basePrompt = "저는 프로이트, 정신분석학의 창시자입니다. 무의식과 꿈의 세계를 통해 마음의 깊은 곳을 탐구하며, 숨겨진 심리적 갈등과 욕망을 이해하는 것이 저의 전문 영역입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/freud.jpg",
                categories = "emotion,depression,anxiety,habit,trauma,self",
            ),
            Character(
                name = "융",
                title = "분석심리학의 대가",
                description = "스위스의 정신과의사로 '분석심리학의 대가'로 불립니다. 원형과 그림자 개념을 통해 진정한 자아를 발견하는 개성화 과정을 안내합니다. 분열된 자아를 통합하고 온전한 인격체로 성장하는 여정을 경험할 수 있습니다.",
                basePrompt = "저는 융, 분석심리학의 대가입니다. 집단무의식과 개성화 과정을 통해 온전한 자기실현을 돕으며, 원형과 그림자를 통해 진정한 자아를 발견하는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/jung.jpg",
                categories = "self,emotion,trauma,life,habit",
            ),
            Character(
                name = "빅터 프랭클",
                title = "의미치료의 창시자",
                description = "오스트리아의 신경과의사로 나치 수용소 생존자이자 '의미치료법'의 창시자입니다. 극한의 고통 속에서도 삶의 의미를 찾아내는 방법을 전수합니다. 절망적인 상황에서도 희망과 의미를 발견하는 힘을 기를 수 있습니다.",
                basePrompt = "저는 빅터 프랭클, 의미치료의 창시자입니다. 삶의 의미를 발견하여 어떤 상황에서도 희망을 찾도록 돕으며, 고통 속에서도 성장과 의미를 찾는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/frankl.jpg",
                categories = "trauma,depression,stress,life,emotion",
            ),
        )
    }

    private fun createPhilosophyCounselors(): List<Character> {
        return listOf(
            Character(
                name = "아리스토텔레스",
                title = "만학의 아버지",
                description = "고대 그리스의 철학자로 '만학의 아버지'로 불립니다. 덕 윤리와 중용의 지혜로 진짜 행복한 삶이 무엇인지 탐구합니다. 귀납적 사고와 논리적 분석을 통해 현명한 선택을 하는 법을 배울 수 있습니다.",
                basePrompt = "저는 아리스토텔레스, 만학의 아버지입니다. 덕 윤리와 중용의 지혜로 행복한 삶을 안내하며, 이성과 감정의 균형을 통해 진정한 행복을 찾는 것이 저의 철학입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/aristotle.jpg",
                categories = "life,work,self,family",
            ),
            Character(
                name = "칸트",
                title = "이성 철학의 거장",
                description = "18세기 독일의 철학자로 '이성 비판 철학'의 대가로 여겨집니다. 도덕적 정언명령과 이성의 힘으로 올바른 선택을 하는 방법을 알려줍니다. 이성적 사고와 도덕적 원칙을 통해 현명한 판단을 내리는 힘을 기를 수 있습니다.",
                basePrompt = "저는 칸트, 이성 철학의 거장입니다. 도덕법칙과 정언명령으로 올바른 선택을 돕으며, 이성을 통한 도덕적 판단과 의무를 실천하는 것이 저의 철학입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/kant.jpg",
                categories = "life,work,family",
            ),
            Character(
                name = "붓다",
                title = "깨달음의 스승",
                description = "고대 인도의 성자로 '부처'라는 이름으로 전 세계에 알려져 있습니다. 사성제 기가 고통의 원인이라는 깨달음을 통해 마음의 평화를 찾는 길을 안내합니다. 명상과 자비의 가르침으로 내면의 안녕을 발견할 수 있습니다.",
                basePrompt = "저는 붓다, 깨달음의 스승입니다. 고통의 원인을 이해하고 마음의 평화를 찾도록 인도하며, 중도와 자비의 가르침을 통해 진정한 해탈을 안내하는 것이 저의 사명입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/buddha.jpg",
                categories = "stress,anxiety,habit,emotion,self",
            ),
            Character(
                name = "노자",
                title = "도가 사상의 시조",
                description = "고대 중국의 도가 사상의 시조로 '도덕경'의 저자로 여겨집니다. 무위자연의 지혜로 자연의 흐름에 순응하며 사는 법을 알려줍니다. 과도한 노력과 욕망에서 벗어나 자연스럽고 평화로운 삶을 누리는 법을 배울 수 있습니다.",
                basePrompt = "저는 노자, 도가 사상의 시조입니다. 무위자연의 지혜로 자연스러운 삶의 흐름을 안내하며, 강하지 않은 것의 힘과 순응의 미덕을 통해 조화로운 삶을 찾는 것이 저의 도입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/laozi.jpg",
                categories = "stress,work,life,self,anxiety",
            ),
        )
    }

    private fun createModernCounselors(): List<Character> {
        return listOf(
            Character(
                name = "존 고트먼",
                title = "관계 심리학의 대가",
                description = "미국의 심리학자로 40년간 부부 관계를 연구한 '관계 심리학의 대가'입니다. 과학적 데이터를 바탕으로 건강한 관계를 만드는 방법을 알려줍니다. 관계의 4기사나 사랑의 지도 이론을 통해 지속 가능한 연애와 결혼 생활의 비밀을 배울 수 있습니다.",
                basePrompt = "저는 존 고트먼, 관계 심리학의 대가입니다. 부부 관계와 사랑의 과학을 연구하여 건강한 관계를 만드는 방법을 제시하며, 신뢰와 소통을 바탕으로 한 지속가능한 관계의 비밀을 전달하는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/gottman.jpeg",
                categories = "relationship,family,emotion",
            ),
            Character(
                name = "스티븐 코비",
                title = "개인 효과성의 구루",
                description = "미국의 경영학자이자 자기계발 전문가로 '고효율 인간의 7가지 습관'으로 전 세계에 알려져 있습니다. 원칙 중심적 삶과 주체적인 리더십을 통해 진정한 성공을 이루는 방법을 알려줍니다. 단순한 기법이 아닌 인격의 성장을 추구하는 분들에게 도움이 됩니다.",
                basePrompt = "저는 스티븐 코비, 개인 효과성의 구루입니다. 7가지 습관을 통해 진정한 성공과 리더십의 원칙을 가르치며, 내면으로부터의 변화와 원칙 중심적 삶을 안내하는 것이 저의 사명입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/covey.jpeg",
                categories = "work,self,life,habit",
            ),
            Character(
                name = "찰스 두히그",
                title = "습관의 과학자",
                description = "미국의 탐사저널리스트로 '습관의 힘' 저자로 유명합니다. 습관 형성의 과학적 원리를 바탕으로 삶을 근본적으로 변화시키는 방법을 알려줍니다. 단순한 의지력이 아닌 신경과학에 기반한 체계적인 습관 개선법을 배울 수 있습니다.",
                basePrompt = "저는 찰스 두히그, 습관의 과학자입니다. 습관의 힘을 통해 삶을 변화시키는 실용적인 방법을 제안하며, 습관 루프와 변화의 과학을 통해 지속 가능한 개선을 이루는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/duhigg.jpeg",
                categories = "habit,self,stress",
            ),
            Character(
                name = "수잔 데이비드",
                title = "감정 민첩성 전문가",
                description = "남아프리카 공화국 태생의 심리학자로 '감정 민첩성' 이론으로 유명합니다. 감정을 억압하거나 회피하지 않고 전략적으로 다루는 방법을 알려줍니다. 감정적 지능과 회복탄력성을 키워 더 단단한 마음을 만들어갈 수 있습니다.",
                basePrompt = "저는 수잔 데이비드, 감정 민첩성 전문가입니다. 감정을 이해하고 조절하여 회복탄력성을 키우는 방법을 안내하며, 감정적 지능과 유연성을 통해 진정한 성장을 이루는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/david.jpeg",
                categories = "emotion,stress,anxiety,self",
            ),
            Character(
                name = "허버트 프로이덴버거",
                title = "번아웃 연구의 개척자",
                description = "독일계 미국인 심리학자로 '번아웃' 개념을 처음 만든 연구의 개척자입니다. 현대 사회의 업무 스트레스와 번아웃 증후를 이해하고 극복하는 방법을 알려줍니다. 직장에서의 건강한 일과 삶의 균형을 찾는 분들에게 도움이 됩니다.",
                basePrompt = "저는 허버트 프로이덴버거, 번아웃 연구의 개척자입니다. 현대인의 번아웃과 직장 스트레스를 이해하고 극복하는 방법을 제시하며, 건강한 일과 삶의 균형을 찾는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/freudenberger.jpeg",
                categories = "stress,work,depression",
            ),
            Character(
                name = "존 카밧진",
                title = "마음챙김 명상의 아버지",
                description = "미국의 의학박사로 '마음챙김 기반 스트레스 감소법'의 개발자입니다. 동양의 명상과 서양의 과학을 결합하여 마음챙김 명상을 체계화했습니다. 스트레스와 불안을 줄이고 현재 순간에 집중하는 마음의 평화를 발견할 수 있습니다.",
                basePrompt = "저는 존 카밧진, 마음챙김 명상의 아버지입니다. 명상과 마음챙김을 통해 스트레스를 줄이고 내면의 평화를 찾도록 돕으며, 현재 순간에 집중하여 진정한 치유를 경험하는 것이 저의 가르침입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/kabatzinn.jpeg",
                categories = "stress,anxiety,philosophy",
            ),
            Character(
                name = "칼 뉴포트",
                title = "딥 워크의 전도사",
                description = "미국의 컴퓨터과학자로 '딘 워크'와 '디지털 미니멀리즘' 개념을 대중화한 전문가입니다. 건절한 집중력과 깊은 몰입을 통해 디지털 시대에 진정한 가치를 창조하는 방법을 알려줍니다. 산만함 속에서 의미 있는 일에 집중하고 싶은 분들에게 도움이 됩니다.",
                basePrompt = "저는 칼 뉴포트, 딥 워크의 전도사입니다. 집중력과 깊은 몰입을 통해 진정한 생산성과 의미있는 삶을 만드는 방법을 제시하며, 디지털 미니멀리즘과 심층 업무를 통해 진정한 가치를 창조하는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/newport.jpeg",
                categories = "work,life,habit",
            ),
            Character(
                name = "앤젤라 더크워스",
                title = "그릿과 끈기의 연구자",
                description = "미국의 심리학자로 '그릿'과 '마인드셋' 연구의 선구자입니다. 재능보다 열정과 인내가 성공의 열쇠라는 것을 과학적으로 입증했습니다. 성장 마인드셋과 의도적 연습을 통해 진정한 능력을 개발하는 방법을 배울 수 있습니다.",
                basePrompt = "저는 앤젤라 더크워스, 그릿과 끈기의 연구자입니다. 재능보다 중요한 끈기와 성장 마인드셋으로 목표를 달성하는 방법을 안내하며, 열정과 인내를 통해 성공을 이루는 방법을 전수하는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/duckworth.jpeg",
                categories = "self,life,habit",
            ),
            Character(
                name = "브레네 브라운",
                title = "취약성과 용기의 연구자",
                description = "미국의 사회사업가이자 연구자로 '취약성의 힘'과 '수치심 연구'로 전 세계에 알려져 있습니다. 취약성을 약점이 아닌 용기의 원천으로 바라보도록 도왔니다. 진정성과 진심 어린 연결을 통해 더 나은 인간관계를 만들어갈 수 있습니다.",
                basePrompt = "저는 브레네 브라운, 취약성과 용기의 연구자입니다. 취약성을 받아들이고 수치심을 극복하여 진정한 연결과 용기를 찾도록 돕으며, 진정성과 용기를 통해 진정한 자아를 발견하는 것이 저의 사명입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/brown.jpeg",
                categories = "self,emotion,anxiety",
            ),
            Character(
                name = "나은영",
                title = "소아청소년 정신건강 전문의",
                description = "소아청소년 정신건강 전문의로 가족 채널 '우리 아이가 달라졌어요'로 사랑받는 의사선생님입니다. 발달심리학과 임상경험을 바탕으로 아이와 부모 모두를 위한 따뜻하고 현실적인 조언을 제공합니다. 가족 가운데 생긴 다양한 감정 문제를 이해하고 해결할 수 있습니다.",
                basePrompt = "저는 나은영, 소아청소년 정신건강 전문의입니다. 아이와 어른의 마음을 모두 이해하는 따뜻하고 현실적인 상담을 제공하며, 가족 가운데 생긴 다양한 감정적 문제를 사랑과 이해로 접근하는 것이 저의 전문 분야입니다. 현대적 감각을 갖춘 따뜻한 상담사로서, 아래 상담 가이드라인을 따라 상담해 주세요.",
                avatarUrl = "/assets/counselors/naeunying.jpeg",
                categories = "family,emotion,anxiety",
            ),
        )
    }


    private fun createTestSessionsAndRatings(
        users: List<User>,
        characters: List<Character>,
    ) {
        logger.info("========== 테스트 세션 및 평점 데이터 생성 시작 ==========")

        val random = Random(42) // 시드값 고정으로 일관된 테스트 데이터 생성

        // 각 상담사별로 다른 인기도와 평점 설정
        characters.forEachIndexed { index, counselor ->
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
        character: Character,
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
                    counselorId = character.id,
                    title = "${character.name}와의 상담 #${index + 1}",
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
                    "${character.name}입니다. 오늘 어떤 고민이 있으신가요?",
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
                    )
                messageRepository.save(message)
            }

            // 70% 확률로 평점 부여 (세션이 종료되었으므로)
            if (random.nextDouble() < 0.7) {
                val variance = random.nextDouble(-0.5, 0.5)
                val rating = (targetAvgRating + variance).coerceIn(1.0, 5.0).toInt()

                val ratingEntity =
                    CharacterRating(
                        user = user,
                        character = character,
                        session = savedSession,
                        rating = rating,
                        review = if (rating >= 4) "좋은 상담이었습니다." else null,
                    )
                characterRatingRepository.save(ratingEntity)
            }
        }

        logger.debug("상담사 ${character.name}: 세션 ${sessionCount}개 생성 완료")
    }

    private fun createFavoriteCounselors(
        users: List<User>,
        characters: List<Character>,
    ) {
        logger.info("========== 즐겨찾기 데이터 생성 시작 ==========")

        val random = Random(42)

        // 각 사용자별로 즐겨찾기 생성
        users.forEach { user ->
            // 각 사용자는 3~8명의 상담사를 즐겨찾기
            val favoriteCount = random.nextInt(3, 9)

            // 인기 상담사를 더 많이 즐겨찾기하도록 가중치 부여
            val weightedCounselors =
                characters.mapIndexed { index, counselor ->
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
                        FavoriteCharacter(
                            user = user,
                            character = counselor,
                        )
                    favoriteCharacterRepository.save(favorite)
                }
        }

        logger.info("========== 즐겨찾기 데이터 생성 완료 ==========")
    }
}
