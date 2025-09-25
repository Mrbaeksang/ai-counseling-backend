package com.aicounseling.app.domain.session.entity

import com.aicounseling.app.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

/**
 * ChatSession 엔티티 - 상담 대화 세션 (순수 데이터만)
 * 비즈니스 로직은 ChatSessionService로 이동
 */
@Entity
@Table(name = "chat_sessions")
class ChatSession(
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "character_id", nullable = false)
    val characterId: Long,
    @Column(nullable = true, length = 100)
    var title: String? = null,
    @Column(name = "is_bookmarked", nullable = false)
    var isBookmarked: Boolean = false,
    @Column(name = "last_message_at")
    var lastMessageAt: Instant? = null,
    @Column(name = "closed_at")
    var closedAt: Instant? = null,
) : BaseEntity()
