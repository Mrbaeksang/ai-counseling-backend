package com.aicounseling.app.domain.counselor.repository

import com.aicounseling.app.domain.counselor.dto.FavoriteCounselorResponse
import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.counselor.entity.FavoriteCounselor
import com.aicounseling.app.domain.user.entity.User
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import kotlin.math.roundToInt

/**
 * FavoriteCounselor 커스텀 레포지토리 구현체
 *
 * JDSL을 사용하여 즐겨찾기 상담사 목록을 평균 평점과 함께 조회
 */
class FavoriteCounselorRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : FavoriteCounselorRepositoryCustom {
    override fun findFavoritesWithRating(
        userId: Long,
        pageable: Pageable,
    ): Page<FavoriteCounselorResponse> {
        // selectNew 패턴으로 임시 결과 생성
        val result =
            kotlinJdslJpqlExecutor.findPage(pageable) {
                selectNew<TempFavoriteCounselorResult>(
                    path(Counselor::id),
                    path(Counselor::name),
                    path(Counselor::title),
                    path(Counselor::avatarUrl),
                    // 평균 평점 (Double로 받음)
                    coalesce(
                        avg(path(CounselorRating::rating)),
                        value(0.0),
                    ),
                ).from(
                    entity(FavoriteCounselor::class),
                    join(User::class).on(
                        path(FavoriteCounselor::user).eq(entity(User::class)),
                    ),
                    join(Counselor::class).on(
                        path(FavoriteCounselor::counselor).eq(entity(Counselor::class)),
                    ),
                    leftJoin(CounselorRating::class).on(
                        path(CounselorRating::counselor).eq(entity(Counselor::class)),
                    ),
                ).where(
                    and(
                        path(User::id).eq(userId),
                        path(Counselor::isActive).eq(true),
                    ),
                ).groupBy(
                    path(Counselor::id),
                    path(Counselor::name),
                    path(Counselor::title),
                    path(Counselor::avatarUrl),
                    path(FavoriteCounselor::createdAt),
                ).orderBy(
                    path(FavoriteCounselor::createdAt).desc(),
                )
            }

        // 임시 결과를 최종 DTO로 변환 (타입 변환 처리)
        return result.map { temp ->
            requireNotNull(temp) { "TempFavoriteCounselorResult should not be null" }
            FavoriteCounselorResponse(
                id = temp.id,
                name = temp.name,
                title = temp.title,
                avatarUrl = temp.avatarUrl,
                // 평점을 Int로 변환 (소수점 첫째 자리까지 표현)
                // 예: 4.3 → 43 (UI에서 4.3으로 표시)
                averageRating = (temp.averageRating * 10).roundToInt(),
            )
        }
    }
}

/**
 * JDSL selectNew용 임시 결과 클래스
 * Double 타입으로 받아서 나중에 Int로 변환
 */
data class TempFavoriteCounselorResult(
    val id: Long,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val averageRating: Double,
)
