package com.aicounseling.app.domain.counselor.repository

import com.aicounseling.app.domain.counselor.dto.CounselorDetailResponse
import com.aicounseling.app.domain.counselor.dto.CounselorListResponse
import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.session.entity.ChatSession
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import kotlin.math.roundToInt

/**
 * Counselor 커스텀 레포지토리 구현체
 *
 * JDSL을 사용하여 복잡한 쿼리를 타입 세이프하게 구현
 * N+1 문제를 방지하기 위해 LEFT JOIN 활용
 *
 * @Repository 어노테이션 제거 (Spring이 자동으로 Impl 클래스 인식)
 */
class CounselorRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : CounselorRepositoryCustom {
    override fun findCounselorsWithStats(
        sort: String,
        pageable: Pageable,
    ): Page<CounselorListResponse> {
        // 메모리 정렬 방식으로 변경 (JDSL의 GROUP BY + ORDER BY 제약으로 인해)
        // 1단계: 활성 상담사 전체 조회
        val counselors =
            kotlinJdslJpqlExecutor.findAll {
                select(entity(Counselor::class))
                    .from(entity(Counselor::class))
                    .where(path(Counselor::isActive).eq(true))
            }

        // 2단계: 각 상담사의 통계 정보 계산 (N+1 쿼리 문제 있지만 현재 JDSL 제약상 불가피)
        val counselorStats =
            counselors.mapNotNull { counselor ->
                counselor?.let {
                    // 평균 평점 계산
                    val avgRatingResult =
                        kotlinJdslJpqlExecutor.findAll {
                            select(avg(path(CounselorRating::rating)))
                                .from(entity(CounselorRating::class))
                                .where(
                                    path(CounselorRating::counselor).path(Counselor::id).eq(it.id),
                                )
                        }
                    val avgRating = avgRatingResult.firstOrNull() as? Double ?: 0.0

                    // 세션 수 계산
                    val sessionCountResult =
                        kotlinJdslJpqlExecutor.findAll {
                            select(count(entity(ChatSession::class)))
                                .from(entity(ChatSession::class))
                                .where(path(ChatSession::counselorId).eq(it.id))
                        }
                    val sessionCount = sessionCountResult.firstOrNull() as? Long ?: 0L

                    CounselorWithStats(
                        counselor = it,
                        averageRating = avgRating,
                        totalSessions = sessionCount,
                    )
                }
            }

        // 3단계: 메모리에서 정렬 (sort 파라미터에 따라)
        val sortedStats =
            when (sort) {
                "rating" -> counselorStats.sortedByDescending { it.averageRating }
                "popular" -> counselorStats.sortedByDescending { it.totalSessions }
                "recent" -> counselorStats.sortedByDescending { it.counselor.createdAt }
                else -> counselorStats.sortedByDescending { it.counselor.createdAt } // 기본값: 최신순 (생성일 내림차순)
            }

        // 4단계: 페이징 처리
        val startIndex = pageable.pageNumber * pageable.pageSize
        val endIndex = minOf(startIndex + pageable.pageSize, sortedStats.size)
        val pagedContent =
            if (startIndex < sortedStats.size) {
                sortedStats.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

        // 5단계: DTO로 변환
        val dtoList =
            pagedContent.map { stats ->
                CounselorListResponse(
                    id = stats.counselor.id,
                    name = stats.counselor.name,
                    title = stats.counselor.title,
                    avatarUrl = stats.counselor.avatarUrl,
                    categories = stats.counselor.categories,
                    // 평점을 Int로 변환 (소수점 첫째 자리까지 표현)
                    // 예: 4.3 → 43 (UI에서 4.3으로 표시)
                    averageRating = (stats.averageRating * 10).roundToInt(),
                    totalSessions = stats.totalSessions.toInt(),
                )
            }

        // 6단계: Page 객체 생성
        return PageImpl(
            dtoList,
            pageable,
            sortedStats.size.toLong(),
        )
    }

    override fun findCounselorDetailById(counselorId: Long): CounselorDetailResponse? {
        // Counselor 조회
        val counselor =
            kotlinJdslJpqlExecutor.findAll {
                select(entity(Counselor::class))
                    .from(entity(Counselor::class))
                    .where(
                        and(
                            path(Counselor::id).eq(counselorId),
                            path(Counselor::isActive).eq(true),
                        ),
                    )
            }.firstOrNull() ?: return null

        // 세션 수 카운트
        val sessionCount =
            kotlinJdslJpqlExecutor.findAll {
                select(count(entity(ChatSession::class)))
                    .from(entity(ChatSession::class))
                    .where(
                        path(ChatSession::counselorId).eq(counselorId),
                    )
            }.firstOrNull() ?: 0L

        // 평균 평점 계산 - counselor 관계를 통해 접근
        val avgRating =
            kotlinJdslJpqlExecutor.findAll {
                select(avg(path(CounselorRating::rating)))
                    .from(entity(CounselorRating::class))
                    .where(
                        path(CounselorRating::counselor).path(Counselor::id).eq(counselorId),
                    )
            }.firstOrNull() ?: 0.0

        // 평점 수 카운트 - counselor 관계를 통해 접근
        val ratingCount =
            kotlinJdslJpqlExecutor.findAll {
                select(count(entity(CounselorRating::class)))
                    .from(entity(CounselorRating::class))
                    .where(
                        path(CounselorRating::counselor).path(Counselor::id).eq(counselorId),
                    )
            }.firstOrNull() ?: 0L

        return CounselorDetailResponse(
            id = counselor.id,
            name = counselor.name,
            title = counselor.title,
            description = counselor.description,
            avatarUrl = counselor.avatarUrl,
            // 평점을 Int로 변환 (소수점 첫째 자리까지 표현)
            // 예: 4.3 → 43 (UI에서 4.3으로 표시)
            averageRating = (avgRating * 10).roundToInt(),
            totalSessions = sessionCount.toInt(),
            totalRatings = ratingCount.toInt(),
            categories = counselor.categories,
        )
    }
}

/**
 * 상담사와 통계 정보를 함께 담는 내부 클래스
 */
private data class CounselorWithStats(
    val counselor: Counselor,
    val averageRating: Double,
    val totalSessions: Long,
)
