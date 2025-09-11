package com.aicounseling.app.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 닉네임 변경 요청 DTO
 *
 * PATCH /users/nickname 요청 본문
 * Validation 어노테이션으로 입력값 자동 검증
 */
data class NicknameUpdateRequest(
    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(
        min = MIN_NICKNAME_LENGTH,
        max = MAX_NICKNAME_LENGTH,
        message = "닉네임은 ${MIN_NICKNAME_LENGTH}자 이상 ${MAX_NICKNAME_LENGTH}자 이하여야 합니다",
    )
    val nickname: String,
) {
    companion object {
        const val MIN_NICKNAME_LENGTH = 2
        const val MAX_NICKNAME_LENGTH = 20
    }
}
