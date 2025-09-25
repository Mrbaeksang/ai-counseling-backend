package com.aicounseling.app.domain.character.repository

import com.aicounseling.app.domain.character.dto.FavoriteCharacterResponse
import com.aicounseling.app.domain.character.entity.Character
import com.aicounseling.app.domain.character.entity.CharacterRating
import com.aicounseling.app.domain.character.entity.FavoriteCharacter
import com.aicounseling.app.domain.user.entity.User
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import kotlin.math.roundToInt

/**
 * FavoriteCharacter 커스텀 레포지토리 구현체
 *
 * JDSL을 사용하여 즐겨찾기 캐릭터 목록을 평균 평점과 함께 조회
 */
class FavoriteCharacterRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : FavoriteCharacterRepositoryCustom {
    override fun findFavoritesWithRating(
        userId: Long,
        pageable: Pageable,
    ): Page<FavoriteCharacterResponse> {
        // selectNew 패턴으로 임시 결과 생성
        val result =
            kotlinJdslJpqlExecutor.findPage(pageable) {
                selectNew<TempFavoriteCharacterResult>(
                    path(Character::id),
                    path(Character::name),
                    path(Character::title),
                    path(Character::avatarUrl),
                    // 평균 평점 (Double로 받음)
                    coalesce(
                        avg(path(CharacterRating::rating)),
                        value(0.0),
                    ),
                ).from(
                    entity(FavoriteCharacter::class),
                    join(User::class).on(
                        path(FavoriteCharacter::user).eq(entity(User::class)),
                    ),
                    join(Character::class).on(
                        path(FavoriteCharacter::character).eq(entity(Character::class)),
                    ),
                    leftJoin(CharacterRating::class).on(
                        path(CharacterRating::character).eq(entity(Character::class)),
                    ),
                ).where(
                    and(
                        path(User::id).eq(userId),
                        path(Character::isActive).eq(true),
                    ),
                ).groupBy(
                    path(Character::id),
                    path(Character::name),
                    path(Character::title),
                    path(Character::avatarUrl),
                    path(Character::createdAt),
                    path(FavoriteCharacter::createdAt),
                ).orderBy(
                    path(Character::createdAt).desc(),
                    path(FavoriteCharacter::createdAt).desc(),
                )
            }

        // 임시 결과를 최종 DTO로 변환 (타입 변환 처리)
        return result.map { temp ->
            requireNotNull(temp) { "TempFavoriteCharacterResult should not be null" }
            FavoriteCharacterResponse(
                id = temp.id,
                name = temp.name,
                title = temp.title,
                avatarUrl = temp.avatarUrl,
                // 즐겨찾기 목록은 1~10 스케일을 그대로 사용
                averageRating = temp.averageRating.roundToInt(),
            )
        }
    }
}

/**
 * JDSL selectNew용 임시 결과 클래스
 * Double 타입으로 받아서 나중에 Int로 변환
 */
data class TempFavoriteCharacterResult(
    val id: Long,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val averageRating: Double,
)
