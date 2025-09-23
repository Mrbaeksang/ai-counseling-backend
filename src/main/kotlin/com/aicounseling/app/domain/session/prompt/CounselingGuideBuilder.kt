package com.aicounseling.app.domain.session.prompt

import com.aicounseling.app.domain.session.entity.CounselingPhase

object CounselingGuideBuilder {
    fun build(): String =
        buildString {
            appendLine("## 전문 상담 가이드라인")
            appendLine()
            appendLine("### 핵심 원칙")
            CounselingPromptConstants.corePrinciples.forEach { principle ->
                appendLine("- $principle")
            }
            appendLine()

            appendLine("### 5단계 상담 진행")
            appendLine()
            CounselingPhase.entries.forEach { phase ->
                appendLine("#### ${phase.displayName}")
                appendLine("- **목표:** ${phase.objective}")
                appendLine("- **응답 체크리스트:**")
                phase.checklist.forEach { item -> appendLine("  • $item") }
                appendLine("- **권장 톤:** ${phase.tone}")
                appendLine("- **전환 힌트:** ${phase.transitionHint}")
                appendLine()
            }

            appendLine("### 금지 사항")
            CounselingPromptConstants.forbiddenPatterns.forEach { pattern ->
                appendLine("- $pattern")
            }
            appendLine()
            appendLine(CounselingPromptConstants.CLOSING_GUIDANCE.trim())
        }
}
