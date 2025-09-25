package com.aicounseling.app.domain.character.repository

import com.aicounseling.app.domain.character.dto.CharacterDetailResponse
import com.aicounseling.app.domain.character.dto.CharacterListResponse
import com.aicounseling.app.domain.character.entity.Character
import com.aicounseling.app.domain.character.entity.CharacterRating
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
class CharacterRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : CharacterRepositoryCustom {
    override fun findCharactersWithStats(
        sort: String,
        pageable: Pageable,
    ): Page<CharacterListResponse> {
        // 메모리 정렬 방식으로 변경 (JDSL의 GROUP BY + ORDER BY 제약으로 인해)
        // 1단계: 활성 캐릭터 전체 조회
        val characters =
            kotlinJdslJpqlExecutor.findAll {
                select(entity(Character::class))
                    .from(entity(Character::class))
                    .where(path(Character::isActive).eq(true))
            }

        // 2단계: 각 캐릭터의 통계 정보 계산 (N+1 쿼리 문제 있지만 현재 JDSL 제약상 불가피)
        val counselorStats =
            characters.mapNotNull { counselor ->
                counselor?.let {
                    // 평균 평점 계산
                    val avgRatingResult =
                        kotlinJdslJpqlExecutor.findAll {
                            select(avg(path(CharacterRating::rating)))
                                .from(entity(CharacterRating::class))
                                .where(
                                    path(CharacterRating::character).path(Character::id).eq(it.id),
                                )
                        }
                    val avgRating = avgRatingResult.firstOrNull() as? Double ?: 0.0

                    // 세션 수 계산
                    val sessionCountResult =
                        kotlinJdslJpqlExecutor.findAll {
                            select(count(entity(ChatSession::class)))
                                .from(entity(ChatSession::class))
                                .where(path(ChatSession::characterId).eq(it.id))
                        }
                    val sessionCount = sessionCountResult.firstOrNull() as? Long ?: 0L

                    CounselorWithStats(
                        character = it,
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
                "recent" -> counselorStats.sortedByDescending { it.character.createdAt }
                else -> counselorStats.sortedByDescending { it.character.createdAt } // 기본값: 최신순 (생성일 내림차순)
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
                CharacterListResponse(
                    id = stats.character.id,
                    name = stats.character.name,
                    title = stats.character.title,
                    avatarUrl = stats.character.avatarUrl,
                    categories = stats.character.categories,
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

    override fun findCharacterDetailById(characterId: Long): CharacterDetailResponse? {
        // Counselor 조회
        val character =
            kotlinJdslJpqlExecutor.findAll {
                select(entity(Character::class))
                    .from(entity(Character::class))
                    .where(
                        and(
                            path(Character::id).eq(characterId),
                            path(Character::isActive).eq(true),
                        ),
                    )
            }.firstOrNull() ?: return null

        // 세션 수 카운트
        val sessionCount =
            kotlinJdslJpqlExecutor.findAll {
                select(count(entity(ChatSession::class)))
                    .from(entity(ChatSession::class))
                    .where(
                        path(ChatSession::characterId).eq(characterId),
                    )
            }.firstOrNull() ?: 0L

        // 평균 평점 계산 - counselor 관계를 통해 접근
        val avgRating =
            kotlinJdslJpqlExecutor.findAll {
                select(avg(path(CharacterRating::rating)))
                    .from(entity(CharacterRating::class))
                    .where(
                        path(CharacterRating::character).path(Character::id).eq(characterId),
                    )
            }.firstOrNull() ?: 0.0

        // 평점 수 카운트 - counselor 관계를 통해 접근
        val ratingCount =
            kotlinJdslJpqlExecutor.findAll {
                select(count(entity(CharacterRating::class)))
                    .from(entity(CharacterRating::class))
                    .where(
                        path(CharacterRating::character).path(Character::id).eq(characterId),
                    )
            }.firstOrNull() ?: 0L

        return CharacterDetailResponse(
            id = character.id,
            name = character.name,
            title = character.title,
            description = character.description,
            avatarUrl = character.avatarUrl,
            // 평점을 Int로 변환 (소수점 첫째 자리까지 표현)
            // 예: 4.3 → 43 (UI에서 4.3으로 표시)
            averageRating = (avgRating * 10).roundToInt(),
            totalSessions = sessionCount.toInt(),
            totalRatings = ratingCount.toInt(),
            categories = character.categories,
        )
    }
}

/**
 * 캐릭터와 통계 정보를 함께 담는 내부 클래스
 */
private data class CounselorWithStats(
    val character: Character,
    val averageRating: Double,
    val totalSessions: Long,
)
