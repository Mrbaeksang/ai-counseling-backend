package com.aicounseling.app.domain.character.repository

import com.aicounseling.app.domain.character.dto.CharacterDetailResponse
import com.aicounseling.app.domain.character.dto.CharacterListResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Counselor 커스텀 레포지토리 인터페이스
 *
 * JDSL을 사용한 복잡한 쿼리 처리를 위한 커스텀 메서드 정의
 * - 평균 평점 계산
 * - 총 세션 수 집계
 * - N+1 문제 해결
 */
interface CharacterRepositoryCustom {
    /**
     * 캐릭터 목록을 통계 정보와 함께 조회
     *
     * @param sort 정렬 옵션 (popular: 세션 많은 순, rating: 평점 높은 순, recent: 최신 순)
     * @param pageable 페이징 정보
     * @return 캐릭터 목록 (평균 평점, 총 세션 수 포함)
     */
    fun findCharactersWithStats(
        sort: String,
        pageable: Pageable,
    ): Page<CharacterListResponse>

    /**
     * 캐릭터 상세 정보를 통계와 함께 조회
     *
     * @param characterId 캐릭터 ID
     * @return 캐릭터 상세 정보 (평균 평점, 총 세션 수, 총 평가 수 포함)
     */
    fun findCharacterDetailById(characterId: Long): CharacterDetailResponse?
}
