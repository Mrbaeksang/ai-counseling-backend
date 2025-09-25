package com.aicounseling.app.global.controller

import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/debug")
@Profile("dev") // 개발 환경에서만 활성화
@Tag(name = "Debug", description = "디버그용 API (개발 환경 전용)")
class DebugController(
    private val sessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
) {
    companion object {
        private const val CONTENT_PREVIEW_LENGTH = 100
        private const val MESSAGE_PREVIEW_LENGTH = 50
        private const val RECENT_SESSIONS_LIMIT = 10
    }

    data class SessionDebugInfo(
        val sessionId: Long,
        val title: String?,
        val isBookmarked: Boolean,
        val closedAt: String?,
        val messageCount: Int,
        val messages: List<MessageDebugInfo>,
    )

    data class MessageDebugInfo(
        val id: Long,
        val senderType: String,
        // 처음 100자만
        val contentPreview: String,
        val contentLength: Int,
        val createdAt: String,
    )

    @GetMapping("/sessions/{sessionId}/details")
    @Operation(
        summary = "세션 디버그 정보 조회",
        description = "특정 세션의 메시지 목록과 메타데이터를 디버그 용도로 확인합니다",
    )
    fun getSessionDetails(
        @PathVariable sessionId: Long,
    ): RsData<SessionDebugInfo> {
        val session =
            sessionRepository.findById(sessionId)
                .orElseThrow { IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId") }

        val messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)

        val messageInfos =
            messages.map { msg ->
                MessageDebugInfo(
                    id = msg.id,
                    senderType = msg.senderType.name,
                    contentPreview =
                        if (msg.content.length > CONTENT_PREVIEW_LENGTH) {
                            msg.content.substring(0, CONTENT_PREVIEW_LENGTH) + "..."
                        } else {
                            msg.content
                        },
                    contentLength = msg.content.length,
                    createdAt = msg.createdAt.toString(),
                )
            }

        val debugInfo =
            SessionDebugInfo(
                sessionId = session.id,
                title = session.title,
                isBookmarked = session.isBookmarked,
                closedAt = session.closedAt?.toString(),
                messageCount = messages.size,
                messages = messageInfos,
            )

        return RsData.of("S-1", "세션 디버그 정보 조회 성공", debugInfo)
    }
    @GetMapping("/sessions/recent")
    @Operation(
        summary = "최근 세션 목록 조회",
        description = "최근 생성된 세션 10개의 간단한 정보를 조회합니다",
    )
    fun getRecentSessions(): RsData<List<SessionSummary>> {
        val sessions =
            sessionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .take(RECENT_SESSIONS_LIMIT)
                .map { session ->
                    val messageCount = messageRepository.countBySessionId(session.id)
                    val messages = messageRepository.findBySessionIdOrderByCreatedAtDesc(session.id)
                    val lastMessage = messages.firstOrNull()

                    SessionSummary(
                        sessionId = session.id,
                        title = session.title ?: "제목 없음",
                        messageCount = messageCount.toInt(),
                        createdAt = session.createdAt.toString(),
                        lastMessageAt = session.lastMessageAt?.toString(),
                    )
                }

        return RsData.of("S-1", "최근 세션 목록 조회 성공", sessions)
    }

    data class SessionSummary(
        val sessionId: Long,
        val title: String,
        val messageCount: Int,
        val createdAt: String,
        val lastMessageAt: String?,
    )
}
