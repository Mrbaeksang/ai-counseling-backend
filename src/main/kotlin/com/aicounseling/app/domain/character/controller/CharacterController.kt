package com.aicounseling.app.domain.character.controller

import com.aicounseling.app.domain.character.dto.CharacterDetailResponse
import com.aicounseling.app.domain.character.dto.CharacterListResponse
import com.aicounseling.app.domain.character.dto.FavoriteCharacterResponse
import com.aicounseling.app.domain.character.service.CharacterService
import com.aicounseling.app.global.pagination.PagedResponse
import com.aicounseling.app.global.rq.Rq
import com.aicounseling.app.global.rsData.RsData
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/characters")
class CharacterController(
    private val characterService: CharacterService,
    private val rq: Rq,
) {
    @GetMapping
    fun getCharacters(
        @RequestParam(defaultValue = "recent") sort: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<PagedResponse<CharacterListResponse>> {
        val pageable = PageRequest.of(page - 1, size)
        val userId = rq.currentUserId
        return characterService.getCharacters(sort, pageable, userId)
    }

    @GetMapping("/{id}")
    fun getCharacterDetail(
        @PathVariable id: Long,
    ): RsData<CharacterDetailResponse> {
        val userId = rq.currentUserId
        return characterService.getCharacterDetail(id, userId)
    }

    @GetMapping("/favorites")
    fun getFavoriteCharacters(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<PagedResponse<FavoriteCharacterResponse>> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val pageable =
            PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"),
            )
        return characterService.getFavoriteCharacters(userId, pageable)
    }

    @PostMapping("/{id}/favorite")
    fun addFavorite(
        @PathVariable id: Long,
    ): RsData<String> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        return characterService.addFavorite(userId, id)
    }

    @DeleteMapping("/{id}/favorite")
    fun removeFavorite(
        @PathVariable id: Long,
    ): RsData<String> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        return characterService.removeFavorite(userId, id)
    }
}
