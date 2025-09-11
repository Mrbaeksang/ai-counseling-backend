package com.aicounseling.app.domain.user.service

import com.aicounseling.app.domain.user.dto.UserProfileResponse
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.constants.AppConstants
import com.aicounseling.app.global.security.AuthProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * UserService - 사용자 비즈니스 로직
 *
 * API 명세서 매핑:
 * - GET /users/me → getUserProfile()
 * - PATCH /users/nickname → updateNickname()
 * - DELETE /users/me → deleteUser()
 *
 * OAuth 로그인 지원:
 * - findOrCreateOAuthUser() → AuthService에서 호출
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    /**
     * 사용자 엔티티 조회 (내부 사용)
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 엔티티
     * @throws NoSuchElementException 사용자를 찾을 수 없을 때
     */
    fun getUser(userId: Long): User {
        return userRepository.findById(userId).orElseThrow {
            NoSuchElementException("${AppConstants.ErrorMessages.USER_NOT_FOUND}: $userId")
        }
    }

    /**
     * GET /users/me - 사용자 프로필 조회
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 프로필 정보
     * @throws NoSuchElementException 사용자를 찾을 수 없을 때
     */
    fun getUserProfile(userId: Long): UserProfileResponse {
        val user = getUser(userId)
        return UserProfileResponse.from(user)
    }

    /**
     * PATCH /users/nickname - 닉네임 변경
     *
     * @param userId 사용자 ID
     * @param newNickname 변경할 닉네임
     * @return 업데이트된 사용자 프로필
     * @throws IllegalArgumentException 닉네임 길이가 유효하지 않을 때
     * @throws NoSuchElementException 사용자를 찾을 수 없을 때
     */
    @Transactional
    fun updateNickname(
        userId: Long,
        newNickname: String,
    ): UserProfileResponse {
        val trimmedNickname = newNickname.trim()
        val minLength = AppConstants.Validation.MIN_NICKNAME_LENGTH
        val maxLength = AppConstants.Validation.MAX_NICKNAME_LENGTH
        require(trimmedNickname.length in minLength..maxLength) {
            "닉네임은 ${minLength}자 이상 ${maxLength}자 이하여야 합니다"
        }

        val user = getUser(userId)
        user.nickname = trimmedNickname
        val updatedUser = userRepository.save(user)

        return UserProfileResponse.from(updatedUser)
    }

    /**
     * DELETE /users/me - 회원 탈퇴
     *
     * CASCADE 설정으로 연관 데이터 자동 삭제:
     * - chat_sessions
     * - messages
     * - user_favorite_counselors
     * - counselor_ratings
     *
     * @param userId 탈퇴할 사용자 ID
     * @throws NoSuchElementException 사용자를 찾을 수 없을 때
     */
    @Transactional
    fun deleteUser(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException("${AppConstants.ErrorMessages.USER_NOT_FOUND}: $userId")
        }

        // CASCADE DELETE로 연관 데이터 자동 삭제
        userRepository.deleteById(userId)
    }

    /**
     * OAuth 로그인 처리 - 기존 사용자 찾기 또는 신규 생성
     *
     * AuthService에서 OAuth 인증 후 호출됨
     *
     * @param provider OAuth 제공자 (GOOGLE/KAKAO/NAVER)
     * @param providerId OAuth 제공자의 사용자 ID
     * @param email 사용자 이메일
     * @param nickname OAuth에서 제공한 이름
     * @param profileImageUrl 프로필 이미지 URL (optional)
     * @return 찾거나 생성된 사용자 엔티티
     */
    @Transactional
    fun findOrCreateOAuthUser(
        provider: AuthProvider,
        providerId: String,
        email: String,
        nickname: String,
        profileImageUrl: String? = null,
    ): User {
        // 기존 사용자 찾기
        val existingUser = userRepository.findByProviderIdAndAuthProvider(providerId, provider)

        if (existingUser != null) {
            // 로그인 시간 업데이트
            existingUser.lastLoginAt = LocalDateTime.now()
            return userRepository.save(existingUser)
        }

        // 신규 사용자 생성 (Zero Friction - 온보딩 없이 바로 가입)
        // OAuth 이름 그대로 사용, 단 최대 길이 제한 적용
        val maxLength = AppConstants.Validation.MAX_NICKNAME_LENGTH
        val finalNickname =
            if (nickname.length > maxLength) {
                nickname.substring(0, maxLength)
            } else {
                nickname
            }

        val newUser =
            User(
                email = email,
                nickname = finalNickname,
                authProvider = provider,
                providerId = providerId,
                profileImageUrl = profileImageUrl,
                isActive = true,
                lastLoginAt = LocalDateTime.now(),
            )

        return userRepository.save(newUser)
    }
}
