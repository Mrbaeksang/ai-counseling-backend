package com.aicounseling.app.domain.session.dto

import java.time.LocalDateTime

/**
 * 세션 목록 조회 응답 DTO
 *
 * 목록 화면에서 필요한 최소 정보만 포함
 * - 세션 식별 정보
 * - 표시용 정보 (제목, 상담사명, 시간)
 * - UI 상태 정보 (북마크 여부)
 * - counselorId: 상담사 ID (세션 상세 이동 시 필요)
 * - avatarUrl: 상담사 아바타 이미지 URL (목록 UI 표시용)
 */
data class SessionListResponse(
    val sessionId: Long,
    val counselorId: Long,
    val title: String,
    val counselorName: String,
    val lastMessageAt: LocalDateTime,
    val isBookmarked: Boolean,
    val avatarUrl: String? = null,
    val closedAt: LocalDateTime? = null,
)
