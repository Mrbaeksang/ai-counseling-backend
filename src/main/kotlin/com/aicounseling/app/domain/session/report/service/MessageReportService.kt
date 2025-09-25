package com.aicounseling.app.domain.session.report.service

import com.aicounseling.app.domain.session.report.dto.MessageReportRequest
import com.aicounseling.app.domain.session.report.dto.MessageReportResponse
import com.aicounseling.app.domain.session.report.entity.MessageReport
import com.aicounseling.app.domain.session.report.repository.MessageReportRepository
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.global.constants.AppConstants
import com.aicounseling.app.global.exception.BadRequestException
import com.aicounseling.app.global.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MessageReportService(
    private val messageReportRepository: MessageReportRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
) {
    @Transactional
    fun submitReport(
        userId: Long,
        sessionId: Long,
        messageId: Long,
        request: MessageReportRequest,
    ): MessageReportResponse {
        val session =
            chatSessionRepository.findByIdAndUserId(sessionId, userId)
                ?: throw NotFoundException(AppConstants.ErrorMessages.SESSION_NOT_FOUND)

        val message =
            messageRepository.findById(messageId).orElse(null)
                ?: throw NotFoundException(AppConstants.ErrorMessages.MESSAGE_NOT_FOUND)

        if (message.session.id != session.id) {
            throw NotFoundException(AppConstants.ErrorMessages.MESSAGE_NOT_FOUND)
        }

        if (messageReportRepository.existsByReporterUserIdAndMessageId(userId, messageId)) {
            throw BadRequestException(AppConstants.ErrorMessages.MESSAGE_ALREADY_REPORTED)
        }

        val detail = request.detail?.takeIf { it.isNotBlank() }

        val saved =
            messageReportRepository.save(
                MessageReport(
                    message = message,
                    reporterUserId = userId,
                    reasonCode = request.reasonCode,
                    detail = detail,
                ),
            )

        return MessageReportResponse.from(saved)
    }
}
