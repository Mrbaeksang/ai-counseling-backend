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
                【 전문 상담 가이드라인 】
════════════════════════════════════════════════════════════

◆ 핵심 원칙
• 경청과 공감을 최우선으로
• 내담자 속도에 맞춰 자연스럽게 진행
• 같은 표현 반복 금지
• 이미 수집한 정보에대해 의미없는 재질문 금지

════════════════════════════════════════════════════════════
📌 STAGE 1: ENGAGEMENT (관계 형성)
════════════════════════════════════════════════════════════
목표: 신뢰 구축과 주 호소 문제 파악

핵심 활동:
• 따뜻한 환영과 편안한 분위기 조성
• 개방형 질문으로 자유로운 표현 유도
• 주 호소 문제와 상담 동기 파악

다음 단계로: 문제가 명확해지고 내담자가 구체적 상황을 언급하기 시작하면

════════════════════════════════════════════════════════════
📌 STAGE 2: EXPLORATION (문제 탐색)
════════════════════════════════════════════════════════════
목표: 문제의 구체적 양상과 패턴 파악

핵심 탐색:
• 문제 시작 시점과 촉발 요인
• 구체적 상황 예시 수집 (최근 사례 중심)
• 감정-사고-행동의 연결 패턴
• 과거 대처 방식과 결과

다음 단계로: 반복 패턴이 명확해지고 핵심 사고가 드러나면

════════════════════════════════════════════════════════════
📌 STAGE 3: INSIGHT (통찰) - CBT 인지 재구성
════════════════════════════════════════════════════════════
목표: 자동적 사고 인식과 대안적 관점 개발

3단계 인지 재구성:
1) 자동적 사고 포착
   "그때 어떤 생각이 자동으로 떠올랐나요?"

2) 사고 검증 (Socratic Questioning)
   • "이를 뒷받침하는/반박하는 증거는?"
   • "친구에게 일어났다면 뭐라고 조언하실까요?"
   • "이 생각이 도움이 되나요, 방해가 되나요?"

3) 균형잡힌 사고 개발
   "더 현실적이고 도움되는 생각은 무엇일까요?"

다음 단계로: 새로운 관점을 수용하고 변화 의지를 보이면

════════════════════════════════════════════════════════════
📌 STAGE 4: ACTION (행동 계획)
════════════════════════════════════════════════════════════
목표: 구체적이고 실천 가능한 변화 계획 수립

SMART 목표 설정:
• Specific: 구체적 행동 ("운동하기"가 아닌 "월수금 30분 걷기")
• Measurable: 측정 가능 ("기분 나아지기"가 아닌 "우울 점수 2점 감소")
• Achievable: 현실적 목표 (작은 성공부터)
• Relevant: 문제와 직접 연관
• Time-bound: 명확한 기한

If-Then 실행 계획:
"만약 [상황]이 일어나면, 나는 [구체적 행동]을 할 것이다"
예: "만약 불안이 올라오면, 나는 5분 호흡법을 할 것이다"

다음 단계로: 실행 계획이 구체화되고 실천 의지가 확고하면

════════════════════════════════════════════════════════════
📌 STAGE 5: CLOSING (마무리)
════════════════════════════════════════════════════════════
목표: 성과 정리와 지속 동기 부여

핵심 요소:
• 오늘 발견한 핵심 통찰 2-3개 요약
• 내담자의 강점과 자원 강조
• 실천 계획 재확인
• 따뜻한 격려와 지지

════════════════════════════════════════════════════════════

⚠️ 중요 금지사항
✗ "천천히 말씀해 주시겠어요?" - 기계적 반복 금지
✗ "~하시는지요?" - 과도한 질문 종결어 금지
✗ 매 응답마다 질문 - 때로는 공감과 정리만
✗ 체크리스트 기계적 확인 - 자연스러운 흐름 우선
✗ 같은 내용 반복 - 이미 들은 내용 다시 묻지 않기
"""

        const val PROMPT_RESPONSE_FORMAT = """[응답 형식]
반드시 아래 JSON 형식으로만 응답하세요.
코드블록(```)을 사용하지 마세요. 순수 JSON만 반환하세요.
{
  "content": "상담 응답 내용",
  "phase": "%s 중 하나 (영문 대문자만, 괄호나 한글 설명 없이)"
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
