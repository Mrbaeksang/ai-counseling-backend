package com.aicounseling.app.domain.counselor.service

import com.aicounseling.app.domain.counselor.dto.CounselorDetailResponse
import com.aicounseling.app.domain.counselor.dto.CounselorListResponse
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.global.pagination.PagedResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class CounselorCacheService(
    private val counselorRepository: CounselorRepository,
) {
    @Cacheable(
        cacheNames = ["counselor:list"],
        key = "T(java.lang.String).format('%s:%d:%d', #sort, #pageable.pageNumber, #pageable.pageSize)",
    )
    fun getCounselorPage(
        sort: String,
        pageable: Pageable,
    ): PagedResponse<CounselorListResponse> {
        val page = counselorRepository.findCounselorsWithStats(sort, pageable)
        return PagedResponse.from(page)
    }

    @Cacheable(cacheNames = ["counselor:detail"], key = "#counselorId")
    fun getCounselorDetail(counselorId: Long): CounselorDetailResponse? {
        return counselorRepository.findCounselorDetailById(counselorId)
    }
}
