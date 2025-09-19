package com.aicounseling.app.domain.counselor.service

import com.aicounseling.app.domain.counselor.dto.CounselorDetailResponse
import com.aicounseling.app.domain.counselor.dto.CounselorListResponse
import com.aicounseling.app.domain.counselor.dto.FavoriteCounselorResponse
import com.aicounseling.app.domain.counselor.dto.RateSessionRequest
import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.counselor.entity.FavoriteCounselor
import com.aicounseling.app.domain.counselor.repository.CounselorRatingRepository
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.counselor.repository.FavoriteCounselorRepository
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.pagination.PagedResponse
import com.aicounseling.app.global.rsData.RsData
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CounselorService - 상담사 관련 비즈니스 로직
 *
 * 주요 기능:
 * - 상담사 목록 조회 (정렬, 페이징)
 * - 상담사 상세 정보 조회
 * - 즐겨찾기 관리
 */
@Service
@Transactional(readOnly = true)
class CounselorService(
    private val counselorRepository: CounselorRepository,
    private val favoriteCounselorRepository: FavoriteCounselorRepository,
    private val counselorRatingRepository: CounselorRatingRepository,
    private val userRepository: UserRepository,
) {
    /**
     * 상담사 목록 조회
     * GET /counselors?sort={sort}
     */
    fun getCounselors(
        sort: String?,
        pageable: Pageable,
        userId: Long? = null,
    ): RsData<PagedResponse<CounselorListResponse>> {
        // 정렬 옵션 검증 (popular, rating, recent)
        val validSorts = listOf("popular", "rating", "recent")
        val finalSort: String = if (!sort.isNullOrEmpty() && sort in validSorts) sort else "recent"

        val counselors = counselorRepository.findCounselorsWithStats(finalSort, pageable)

        val counselorsWithFavorite =
            if (userId != null) {
                counselors.map { counselor ->
                    val isFavorite = favoriteCounselorRepository.existsByUserIdAndCounselorId(userId, counselor.id)
                    counselor.copy(isFavorite = isFavorite)
                }
            } else {
                counselors
            }

        val pagedResponse = PagedResponse.from(counselorsWithFavorite)

        return RsData.of(
            "S-1",
            "상담사 목록 조회 성공",
            pagedResponse,
        )
    }

    /**
     * 상담사 상세 정보 조회
     * GET /counselors/{id}
     */
    fun getCounselorDetail(
        counselorId: Long,
        userId: Long?,
    ): RsData<CounselorDetailResponse> {
        val counselor =
            counselorRepository.findCounselorDetailById(counselorId)
                ?: return RsData.of(
                    "F-404",
                    "상담사를 찾을 수 없습니다",
                    null,
                )

        // 사용자가 로그인한 경우 즐겨찾기 여부 확인
        val counselorWithFavorite =
            if (userId != null) {
                val isFavorite = favoriteCounselorRepository.existsByUserIdAndCounselorId(userId, counselorId)
                counselor.copy(isFavorite = isFavorite)
            } else {
                counselor
            }

        return RsData.of(
            "S-1",
            "상담사 정보 조회 성공",
            counselorWithFavorite,
        )
    }

    /**
     * 즐겨찾기 상담사 목록 조회
     * GET /counselors/favorites
     */
    fun getFavoriteCounselors(
        userId: Long,
        pageable: Pageable,
    ): RsData<PagedResponse<FavoriteCounselorResponse>> {
        val favorites = favoriteCounselorRepository.findFavoritesWithRating(userId, pageable)

        val pagedResponse = PagedResponse.from(favorites)

        return RsData.of(
            "S-1",
            "즐겨찾기 목록 조회 성공",
            pagedResponse,
        )
    }

    /**
     * 상담사 즐겨찾기 추가
     * POST /counselors/{id}/favorite
     */
    @Transactional
    fun addFavorite(
        userId: Long,
        counselorId: Long,
    ): RsData<String> {
        // 사용자 조회
        val user =
            userRepository.findById(userId).orElse(null)
                ?: return RsData.of("F-404", "사용자를 찾을 수 없습니다", null)

        // 상담사 조회 (활성 상태 체크 필요)
        val counselor =
            counselorRepository.findById(counselorId).orElse(null)
                ?: return RsData.of("F-404", "상담사를 찾을 수 없습니다", null)

        if (!counselor.isActive) {
            return RsData.of("F-400", "비활성화된 상담사입니다", null)
        }

        // 이미 즐겨찾기인지 확인
        val exists = favoriteCounselorRepository.existsByUserAndCounselor(user, counselor)
        if (exists) {
            return RsData.of("F-409", "이미 즐겨찾기한 상담사입니다", null)
        }

        // 즐겨찾기 추가
        val favorite =
            FavoriteCounselor(
                user = user,
                counselor = counselor,
            )
        favoriteCounselorRepository.save(favorite)

        return RsData.of(
            "S-1",
            "즐겨찾기가 추가되었습니다",
            "즐겨찾기 추가 성공",
        )
    }

    /**
     * 상담사 즐겨찾기 제거
     * DELETE /counselors/{id}/favorite
     */
    @Transactional
    fun removeFavorite(
        userId: Long,
        counselorId: Long,
    ): RsData<String> {
        // 사용자 조회
        val user =
            userRepository.findById(userId).orElse(null)
                ?: return RsData.of("F-404", "사용자를 찾을 수 없습니다", null)

        // 상담사 조회
        val counselor =
            counselorRepository.findById(counselorId).orElse(null)
                ?: return RsData.of("F-404", "상담사를 찾을 수 없습니다", null)

        // 비활성 상담사 체크
        if (!counselor.isActive) {
            return RsData.of("F-400", "비활성 상담사입니다", null)
        }

        // 즐겨찾기 존재 확인
        val exists = favoriteCounselorRepository.existsByUserAndCounselor(user, counselor)
        if (!exists) {
            return RsData.of("F-404", "즐겨찾기하지 않은 상담사입니다", null)
        }

        // 즐겨찾기 해제
        favoriteCounselorRepository.deleteByUserAndCounselor(user, counselor)

        return RsData.of(
            "S-1",
            "즐겨찾기가 해제되었습니다",
            "즐겨찾기 제거 성공",
        )
    }

    /**
     * 상담사 엔티티 조회 (내부 사용)
     * ChatSessionService에서 사용
     */
    fun findById(counselorId: Long): Counselor? {
        return counselorRepository.findById(counselorId)
            .filter { it.isActive }
            .orElse(null)
    }

    /**
     * 세션 평가 추가
     * ChatSessionService에서 호출
     *
     * @param sessionId 세션 ID
     * @param userId 사용자 ID
     * @param counselorId 상담사 ID
     * @param request 평가 요청 (rating, feedback)
     * @return 평가 결과
     */
    @Transactional
    fun addRating(
        sessionId: Long,
        userId: Long,
        counselorId: Long,
        session: ChatSession,
        request: RateSessionRequest,
    ): RsData<String> {
        // 이미 평가했는지 확인
        if (counselorRatingRepository.existsBySessionId(sessionId)) {
            return RsData.of("F-400", "이미 평가한 세션입니다", null)
        }

        // 사용자 조회
        val user =
            userRepository.findById(userId).orElse(null)
                ?: return RsData.of("F-404", "사용자를 찾을 수 없습니다", null)

        // 상담사 조회
        val counselor =
            counselorRepository.findById(counselorId).orElse(null)
                ?: return RsData.of("F-404", "상담사를 찾을 수 없습니다", null)

        // 평가 생성
        val rating =
            CounselorRating(
                user = user,
                counselor = counselor,
                session = session,
                rating = request.rating,
                review = request.feedback,
            )

        println("평점 저장: 세션ID=$sessionId, 상담사=${counselor.name}, 평점=${request.rating}")

        counselorRatingRepository.save(rating)

        return RsData.of(
            "S-1",
            "평가가 등록되었습니다",
            "평가 등록 성공",
        )
    }

    /**
     * 세션이 이미 평가되었는지 확인
     *
     * @param sessionId 세션 ID
     * @return 평가 여부
     */
    fun isSessionRated(sessionId: Long): Boolean {
        return counselorRatingRepository.existsBySessionId(sessionId)
    }
}
