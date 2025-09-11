package com.aicounseling.app.domain.counselor.dto

/**
 * 즐겨찾기 상담사 응답 DTO
 *
 * @param id 상담사 ID
 * @param name 상담사 이름
 * @param title 상담사 직책
 * @param avatarUrl 프로필 이미지 URL
 * @param averageRating 평균 별점 (1-10)
 */
data class FavoriteCounselorResponse(
    val id: Long,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val averageRating: Int,
)
