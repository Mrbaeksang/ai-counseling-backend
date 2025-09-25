package com.aicounseling.app.domain.character.dto

/**
 * 즐겨찾기 캐릭터 응답 DTO
 *
 * @param id 캐릭터 ID
 * @param name 캐릭터 이름
 * @param title 캐릭터 직책
 * @param avatarUrl 프로필 이미지 URL
 * @param averageRating 평균 별점 (1-10)
 */
data class FavoriteCharacterResponse(
    val id: Long,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val averageRating: Int,
)
