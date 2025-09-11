package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.domain.session.entity.Message

/**
 * 메시지 아이템 DTO
 *
 * 채팅 UI에 표시할 최소한의 메시지 정보
 * - content: 메시지 내용
 * - senderType: 발신자 구분 (USER/AI)
 */
data class MessageItem(
    val content: String,
    // "USER" or "AI"
    val senderType: String,
) {
    companion object {
        fun from(message: Message): MessageItem {
            return MessageItem(
                content = message.content,
                senderType = message.senderType.name,
            )
        }
    }
}
