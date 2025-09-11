package com.aicounseling.app.global.pagination

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

object PageUtils {
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    const val MIN_PAGE_SIZE = 1

    fun createPageRequest(
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
        sort: Sort = Sort.unsorted(),
    ): PageRequest {
        val validatedSize = size.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
        val validatedPage = page.coerceAtLeast(0)
        return PageRequest.of(validatedPage, validatedSize, sort)
    }

    fun createPageRequest(
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
        sortBy: String,
        direction: Sort.Direction = Sort.Direction.DESC,
    ): PageRequest {
        val sort = Sort.by(direction, sortBy)
        return createPageRequest(page, size, sort)
    }

    fun <T> createEmptyPage(pageable: Pageable): Page<T> {
        return Page.empty(pageable)
    }

    fun <T> toPageInfo(page: Page<T>): PageInfo {
        return PageInfo.from(page)
    }
}

data class PageInfo(
    val currentPage: Int,
    val totalPages: Int,
    val pageSize: Int,
    val totalElements: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val isFirst: Boolean,
    val isLast: Boolean,
) {
    companion object {
        fun <T> from(page: Page<T>): PageInfo {
            return PageInfo(
                currentPage = page.number,
                totalPages = page.totalPages,
                pageSize = page.size,
                totalElements = page.totalElements,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious(),
                isFirst = page.isFirst,
                isLast = page.isLast,
            )
        }
    }
}

data class PagedResponse<T>(
    val content: List<T>,
    val pageInfo: PageInfo,
) {
    companion object {
        fun <T, R> from(
            page: Page<T>,
            mapper: (T) -> R,
        ): PagedResponse<R> {
            return PagedResponse(
                content = page.content.map(mapper),
                pageInfo = PageInfo.from(page),
            )
        }

        fun <T> from(page: Page<T>): PagedResponse<T> {
            return PagedResponse(
                content = page.content,
                pageInfo = PageInfo.from(page),
            )
        }
    }
}
