package com.aicounseling.app.domain.counselor.repository

import com.aicounseling.app.domain.counselor.entity.CounselorRating
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * CounselorRatingRepository - 상담사 평가 데이터 액세스
 *
 * 핵심 원칙: 세션당 1개 평가만 허용
 * - 한 번 평가하면 수정/삭제 불가
 * - 중복 평가 방지가 핵심
 */
@Repository
interface CounselorRatingRepository : JpaRepository<CounselorRating, Long> {
    /**
     * 특정 세션에 평가가 있는지 확인
     * 용도: 중복 평가 방지, "평가하기" vs "평가완료" 버튼 표시
     */
    fun existsBySessionId(sessionId: Long): Boolean
}
