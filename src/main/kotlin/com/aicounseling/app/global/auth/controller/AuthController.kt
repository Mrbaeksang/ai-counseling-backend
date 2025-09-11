package com.aicounseling.app.global.auth.controller

import com.aicounseling.app.global.auth.dto.AuthResponse
import com.aicounseling.app.global.auth.dto.OAuthLoginRequest
import com.aicounseling.app.global.auth.dto.RefreshTokenRequest
import com.aicounseling.app.global.auth.service.AuthService
import com.aicounseling.app.global.constants.AppConstants
import com.aicounseling.app.global.exception.UnauthorizedException
import com.aicounseling.app.global.rsData.RsData
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login/google")
    fun loginWithGoogle(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): RsData<AuthResponse> = handleOAuthLogin(request.token, "GOOGLE", "구글 로그인 성공")

    @PostMapping("/login/kakao")
    fun loginWithKakao(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): RsData<AuthResponse> = handleOAuthLogin(request.token, "KAKAO", "카카오 로그인 성공")

    @PostMapping("/login/naver")
    fun loginWithNaver(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): RsData<AuthResponse> = handleOAuthLogin(request.token, "NAVER", "네이버 로그인 성공")

    private fun handleOAuthLogin(
        token: String,
        provider: String,
        successMessage: String,
    ): RsData<AuthResponse> =
        runBlocking {
            try {
                val response = authService.loginWithOAuth(token, provider).awaitSingle()
                RsData.of(
                    AppConstants.Response.SUCCESS_CODE,
                    successMessage,
                    response,
                )
            } catch (e: UnauthorizedException) {
                RsData.of(
                    AppConstants.Response.UNAUTHORIZED_CODE,
                    e.message ?: "인증 실패",
                    null,
                )
            } catch (e: IllegalArgumentException) {
                RsData.of(
                    AppConstants.Response.BAD_REQUEST_CODE,
                    "잘못된 요청: ${e.message}",
                    null,
                )
            } catch (e: IllegalStateException) {
                RsData.of(
                    AppConstants.Response.UNAUTHORIZED_CODE,
                    "${getProviderName(provider)} 로그인 실패: ${e.message}",
                    null,
                )
            }
        }

    private fun getProviderName(provider: String): String =
        when (provider) {
            "GOOGLE" -> "구글"
            "KAKAO" -> "카카오"
            "NAVER" -> "네이버"
            else -> provider
        }

    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): RsData<AuthResponse> {
        return try {
            val response = authService.refreshToken(request.refreshToken)
            RsData.of(
                AppConstants.Response.SUCCESS_CODE,
                "토큰 갱신 성공",
                response,
            )
        } catch (e: UnauthorizedException) {
            RsData.of(
                AppConstants.Response.UNAUTHORIZED_CODE,
                e.message ?: "인증 실패",
                null,
            )
        } catch (e: IllegalArgumentException) {
            RsData.of(
                AppConstants.Response.BAD_REQUEST_CODE,
                "잘못된 토큰 형식: ${e.message}",
                null,
            )
        } catch (e: IllegalStateException) {
            RsData.of(
                AppConstants.Response.UNAUTHORIZED_CODE,
                "토큰 갱신 실패: ${e.message}",
                null,
            )
        }
    }
}
