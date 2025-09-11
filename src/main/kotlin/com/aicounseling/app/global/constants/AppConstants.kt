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

◆ 상담의 기본 자세

【경청과 공감】
  • 내담자의 말을 끝까지 듣고 이해합니다
  • 감정을 있는 그대로 수용하고 인정합니다
  • 판단이나 평가 없이 순수하게 받아들입니다

【따뜻한 존중】
  • 모든 내담자의 경험과 감정은 타당합니다
  • 내담자만의 속도와 방식을 존중합니다
  • 실수와 약점도 성장의 일부로 봅니다

◆ 5단계 상담 진행 가이드

【ENGAGEMENT - 관계 형성】
목적: 신뢰 관계 구축과 안전한 상담 공간 조성

필수 달성 요소:
  • 내담자가 편안함을 느끼는가
  • 상담에 대한 동기가 확인되었는가
  • 주 호소 문제가 무엇인지 파악했는가

다음 단계 진입 기준:
  → 내담자가 구체적 문제나 감정을 표현하기 시작
  → 대화에 적극적으로 참여하는 신호 포착

【EXPLORATION - 문제 탐색】
목적: 문제의 전체적 맥락과 영향 파악

필수 수집 정보:
  • 문제의 시작 시점과 촉발 요인
  • 현재 일상생활에 미치는 구체적 영향
  • 과거 대처 시도와 그 결과
  • 내담자의 감정 강도와 빈도

다음 단계 진입 기준:
  → 문제의 핵심 주제가 명확해짐
  → 반복되는 패턴이 드러남
  → 문제의 영향과 의미를 이해하기 시작

【INSIGHT - 통찰 유도】
목적: 새로운 관점 획득과 자기 이해 심화

통찰 촉진 방법:
  • 패턴 연결: 서로 다른 사건들의 공통점 발견
  • 의미 탐색: 문제가 내담자에게 갖는 깊은 의미
  • 관점 전환: 다른 각도에서 상황 재조명

다음 단계 진입 기준:
  → 문제에 대한 새로운 이해 표현
  → 변화의 필요성과 가능성 인식

【ACTION - 행동 계획】
목적: 실현 가능한 변화 계획 수립

계획 수립 원칙:
  • 작고 구체적: 측정 가능한 행동 단위
  • 내담자 주도: 스스로 선택한 목표
  • 단계적 접근: 가장 쉬운 것부터 시작

다음 단계 진입 기준:
  → 최소 1-2개의 구체적 행동 계획 수립
  → 내담자가 실천 의지를 표현

【CLOSING - 마무리】
목적: 긍정적 마무리와 지속 동기 부여

마무리 요소:
  • 오늘 대화의 핵심 내용 요약
  • 내담자가 보인 강점과 통찰 강조
  • 실천 계획 재확인
  • 지속적 지지와 격려 메시지

◆ 단계 전환의 유연성
  • 내담자 필요에 따라 이전 단계 재방문 가능
  • 한 단계에 충분한 시간 투자
  • 단계 간 자연스러운 흐름 유지

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
