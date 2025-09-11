package com.aicounseling.app.domain.session.repository

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.session.dto.SessionListResponse
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.global.constants.AppConstants
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * ChatSession 커스텀 레포지토리 구현체
 *
 * JDSL을 사용하여 N+1 문제를 해결하는 최적화된 쿼리 구현
 *
 * Spring Data JPA는 Impl 접미사를 가진 클래스를 자동으로 감지하여 Custom Repository 구현체로 인식
 * @Repository 어노테이션은 제거해야 함 (중복 Bean 등록 방지)
 */
class ChatSessionRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : ChatSessionRepositoryCustom {
    override fun findSessionsWithCounselor(
        userId: Long,
        bookmarked: Boolean?,
        isClosed: Boolean?,
        pageable: Pageable,
    ): Page<SessionListResponse> {
        // JDSL의 selectNew를 사용하여 DTO로 직접 프로젝션 (타입 안정성 보장)
        val result =
            kotlinJdslJpqlExecutor.findPage(pageable) {
                selectNew<SessionListResponse>(
                    path(ChatSession::id),
                    path(ChatSession::counselorId),
                    coalesce(
                        path(ChatSession::title),
                        value(AppConstants.Session.DEFAULT_SESSION_TITLE),
                    ),
                    path(Counselor::name),
                    coalesce(
                        path(ChatSession::lastMessageAt),
                        path(ChatSession::createdAt),
                    ),
                    path(ChatSession::isBookmarked),
                    path(Counselor::avatarUrl),
                    path(ChatSession::closedAt),
                ).from(
                    entity(ChatSession::class),
                    join(Counselor::class).on(
                        path(ChatSession::counselorId).eq(path(Counselor::id)),
                    ),
                ).where(
                    and(
                        path(ChatSession::userId).eq(userId),
                        bookmarked?.let { path(ChatSession::isBookmarked).eq(it) },
                        isClosed?.let {
                            if (it) {
                                path(ChatSession::closedAt).isNotNull()
                            } else {
                                path(ChatSession::closedAt).isNull()
                            }
                        },
                    ),
                ).orderBy(
                    path(ChatSession::lastMessageAt).desc().nullsLast(),
                    path(ChatSession::createdAt).desc(),
                )
            }

        // INNER JOIN이므로 null이 없음이 보장됨 - nullable 타입을 non-nullable로 안전하게 변환
        return result.map { requireNotNull(it) { "SessionListResponse should not be null" } }
    }
}
