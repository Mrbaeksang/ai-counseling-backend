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
        cacheNames = ["character:list"],
        key = "T(java.lang.String).format('%s:%d:%d', #sort, #pageable.pageNumber, #pageable.pageSize)",
    )
    fun getCharacterPage(
        sort: String,
        pageable: Pageable,
    ): PagedResponse<CharacterListResponse> {
        val page = characterRepository.findCharactersWithStats(sort, pageable)
        return PagedResponse.from(page)
    }

    @Cacheable(cacheNames = ["character:detail"], key = "#characterId")
    fun getCharacterDetail(characterId: Long): CharacterDetailResponse? {
        return characterRepository.findCharacterDetailById(characterId)
    }
}
