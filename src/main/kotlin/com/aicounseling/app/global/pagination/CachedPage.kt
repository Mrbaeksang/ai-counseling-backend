package com.aicounseling.app.global.pagination

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

/**
 * 캐시 계층에서 PageImpl 대신 직렬화하기 쉬운 구조로 사용하기 위한 DTO.
 */
data class CachedPage<T>(
    val content: List<T>,
    val totalElements: Long,
) {
    fun toPage(pageable: Pageable): Page<T> = PageImpl(content, pageable, totalElements)

    companion object {
        fun <T> from(page: Page<T>): CachedPage<T> =
            CachedPage(
                content = page.content,
                totalElements = page.totalElements,
            )
    }
}
