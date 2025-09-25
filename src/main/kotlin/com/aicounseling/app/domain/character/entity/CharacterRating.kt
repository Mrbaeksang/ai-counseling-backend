package com.aicounseling.app.domain.character.entity

import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * CharacterRating 엔티티 - 캐릭터 평가
 * 세션 종료 후 사용자가 남기는 평가
 */
@Entity
@Table(
    name = "character_ratings",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["session_id"]),
    ],
)
class CharacterRating(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    val character: Character,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: ChatSession,
    @Column(nullable = false)
    val rating: Int,
    @Column(columnDefinition = "TEXT")
    val review: String? = null,
) : BaseEntity()
