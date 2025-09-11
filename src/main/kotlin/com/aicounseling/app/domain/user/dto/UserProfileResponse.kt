package com.aicounseling.app.domain.user.dto

import com.aicounseling.app.domain.user.entity.User
import java.time.LocalDate

/**
 * 사용자 프로필 응답 DTO
 *
 * OAuth 기반 사용자 정보를 클라이언트에 전달
 * GET /users/me, PATCH /users/nickname 응답에 사용
 */
data class UserProfileResponse(
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
    val authProvider: String,
    val memberSince: LocalDate,
) {
    companion object {
        /**
         * Entity → DTO 변환 팩토리 메서드
         */
        fun from(user: User): UserProfileResponse =
            UserProfileResponse(
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl,
                authProvider = user.authProvider.name,
                memberSince = user.createdAt.toLocalDate(),
            )
    }
}
