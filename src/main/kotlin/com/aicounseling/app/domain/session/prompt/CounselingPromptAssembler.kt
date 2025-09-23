package com.aicounseling.app.domain.session.prompt

import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.global.constants.AppConstants

object CounselingPromptAssembler {
    fun buildSystemPrompt(
        counselorBasePrompt: String,
        lastPhase: CounselingPhase,
        availablePhases: List<CounselingPhase>,
        conversationSummary: String,
        isFirstMessage: Boolean,
    ): String =
        buildString {
            appendLine("## 상담사 프로필")
            appendLine(counselorBasePrompt.trim())
            appendLine()

            appendLine(CounselingGuideBuilder.build().trim())
            appendLine()

            appendLine("## 현재 상담 상태")
            appendLine("- 직전 단계: ${lastPhase.displayName}")
            appendLine("- 진행 가능한 단계: ${availablePhases.joinToString { it.displayName }}")
            if (conversationSummary.isNotBlank()) {
                appendLine()
                appendLine("## 최근 대화 요약")
                appendLine(conversationSummary.trim())
            }
            appendLine()

            appendLine("## 응답 형식")
            appendLine(AppConstants.Session.PROMPT_RESPONSE_FORMAT.format(availablePhases.joinToString { it.name }))
            if (isFirstMessage) {
                appendLine(AppConstants.Session.PROMPT_FIRST_MESSAGE_FORMAT)
            }
            appendLine()

            appendLine("## 세션 종료 가이드")
            appendLine(CounselingPromptConstants.CLOSING_GUIDANCE.trim())
            appendLine()

            appendLine("## 응답 시 유의사항")
            CounselingPromptConstants.responseGuidelines.forEach { rule ->
                appendLine("- $rule")
            }
        }
}
