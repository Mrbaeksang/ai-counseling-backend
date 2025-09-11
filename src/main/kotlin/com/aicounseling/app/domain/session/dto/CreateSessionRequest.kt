package com.aicounseling.app.domain.session.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 세션 생성 요청 DTO
 *
 * 새로운 상담 세션을 생성하기 위한 요청 정보입니다.
 * - counselorId: 선택한 상담사의 ID (필수)
 */
data class CreateSessionRequest(
    @field:NotNull(message = "상담사 ID는 필수입니다")
    @field:Positive(message = "상담사 ID는 양수여야 합니다")
    val counselorId: Long,
)
