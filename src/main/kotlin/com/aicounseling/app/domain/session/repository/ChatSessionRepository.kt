package com.aicounseling.app.domain.session.repository

import com.aicounseling.app.domain.session.entity.ChatSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatSessionRepository : JpaRepository<ChatSession, Long>, ChatSessionRepositoryCustom {
    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): ChatSession?

    fun countByCounselorId(counselorId: Long): Long
}
