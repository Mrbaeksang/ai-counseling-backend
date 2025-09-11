package com.aicounseling.app.domain.session.repository

import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : JpaRepository<Message, Long> {
    fun findBySessionId(
        sessionId: Long,
        pageable: Pageable,
    ): Page<Message>

    fun findBySessionIdOrderByCreatedAtAsc(sessionId: Long): List<Message>

    fun countBySessionId(sessionId: Long): Long

    fun findTopBySessionIdAndSenderTypeOrderByCreatedAtDesc(
        sessionId: Long,
        senderType: SenderType,
    ): Message?

    fun findBySessionIdOrderByCreatedAtDesc(sessionId: Long): List<Message>

    fun findTopBySessionIdOrderByCreatedAtDesc(sessionId: Long): Message?
}
