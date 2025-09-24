package com.aicounseling.app.domain.character.repository

import com.aicounseling.app.domain.character.entity.Character
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * CounselorRepository - 상담사 데이터 액세스
 *
 * Custom 메서드를 통해 모든 조회 처리
 * - findCounselorsWithStats: 목록 조회 (정렬, 페이징, 통계 포함)
 * - findCounselorDetailById: 상세 조회 (통계 포함)
 */
@Repository
interface CharacterRepository : JpaRepository<Character, Long>, CharacterRepositoryCustom
