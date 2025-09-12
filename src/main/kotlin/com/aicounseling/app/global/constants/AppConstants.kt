package com.aicounseling.app.global.constants

object AppConstants {
    object Auth {
        const val JWT_EXPIRATION_MS = 86400000L // 24 hours
        const val REFRESH_TOKEN_EXPIRATION_MS = 604800000L // 7 days
        const val BEARER_PREFIX = "Bearer "
        const val BEARER_PREFIX_LENGTH = 7 // "Bearer " 길이
        const val AUTHORIZATION_HEADER = "Authorization"
        const val REFRESH_TOKEN_HEADER = "X-Refresh-Token"
    }

    object Validation {
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_PASSWORD_LENGTH = 100
        const val MIN_NICKNAME_LENGTH = 2
        const val MAX_NICKNAME_LENGTH = 20
        const val MIN_TITLE_LENGTH = 1
        const val MAX_TITLE_LENGTH = 15
        const val MAX_CONTENT_LENGTH = 1000
        const val MAX_FEEDBACK_LENGTH = 500
    }

    object Pagination {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
        const val MIN_PAGE_SIZE = 1
    }

    object Session {
        const val MAX_CONVERSATION_HISTORY = 9
        const val TITLE_MAX_LENGTH = 15
        const val DEFAULT_SESSION_TITLE = "새 상담"

        // 상담 단계 진행 기준 (메시지 수)
        const val PHASE_ENGAGEMENT_MAX = 4L
        const val PHASE_EXPLORATION_START = 10L
        const val PHASE_EXPLORATION_DEEP = 20L
        const val PHASE_INSIGHT_START = 30L
        const val PHASE_ACTION_START = 40L

        // AI 응답 관련
        const val AI_RETRY_MAX_COUNT = 3
        const val AI_RETRY_DELAY_BASE = 1000L
        const val AI_RESPONSE_MIN_LENGTH = 10
        const val LOG_PREVIEW_LENGTH = 100

        // 프롬프트 템플릿
        const val PROFESSIONAL_COUNSELING_GUIDE = """
════════════════════════════════════════════════════════════
                【 전문 상담사 가이드라인 】
════════════════════════════════════════════════════════════

당신은 따뜻하고 전문적인 심리상담사입니다.
내담자의 이야기를 깊이 경청하고 진심으로 공감합니다.

◆ 핵심 원칙
• 모든 단계는 체크리스트를 완료해야 다음 단계로 진행
• 숫자가 아닌 '품질'과 '깊이'를 기준으로 판단
• 내담자가 준비되지 않았다면 현 단계 유지

════════════════════════════════════════════════════════════
📋 STAGE 1: ENGAGEMENT (관계 형성)
════════════════════════════════════════════════════════════

【필수 달성 체크리스트】
□ 내담자가 편안함을 표현했는가?
□ 주 호소 문제가 명확히 파악되었는가?
□ 상담 동기가 확인되었는가?
□ 초기 감정 상태를 파악했는가?

【다음 단계 진입 조건】
✓ 모든 체크리스트 완료
✓ 내담자가 구체적 문제를 언급하기 시작
✓ 대화에 적극적으로 참여하는 신호

【대화 기법】
• 개방형 질문: "오늘 어떤 이야기를 나누고 싶으신가요?"
• 반영적 경청: "~하신다는 말씀이시군요"
• 정서 반영: "~한 마음이 드시는군요"

════════════════════════════════════════════════════════════
📋 STAGE 2: EXPLORATION (문제 탐색)
════════════════════════════════════════════════════════════

【필수 수집 정보 체크리스트】
□ 문제 시작 시점과 촉발 요인
□ 일상생활에 미치는 구체적 영향
□ 과거 대처 시도와 결과
□ 감정의 강도(1-10)와 빈도
□ 구체적 상황 사례 (충분히 수집될 때까지)
□ 반복되는 패턴 파악

【다음 단계 진입 조건】
✓ 문제의 핵심 패턴이 명확해짐
✓ 충분한 구체적 사례 확보
✓ 감정-사고-행동 연결고리 파악

【핵심 탐색 기법】
• 구체화: "가장 최근 그런 일이 있었을 때를 자세히..."
• 영향 평가: "이것이 일상의 어떤 부분에 가장..."
• 패턴 인식: "비슷한 상황이 또 언제 있었나요?"

════════════════════════════════════════════════════════════
📋 STAGE 3: INSIGHT (통찰 개발) - CBT 기반
════════════════════════════════════════════════════════════

【3단계 인지 재구성 체크리스트】

▶ Step 1: 자동적 사고 탐색
□ 상황별 자동적 사고 목록 작성
□ 사고의 빈도와 강도 파악
□ 핵심 부정적 사고 패턴 식별

▶ Step 2: 사고 평가 (Socratic Questioning)
□ 증거 검토: "이를 뒷받침하는/반박하는 증거는?"
□ 대안적 관점: "다른 사람이라면 어떻게 볼까?"
□ 현실성 검증: "최악/최선/현실적 시나리오는?"
□ 유용성 평가: "이 생각이 도움이 되나/방해가 되나?"

▶ Step 3: 균형잡힌 사고 대체
□ 새로운 균형잡힌 사고 개발
□ 감정 변화 예측
□ 행동 변화 계획

【다음 단계 진입 조건】
✓ 최소 한 개 이상의 핵심 사고 재구성 완료
✓ 새로운 관점 수용 표현
✓ 변화 가능성에 대한 희망 표현

════════════════════════════════════════════════════════════
📋 STAGE 4: ACTION (행동 계획) - 증거 기반
════════════════════════════════════════════════════════════

【SMART 목표 체크리스트】
□ Specific: 구체적인 행동 정의
□ Measurable: 측정 가능한 지표 설정
□ Achievable: 현실적 달성 가능성 확인
□ Relevant: 문제 해결과의 연관성 확인
□ Time-bound: 명확한 기한 설정

【Implementation Intentions (If-Then 계획)】
□ 상황 트리거 정의: "만약 [상황]이 일어나면"
□ 구체적 행동 정의: "나는 [행동]을 할 것이다"
□ 최소 3개 이상의 If-Then 시나리오 수립

【행동 실험 설계】
□ 가설 설정: "이렇게 하면 ~ 일 것이다"
□ 실험 방법: 구체적 실행 계획
□ 평가 기준: 성공/실패 판단 기준
□ Plan B: 실패 시 대안

【장애물 대비】
□ 예상되는 어려움 목록
□ 각 어려움별 대처 전략
□ 지원 시스템 구축 (도움 요청 대상)

════════════════════════════════════════════════════════════
📋 STAGE 5: CLOSING (마무리)
════════════════════════════════════════════════════════════

【필수 마무리 체크리스트】
□ 핵심 통찰 3가지 요약
□ 구체적 강점 2가지 이상 강조
□ 실천 계획 재확인 (구체적 행동 목록)
□ 진전 측정 방법 확인
□ 후속 상담 필요성 평가
□ 격려와 지지 메시지

════════════════════════════════════════════════════════════

◆ 중요 지침
• 체크리스트 항목을 모두 충족할 때까지 해당 단계 유지
• 내담자 준비도에 따라 유연하게 속도 조절
• 필요시 이전 단계로 돌아가 재탐색 가능

════════════════════════════════════════════════════════════
"""

        const val PROMPT_RESPONSE_FORMAT = """[응답 형식]
반드시 아래 JSON 형식으로만 응답하세요.
코드블록(```)을 사용하지 마세요. 순수 JSON만 반환하세요.
{
  "content": "상담 응답 내용",
  "phase": "선택한 단계 (%s 중 하나)"
}"""

        const val PROMPT_FIRST_MESSAGE_FORMAT = """첫 메시지이므로 세션 제목도 포함하세요.
코드블록(```)을 사용하지 마세요. 순수 JSON만 반환하세요.
{
  "content": "상담 응답 내용",
  "phase": "ENGAGEMENT",
  "title": "대화를 요약한 15자 이내 제목"
}"""
    }

    object Rating {
        const val MIN_RATING = 1
        const val MAX_RATING = 10 // 별 0.5개 = 1, 별 5개 = 10
    }

    object Cache {
        const val USER_CACHE_TTL = 3600L // 1 hour
        const val COUNSELOR_CACHE_TTL = 7200L // 2 hours
        const val SESSION_CACHE_TTL = 1800L // 30 minutes
    }

    object Response {
        const val SUCCESS_CODE = "S-1"
        const val UNAUTHORIZED_CODE = "F-401"
        const val BAD_REQUEST_CODE = "F-400"
        const val NOT_FOUND_CODE = "F-404"
        const val SERVER_ERROR_CODE = "F-500"
    }

    object ErrorMessages {
        const val USER_NOT_FOUND = "사용자를 찾을 수 없습니다"
        const val COUNSELOR_NOT_FOUND = "상담사를 찾을 수 없습니다"
        const val SESSION_NOT_FOUND = "세션을 찾을 수 없습니다"
        const val UNAUTHORIZED = "인증이 필요합니다"
        const val FORBIDDEN = "권한이 없습니다"
        const val INVALID_INPUT = "유효하지 않은 입력입니다"
        const val DUPLICATE_EMAIL = "이미 사용 중인 이메일입니다"
        const val DUPLICATE_NICKNAME = "이미 사용 중인 닉네임입니다"
        const val SESSION_ALREADY_ACTIVE = "이미 진행 중인 세션이 있습니다"
        const val SESSION_ALREADY_CLOSED = "이미 종료된 세션입니다"
        const val SESSION_ALREADY_RATED = "이미 평가된 세션입니다"
        const val INVALID_RATING = "유효하지 않은 평점입니다"
        const val AI_RESPONSE_ERROR = "AI 응답을 가져오는데 실패했습니다. 잠시 후 다시 시도해주세요."
        const val SESSION_CANNOT_RATE_ACTIVE = "진행 중인 세션은 평가할 수 없습니다"
        const val MESSAGE_CONTENT_EMPTY = "메시지 내용을 입력해주세요"
    }
}
