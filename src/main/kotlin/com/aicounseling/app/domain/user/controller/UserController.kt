package com.aicounseling.app.domain.user.controller

import com.aicounseling.app.domain.user.dto.NicknameUpdateRequest
import com.aicounseling.app.domain.user.dto.UserProfileResponse
import com.aicounseling.app.domain.user.service.UserService
import com.aicounseling.app.global.constants.AppConstants
import com.aicounseling.app.global.rq.Rq
import com.aicounseling.app.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val rq: Rq,
) {
    /**
     * GET /users/me - 내 정보 조회
     * OAuth 기반 사용자 프로필 반환
     */
    @GetMapping("/me")
    fun getMyProfile(): RsData<UserProfileResponse> {
        val userId =
            rq.currentUserId
                ?: return RsData.of(AppConstants.Response.UNAUTHORIZED_CODE, "로그인이 필요합니다", null)

        val profile = userService.getUserProfile(userId)
        return RsData.of(
            AppConstants.Response.SUCCESS_CODE,
            "프로필 조회 성공",
            profile,
        )
    }

    /**
     * PATCH /users/nickname - 닉네임 변경
     * Validation은 @Valid로 자동 처리
     */
    @PatchMapping("/nickname")
    fun updateNickname(
        @Valid @RequestBody request: NicknameUpdateRequest,
    ): RsData<UserProfileResponse> {
        val userId =
            rq.currentUserId
                ?: return RsData.of(AppConstants.Response.UNAUTHORIZED_CODE, "로그인이 필요합니다", null)

        val updatedProfile =
            userService.updateNickname(
                userId,
                request.nickname,
            )

        return RsData.of(
            AppConstants.Response.SUCCESS_CODE,
            "닉네임 변경 성공",
            updatedProfile,
        )
    }

    /**
     * DELETE /users/me - 회원 탈퇴
     * CASCADE로 연관 데이터 자동 삭제
     * OAuth라 비밀번호 확인 불필요
     *
     * @return 204 No Content (성공 시)
     */
    @DeleteMapping("/me")
    fun deleteAccount(): RsData<Nothing> {
        val userId =
            rq.currentUserId
                ?: return RsData.of(AppConstants.Response.UNAUTHORIZED_CODE, "로그인이 필요합니다", null)

        userService.deleteUser(userId)

        return RsData.of(
            "S-204",
            "회원 탈퇴가 완료되었습니다",
            null,
        )
    }
}
