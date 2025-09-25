package com.aicounseling.app.domain.session.dto

/**
 * 메시지 전송 응답 DTO
 *
 * @property sessionTitle 첫 메시지 후 타이틀 생성 시에만 포함
 */
data class SendMessageResponse(
    val userMessageId: Long,
    val userMessage: String,
    val aiMessageId: Long,
    val aiMessage: String,
    val sessionTitle: String? = null,
    val isSessionEnded: Boolean = false,
)
