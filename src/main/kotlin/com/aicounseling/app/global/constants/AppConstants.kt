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
        const val PROMPT_PHASE_INSTRUCTION = """[단계 선택 규칙 - 매우 중요!]
1. **절대 규칙**: 이전 단계(%s)보다 낮은 단계로 돌아가지 마세요
2. **최소 단계**: %d번째 대화이므로 최소 %s 이상이어야 합니다
3. **선택 가능한 단계**: %s"""

        const val PROMPT_PHASE_KEYWORDS = """[키워드 기반 단계 판단 가이드]
- ENGAGEMENT 키워드: 안녕, 처음, 시작, 만남
- EXPLORATION 키워드: 고민, 문제, 어려움, 힘든, 때문에, 걱정
- INSIGHT 키워드: 깨달음, 알게, 이해, 패턴, 반복, 왜
- ACTION 키워드: 해볼게, 시도, 실천, 계획, 목표, 방법
- CLOSING 키워드: 감사, 마무리, 정리, 다음에

사용자 메시지에 위 키워드가 포함되면 해당 단계를 우선 고려하되,
반드시 선택 가능한 단계 중에서만 선택하세요."""

        const val PROMPT_RESPONSE_FORMAT = """[응답 형식]
반드시 아래 JSON 형식으로만 응답하세요.
코드블록(```)을 사용하지 마세요. 순수 JSON만 반환하세요.
{
  "content": "상담 응답 내용 (공감적이고 따뜻하게)",
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
