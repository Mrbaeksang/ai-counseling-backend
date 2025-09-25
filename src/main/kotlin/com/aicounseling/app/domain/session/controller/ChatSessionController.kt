package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.character.dto.RateSessionRequest
import com.aicounseling.app.domain.session.dto.CreateSessionRequest
import com.aicounseling.app.domain.session.dto.CreateSessionResponse
import com.aicounseling.app.domain.session.dto.MessageItem
import com.aicounseling.app.domain.session.dto.SendMessageRequest
import com.aicounseling.app.domain.session.dto.SendMessageResponse
import com.aicounseling.app.domain.session.dto.SessionListResponse
import com.aicounseling.app.domain.session.dto.UpdateSessionTitleRequest
import com.aicounseling.app.domain.session.report.dto.MessageReportRequest
import com.aicounseling.app.domain.session.report.dto.MessageReportResponse
import com.aicounseling.app.domain.session.report.service.MessageReportService
import com.aicounseling.app.domain.session.service.ChatSessionService
import com.aicounseling.app.global.constants.AppConstants
import com.aicounseling.app.global.pagination.PageUtils
import com.aicounseling.app.global.pagination.PagedResponse
import com.aicounseling.app.global.rq.Rq
import com.aicounseling.app.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * ChatSessionController - 상담 세션 관련 API
 *
 * API 명세서 매핑 (8개 엔드포인트):
 * 1. GET /sessions - 세션 목록 조회
 * 2. POST /sessions - 새 세션 시작
 * 3. DELETE /sessions/{id} - 세션 종료
 * 4. GET /sessions/{id}/messages - 메시지 목록 조회
 * 5. POST /sessions/{id}/messages - 메시지 전송 (AI 응답 포함)
 * 6. POST /sessions/{id}/rate - 세션 평가
 * 7. PATCH /sessions/{id}/bookmark - 북마크 토글
 * 8. PATCH /sessions/{id}/title - 제목 수정
 */
@Tag(name = "sessions", description = "상담 세션 관련 API")
@RestController
@RequestMapping("/api/sessions")
class ChatSessionController(
    private val sessionService: ChatSessionService,
    private val messageReportService: MessageReportService,
    private val rq: Rq,
) {
    /**
     * 1. GET /sessions - 내 상담 세션 목록 조회
     */
    @Operation(summary = "내 상담 세션 목록 조회")
    @GetMapping
    fun getUserSessions(
        @RequestParam(required = false) bookmarked: Boolean?,
        @RequestParam(required = false) isClosed: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<PagedResponse<SessionListResponse>> {
        // 인증 확인
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        // PageUtils를 사용하여 페이지 요청 생성 (자동 검증 포함)
        val pageable =
            PageUtils.createPageRequest(
                page = page,
                size = size,
                sort = Sort.by(Sort.Direction.DESC, "lastMessageAt"),
            )
        // N+1 문제가 이미 해결된 getUserSessions 호출
        val sessionPage = sessionService.getUserSessions(userId, bookmarked, isClosed, pageable)

        // PagedResponse로 변환 (이미 SessionListResponse로 매핑됨)
        val response = PagedResponse.from(sessionPage)

        return RsData.of(
            "S-1",
            if (bookmarked == true) "북마크된 세션 조회 성공" else "세션 목록 조회 성공",
            response,
        )
    }

    /**
     * 2. POST /sessions - 새 상담 세션 시작
     */
    @Operation(summary = "새 상담 세션 시작")
    @PostMapping
    fun startSession(
        @Valid @RequestBody request: CreateSessionRequest,
    ): RsData<CreateSessionResponse> {
        // 인증 확인
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val response = sessionService.startSession(userId, request.characterId)

        return RsData.of(
            "S-1",
            "세션 시작 성공",
            response,
        )
    }

    /**
     * 3. DELETE /sessions/{id} - 세션 종료
     */
    @Operation(summary = "세션 종료")
    @DeleteMapping("/{sessionId}")
    fun closeSession(
        @PathVariable sessionId: Long,
    ): RsData<Unit> {
        // 인증 확인
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        sessionService.closeSession(userId, sessionId)

        return RsData.of(
            "S-1",
            "세션 종료 성공",
            null,
        )
    }

    /**
     * 4. GET /sessions/{id}/messages - 세션 메시지 목록 조회
     */
    @Operation(summary = "세션 메시지 목록 조회")
    @GetMapping("/{sessionId}/messages")
    fun getSessionMessages(
        @PathVariable sessionId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<PagedResponse<MessageItem>> {
        // 인증 확인
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        // PageUtils를 사용하여 페이지 요청 생성 (자동 검증 포함)
        val pageable =
            PageUtils.createPageRequest(
                page = page,
                size = size,
                sort = Sort.by(Sort.Direction.ASC, "createdAt"),
            )
        // 수정된 getSessionMessages 호출 (이제 Page<MessageItem> 반환)
        val messagePage = sessionService.getSessionMessages(userId, sessionId, pageable)

        // PagedResponse로 변환 (이미 MessageItem으로 매핑됨)
        val response = PagedResponse.from(messagePage)

        return RsData.of(
            "S-1",
            "메시지 조회 성공",
            response,
        )
    }

    /**
     * 5. POST /sessions/{sessionId}/messages/{messageId}/report - 메시지 신고
     */
    @Operation(summary = "메시지 신고")
    @PostMapping("/{sessionId}/messages/{messageId}/report")
    fun reportMessage(
        @PathVariable sessionId: Long,
        @PathVariable messageId: Long,
        @Valid @RequestBody request: MessageReportRequest,
    ): RsData<MessageReportResponse> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val report = messageReportService.submitReport(userId, sessionId, messageId, request)

        return RsData.of(
            "S-1",
            "메시지 신고가 접수되었습니다",
            report,
        )
    }

    /**
     * 6. POST /sessions/{id}/messages - 메시지 전송 (AI 응답 포함)
     */
    @Operation(summary = "메시지 전송 (AI 응답 포함)")
    @PostMapping("/{sessionId}/messages")
    fun sendMessage(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: SendMessageRequest,
    ): RsData<SendMessageResponse> {
        // 인증 확인
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val (userMessage, aiMessage, session) =
            sessionService.sendMessage(
                userId = userId,
                sessionId = sessionId,
                content = request.content,
            )

        return RsData.of(
            "S-1",
            "메시지 전송 성공",
            SendMessageResponse(
                userMessageId = userMessage.id,
                userMessage = userMessage.content,
                aiMessageId = aiMessage.id,
                aiMessage = aiMessage.content,
                sessionTitle = if (session.title != AppConstants.Session.DEFAULT_SESSION_TITLE) session.title else null,
                isSessionEnded = session.closedAt != null,
            ),
        )
    }

    /**
     * 6. POST /sessions/{id}/rate - 세션 평가
     */
    @Operation(summary = "세션 평가")
    @PostMapping("/{sessionId}/rate")
    fun rateSession(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: RateSessionRequest,
    ): RsData<String> {
        // 인증 확인
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val result =
            sessionService.rateSession(
                userId = userId,
                sessionId = sessionId,
                request = request,
            )

        // Service에서 이미 RsData를 반환하므로 그대로 반환
        return result
    }

    /**
     * 7. PATCH /sessions/{id}/bookmark - 세션 북마크 토글
     */
    @Operation(summary = "세션 북마크 토글")
    @PatchMapping("/{sessionId}/bookmark")
    fun toggleBookmark(
        @PathVariable sessionId: Long,
    ): RsData<Map<String, Any>> {
        // 인증 확인
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val isBookmarked = sessionService.toggleBookmark(userId, sessionId)

        return RsData.of(
            "S-1",
            if (isBookmarked) "북마크 추가 성공" else "북마크 제거 성공",
            mapOf(
                "sessionId" to sessionId,
                "isBookmarked" to isBookmarked,
            ),
        )
    }

    /**
     * 8. PATCH /sessions/{id}/title - 세션 제목 수정
     */
    @Operation(summary = "세션 제목 수정")
    @PatchMapping("/{sessionId}/title")
    fun updateSessionTitle(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: UpdateSessionTitleRequest,
    ): RsData<Any?> {
        // 인증 확인
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        sessionService.updateSessionTitle(userId, sessionId, request.title)

        return RsData.of(
            "S-1",
            "세션 제목 변경 성공",
            null,
        )
    }
}
