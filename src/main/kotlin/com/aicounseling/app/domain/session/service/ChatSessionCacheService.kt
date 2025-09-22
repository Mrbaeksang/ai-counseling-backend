package com.aicounseling.app.domain.session.service

import com.aicounseling.app.domain.session.dto.MessageItem
import com.aicounseling.app.domain.session.dto.SessionListResponse
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ChatSessionCacheService(
    private val sessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
) {
    @Cacheable(
        cacheNames = ["user-sessions"],
        key = "T(java.lang.String).format('user:%d:b:%s:c:%s:p:%d:s:%d', #userId, #bookmarked, #isClosed, #pageable.pageNumber, #pageable.pageSize)",
    )
    fun getUserSessions(
        userId: Long,
        bookmarked: Boolean?,
        isClosed: Boolean?,
        pageable: Pageable,
    ): Page<SessionListResponse> {
        return sessionRepository.findSessionsWithCounselor(userId, bookmarked, isClosed, pageable)
    }

    @Cacheable(
        cacheNames = ["session-messages"],
        key = "T(java.lang.String).format('session:%d:p:%d:s:%d', #sessionId, #pageable.pageNumber, #pageable.pageSize)",
    )
    fun getSessionMessages(
        sessionId: Long,
        pageable: Pageable,
    ): Page<MessageItem> {
        val messages = messageRepository.findBySessionId(sessionId, pageable)
        val content =
            messages.content.map { message ->
                MessageItem(
                    content = message.content,
                    senderType = message.senderType.name,
                )
            }
        return PageImpl(content, messages.pageable, messages.totalElements)
    }
}
