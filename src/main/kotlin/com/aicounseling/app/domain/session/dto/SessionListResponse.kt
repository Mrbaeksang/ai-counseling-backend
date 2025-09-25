package com.aicounseling.app.domain.session.dto

import java.time.Instant

/**
 * 세션 목록 조회 응답 DTO
 *
 * 목록 화면에서 필요한 최소 정보만 포함
 * - 세션 식별 정보
 * - 표시용 정보 (제목, 캐릭터명, 시간)
 * - UI 상태 정보 (북마크 여부)
 * - characterId: 캐릭터 ID (세션 상세 이동 시 필요)
 * - avatarUrl: 캐릭터 아바타 이미지 URL (목록 UI 표시용)
 */
data class SessionListResponse(
    val sessionId: Long,
    val characterId: Long,
    val title: String,
    val characterName: String,
    val lastMessageAt: Instant,
    val isBookmarked: Boolean,
    val avatarUrl: String? = null,
    val closedAt: Instant? = null,
)
