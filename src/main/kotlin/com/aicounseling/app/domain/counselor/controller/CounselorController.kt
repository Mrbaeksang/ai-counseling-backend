package com.aicounseling.app.domain.counselor.controller

import com.aicounseling.app.domain.counselor.dto.CounselorDetailResponse
import com.aicounseling.app.domain.counselor.dto.CounselorListResponse
import com.aicounseling.app.domain.counselor.dto.FavoriteCounselorResponse
import com.aicounseling.app.domain.counselor.service.CounselorService
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
@RequestMapping("/api/counselors")
class CounselorController(
    private val counselorService: CounselorService,
    private val rq: Rq,
) {
    @GetMapping
    fun getCounselors(
        @RequestParam(defaultValue = "recent") sort: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<PagedResponse<CounselorListResponse>> {
        val pageable = PageRequest.of(page - 1, size)
        val userId = rq.currentUserId
        return counselorService.getCounselors(sort, pageable, userId)
    }

    @GetMapping("/{id}")
    fun getCounselorDetail(
        @PathVariable id: Long,
    ): RsData<CounselorDetailResponse> {
        val userId = rq.currentUserId
        return counselorService.getCounselorDetail(id, userId)
    }

    @GetMapping("/favorites")
    fun getFavoriteCounselors(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<PagedResponse<FavoriteCounselorResponse>> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val pageable =
            PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"),
            )
        return counselorService.getFavoriteCounselors(userId, pageable)
    }

    @PostMapping("/{id}/favorite")
    fun addFavorite(
        @PathVariable id: Long,
    ): RsData<String> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        return counselorService.addFavorite(userId, id)
    }

    @DeleteMapping("/{id}/favorite")
    fun removeFavorite(
        @PathVariable id: Long,
    ): RsData<String> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        return counselorService.removeFavorite(userId, id)
    }
}
