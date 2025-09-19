package com.aicounseling.app.domain.user.repository

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.global.security.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    /**
     * OAuth 로그인 시 기존 사용자 찾기
     *
     * @param providerId OAuth 제공자가 주는 고유 ID
     * @param authProvider OAuth 제공자 (GOOGLE/KAKAO)
     * @return 기존 사용자 또는 null
     */
    fun findByProviderIdAndAuthProvider(
        providerId: String,
        authProvider: AuthProvider,
    ): User?

    /**
     * 이메일과 OAuth 제공자로 사용자 찾기
     *
     * @param email 사용자 이메일
     * @param authProvider OAuth 제공자 (GOOGLE/KAKAO)
     * @return 기존 사용자 또는 null
     */
    fun findByEmailAndAuthProvider(
        email: String,
        authProvider: AuthProvider,
    ): User?
}
