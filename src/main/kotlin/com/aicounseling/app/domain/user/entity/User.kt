package com.aicounseling.app.domain.user.entity

import com.aicounseling.app.global.entity.BaseEntity
import com.aicounseling.app.global.security.AuthProvider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * User 엔티티 - 사용자 정보 (순수 데이터만)
 * 비즈니스 로직은 UserService로 이동
 */
@Entity
@Table(name = "users")
class User(
    @Column(nullable = false)
    val email: String,
    @Column(nullable = false, length = 100)
    var nickname: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    val authProvider: AuthProvider,
    @Column(name = "provider_id", nullable = false)
    val providerId: String,
    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,
) : BaseEntity()
