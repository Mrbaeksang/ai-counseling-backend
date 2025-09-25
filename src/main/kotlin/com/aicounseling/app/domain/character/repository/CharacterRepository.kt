package com.aicounseling.app.domain.character.repository

import com.aicounseling.app.domain.character.entity.Character
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * CharacterRepository - 캐릭터 데이터 액세스
 *
 * Custom 메서드를 통해 모든 조회 처리
 * - findCharactersWithStats: 목록 조회 (정렬, 페이징, 통계 포함)
 * - findCharacterDetailById: 상세 조회 (통계 포함)
 */
@Repository
interface CharacterRepository : JpaRepository<Character, Long>, CharacterRepositoryCustom
