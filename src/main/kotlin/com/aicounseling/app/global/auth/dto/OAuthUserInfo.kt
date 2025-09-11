package com.aicounseling.app.global.auth.dto

data class OAuthUserInfo(
    val providerId: String,
    val email: String,
    val name: String?,
    val provider: String,
    val picture: String? = null,
)
