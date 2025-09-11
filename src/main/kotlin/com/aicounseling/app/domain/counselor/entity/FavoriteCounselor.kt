package com.aicounseling.app.domain.counselor.entity

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.global.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * FavoriteCounselor 엔티티 - 사용자의 상담사 즐겨찾기
 *
 * N:N 관계를 중간 테이블로 구현
 * - 한 사용자는 여러 상담사를 즐겨찾기 가능
 * - 한 상담사는 여러 사용자에게 즐겨찾기 될 수 있음
 */
@Entity
@Table(
    name = "favorite_counselors",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "counselor_id"]),
    ],
)
class FavoriteCounselor(
    // LAZY 로딩: 실제로 user 객체가 필요할 때만 DB에서 조회
    // 즐겨찾기 목록만 볼 때 user 정보 불필요하면 성능 향상
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    // LAZY 로딩: counselor 정보도 필요할 때만 조회
    // EAGER로 하면 매번 JOIN 발생해서 성능 저하
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    val counselor: Counselor,
) : BaseEntity()
