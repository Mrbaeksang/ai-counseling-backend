package com.aicounseling.app.domain.character.entity

import com.aicounseling.app.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Character 엔티티 - AI 철학 캐릭터 (순수 데이터만)
 * 비즈니스 로직은 CharacterService로 이동
 *
 * ERD 기준:
 * - name: 캐릭터 이름 (예: "소크라테스")
 * - title: 직함 (예: "고대 그리스 철학자")
 * - description: 캐릭터 소개
 * - basePrompt: AI 프롬프트 (성격 특성 포함)
 * - avatarUrl: 프로필 이미지 URL
 * - isActive: 활성화 상태
 */
@Entity
@Table(
    name = "characters",
    indexes = [
        Index(name = "idx_character_active", columnList = "is_active"),
        Index(name = "idx_character_name", columnList = "name"),
    ],
)
class Character(
    @Column(nullable = false, length = 50)
    val name: String,
    @Column(nullable = false, length = 100)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Column(name = "base_prompt", nullable = false, columnDefinition = "TEXT")
    val basePrompt: String,
    @Column(name = "avatar_url", length = 500)
    val avatarUrl: String? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "categories", length = 500)
    val categories: String? = null,
) : BaseEntity()
