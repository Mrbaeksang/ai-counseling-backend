package com.aicounseling.app.domain.counselor.dto

/**
 * 상담사 목록 조회 응답 DTO
 *
 * @param id 상담사 ID
 * @param name 상담사 이름
 * @param title 상담사 직책
 * @param avatarUrl 프로필 이미지 URL
 * @param averageRating 평균 별점 (1-10)
 * @param totalSessions 총 세션 수
 * @param categories 카테고리 목록 (콤마로 구분)
 * @param isFavorite 즐겨찾기 여부 (사용자별)
 */
data class CounselorListResponse(
    val id: Long,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val averageRating: Int,
    val totalSessions: Int,
    val categories: String? = null,
    val isFavorite: Boolean = false,
)
