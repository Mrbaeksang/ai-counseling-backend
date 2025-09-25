package com.aicounseling.app.domain.session.report.entity

import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "message_reports")
class MessageReport(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    val message: Message,
    @Column(name = "reporter_user_id", nullable = false)
    val reporterUserId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 30)
    val reasonCode: MessageReportReason,
    @Column(columnDefinition = "TEXT")
    val detail: String? = null,
) : BaseEntity()

enum class MessageReportReason {
    HARASSMENT,
    SELF_HARM,
    HATE_SPEECH,
    MISINFORMATION,
    SPAM,
    OTHER,
}
