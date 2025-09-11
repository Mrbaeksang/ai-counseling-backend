package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.global.constants.AppConstants
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SendMessageRequest(
    @field:NotBlank(message = "메시지를 입력해주세요")
    @field:Size(
        max = AppConstants.Validation.MAX_CONTENT_LENGTH,
        message = "메시지는 ${AppConstants.Validation.MAX_CONTENT_LENGTH}자 이내로 입력해주세요",
    )
    val content: String,
)
