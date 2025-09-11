package com.aicounseling.app.domain.counselor.repository

import com.aicounseling.app.domain.counselor.dto.FavoriteCounselorResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * FavoriteCounselor 커스텀 레포지토리 인터페이스
 *
 * 즐겨찾기 상담사 조회 시 N+1 문제를 해결하기 위한 최적화된 쿼리 제공
 */
interface FavoriteCounselorRepositoryCustom {
    /**
     * 사용자의 즐겨찾기 상담사 목록을 평균 평점과 함께 조회
     *
     * @param userId 사용자 ID
     * @return 즐겨찾기 상담사 목록 (평균 평점 포함)
     */
    fun findFavoritesWithRating(
        userId: Long,
        pageable: Pageable,
    ): Page<FavoriteCounselorResponse>
}
