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
        const val DEFAULT_SESSION_TITLE = "새 대화"

        // AI 응답 관련
        const val AI_RETRY_MAX_COUNT = 3
        const val AI_RETRY_DELAY_BASE = 1000L
        const val AI_RESPONSE_MIN_LENGTH = 10
        const val LOG_PREVIEW_LENGTH = 100

        val BASE_SYSTEM_PROMPT_NOTICE: String = """
[안내]
- 이 대화는 AI 캐릭터와 나누는 엔터테인먼트용 이야기로, 사실과 다를 수 있습니다.
- 폭력, 혐오, 개인정보 노출, 의료·법률과 같은 전문 조언을 요청하거나 제공하지 마세요.
""".trimIndent()

        val RESPONSE_JSON_FORMAT: String = """
[응답 형식]
항상 아래 JSON 형식(코드블록 없이)으로만 응답하세요.
{
  "content": "AI 캐릭터가 전할 대사"
}
""".trimIndent()

        val FIRST_MESSAGE_JSON_FORMAT: String = """
[첫 응답 형식]
첫 메시지에서는 제목을 포함한 JSON 을 반환하세요. 코드블록은 사용하지 마세요.
{
  "content": "첫 응답 내용",
  "title": "대화를 대표할 15자 이내 제목"
}
""".trimIndent()
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
        const val MESSAGE_NOT_FOUND = "메시지를 찾을 수 없습니다"
        const val MESSAGE_ALREADY_REPORTED = "이미 신고한 메시지입니다"
    }
}
