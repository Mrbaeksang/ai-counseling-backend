package com.aicounseling.app.domain.character.service

import com.aicounseling.app.domain.character.dto.CharacterDetailResponse
import com.aicounseling.app.domain.character.dto.CharacterListResponse
import com.aicounseling.app.domain.character.repository.CharacterRepository
import com.aicounseling.app.global.pagination.PagedResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class CharacterCacheService(
    private val characterRepository: CharacterRepository,
) {
    @Cacheable(
        cacheNames = ["counselor:list"],
        key = "T(java.lang.String).format('%s:%d:%d', #sort, #pageable.pageNumber, #pageable.pageSize)",
    )
    fun getCounselorPage(
        sort: String,
        pageable: Pageable,
    ): PagedResponse<CharacterListResponse> {
        val page = characterRepository.findCounselorsWithStats(sort, pageable)
        return PagedResponse.from(page)
    }

    @Cacheable(cacheNames = ["counselor:detail"], key = "#counselorId")
    fun getCounselorDetail(counselorId: Long): CharacterDetailResponse? {
        return characterRepository.findCounselorDetailById(counselorId)
    }
}
