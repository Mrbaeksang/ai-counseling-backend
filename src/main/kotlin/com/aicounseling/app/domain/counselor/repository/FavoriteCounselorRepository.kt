package com.aicounseling.app.domain.counselor.repository

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.FavoriteCounselor
import com.aicounseling.app.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

/**
 * FavoriteCounselorRepository - 즐겨찾기 관리
 *
 * 핵심 기능:
 * - 즐겨찾기 토글 (exists 체크 → save/delete)
 * - 즐겨찾기 목록 조회 (Custom 메서드로 평점 포함)
 */
interface FavoriteCounselorRepository : JpaRepository<FavoriteCounselor, Long>, FavoriteCounselorRepositoryCustom {
    /**
     * 이미 즐겨찾기 했는지 확인
     * 용도: 하트 아이콘 상태 표시, 토글 전 체크
     */
    fun existsByUserAndCounselor(
        user: User,
        counselor: Counselor,
    ): Boolean

    /**
     * 즐겨찾기 삭제
     * 용도: 즐겨찾기 해제 (♥ → ♡)
     */
    fun deleteByUserAndCounselor(
        user: User,
        counselor: Counselor,
    )

    /**
     * 사용자 ID와 상담사 ID로 즐겨찾기 여부 확인
     * 용도: 상담사 상세 조회 시 즐겨찾기 상태 표시
     */
    fun existsByUserIdAndCounselorId(
        userId: Long,
        counselorId: Long,
    ): Boolean
}
