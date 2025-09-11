package com.aicounseling.app.domain.user.dto

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val email: String,
    val nickname: String,
)
