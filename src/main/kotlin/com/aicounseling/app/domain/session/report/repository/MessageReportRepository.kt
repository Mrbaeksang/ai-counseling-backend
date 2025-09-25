package com.aicounseling.app.domain.session.report.repository

import com.aicounseling.app.domain.session.report.entity.MessageReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageReportRepository : JpaRepository<MessageReport, Long> {
    fun existsByReporterUserIdAndMessageId(
        reporterUserId: Long,
        messageId: Long,
    ): Boolean
}
