package com.aicounseling.app.domain.counselor.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

/**
 * 세션 평가 요청 DTO
 * POST /sessions/{id}/rate
 *
 * @param rating 평점 (1-10, 별 반개 단위)
 * @param feedback 피드백 텍스트 (선택)
 */
data class RateSessionRequest(
    @field:Min(value = 1, message = "평점은 최소 1점입니다")
    @field:Max(value = 10, message = "평점은 최대 10점입니다")
    val rating: Int,
    @field:Size(max = 500, message = "피드백은 최대 500자까지 가능합니다")
    val feedback: String? = null,
)
