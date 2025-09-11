package com.aicounseling.app.domain.session.entity

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
@Table(name = "messages")
class Message(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: ChatSession,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val senderType: SenderType,
    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val phase: CounselingPhase,
) : BaseEntity()

enum class SenderType {
    USER,
    AI,
}
