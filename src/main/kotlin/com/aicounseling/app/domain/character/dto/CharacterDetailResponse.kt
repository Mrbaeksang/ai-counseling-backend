package com.aicounseling.app.domain.character.dto

/**
 * 상담사 상세 정보 응답 DTO
 *
 * @param id 상담사 ID
 * @param name 상담사 이름
 * @param title 상담사 직책
 * @param avatarUrl 프로필 이미지 URL
 * @param averageRating 평균 별점 (1-10)
 * @param totalSessions 총 세션 수
 * @param description 상담사 설명
 * @param totalRatings 총 평가 수
 * @param isFavorite 즐겨찾기 여부 (사용자별)
 * @param categories 카테고리 목록 (콤마로 구분)
 */
data class CharacterDetailResponse(
    val id: Long,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val averageRating: Int,
    val totalSessions: Int,
    val description: String?,
    val totalRatings: Int,
    val isFavorite: Boolean = false,
    val categories: String? = null,
)
