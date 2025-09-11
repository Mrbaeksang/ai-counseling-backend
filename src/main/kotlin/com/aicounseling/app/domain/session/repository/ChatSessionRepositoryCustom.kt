package com.aicounseling.app.domain.session.repository

import com.aicounseling.app.domain.session.dto.SessionListResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * ChatSession 커스텀 레포지토리 인터페이스
 *
 * JDSL을 사용한 복잡한 쿼리를 처리하기 위한 커스텀 메서드 정의
 */
interface ChatSessionRepositoryCustom {
    /**
     * 사용자의 세션 목록을 Counselor 정보와 함께 조회 (N+1 문제 해결)
     *
     * @param userId 사용자 ID
     * @param bookmarked 북마크 필터 (null이면 전체, true면 북마크만)
     * @param pageable 페이징 정보
     * @return 세션 목록 DTO Page
     */
    fun findSessionsWithCounselor(
        userId: Long,
        bookmarked: Boolean?,
        pageable: Pageable,
    ): Page<SessionListResponse>
}
