package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.global.constants.AppConstants

/**
 * 세션 생성 응답 DTO
 *
 * 세션 생성 시 클라이언트에게 반환되는 최소한의 정보를 포함합니다.
 * - sessionId: 생성된 세션의 ID (이후 메시지 전송 시 사용)
 * - counselorId: 상담사 ID (캐시 무효화 및 데이터 일관성용)
 * - counselorName: 상담사 이름 (UI 표시용)
 * - title: 세션 제목 (초기값은 기본 제목)
 * - avatarUrl: 상담사 아바타 이미지 URL (채팅 UI 표시용)
 */
data class CreateSessionResponse(
    val sessionId: Long,
    val counselorId: Long,
    val counselorName: String,
    val title: String = AppConstants.Session.DEFAULT_SESSION_TITLE,
    val avatarUrl: String? = null,
)
