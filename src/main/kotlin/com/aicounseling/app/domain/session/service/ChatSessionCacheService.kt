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
        key = USER_SESSION_CACHE_KEY,
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
        key = SESSION_MESSAGES_CACHE_KEY,
    )
    fun getSessionMessages(
        sessionId: Long,
        pageable: Pageable,
    ): Page<MessageItem> {
        val messages = messageRepository.findBySessionId(sessionId, pageable)
        val content = messages.content.map(MessageItem::from)
        return PageImpl(content, messages.pageable, messages.totalElements)
    }

    companion object {
        private const val USER_SESSION_CACHE_KEY =
            "T(java.lang.String).format('user:%d:b:%s:c:%s:p:%d:s:%d', " +
                "#userId, #bookmarked, #isClosed, #pageable.pageNumber, #pageable.pageSize)"
        private const val SESSION_MESSAGES_CACHE_KEY =
            "T(java.lang.String).format('session:%d:p:%d:s:%d', " +
                "#sessionId, #pageable.pageNumber, #pageable.pageSize)"
    }
}
