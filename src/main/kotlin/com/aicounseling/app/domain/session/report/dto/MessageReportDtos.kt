package com.aicounseling.app.domain.session.report.dto

import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.report.entity.MessageReport
import com.aicounseling.app.domain.session.report.entity.MessageReportReason
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * 메시지 신고 요청 DTO
 */
data class MessageReportRequest(
    val reasonCode: MessageReportReason,
    @field:Size(max = 2000)
    val detail: String? = null,
)

/**
 * 메시지 신고 응답 DTO
 */
data class MessageReportResponse(
    val reportId: Long,
    val sessionId: Long,
    val messageId: Long,
    val reasonCode: MessageReportReason,
    val detail: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(entity: MessageReport): MessageReportResponse {
            val message = entity.message
            return MessageReportResponse(
                reportId = entity.id,
                sessionId = message.session.id,
                messageId = message.id,
                reasonCode = entity.reasonCode,
                detail = entity.detail,
                createdAt = entity.createdAt,
            )
        }
    }
}
