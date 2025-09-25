package com.aicounseling.app.domain.character.dto

/**
 * 캐릭터 상세 정보 응답 DTO
 *
 * @param id 캐릭터 ID
 * @param name 캐릭터 이름
 * @param title 캐릭터 직책
 * @param avatarUrl 프로필 이미지 URL
 * @param averageRating 평균 별점 (1-10)
 * @param totalSessions 총 세션 수
 * @param description 캐릭터 설명
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
