package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.report.entity.MessageReportReason
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("POST /api/sessions/{sessionId}/messages/{messageId}/report - 메시지 신고")
class ReportMessageApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
        jwtTokenProvider: com.aicounseling.app.global.security.JwtTokenProvider,
        userRepository: com.aicounseling.app.domain.user.repository.UserRepository,
        characterRepository: com.aicounseling.app.domain.character.repository.CharacterRepository,
        sessionRepository: com.aicounseling.app.domain.session.repository.ChatSessionRepository,
        messageRepository: com.aicounseling.app.domain.session.repository.MessageRepository,
        messageReportRepository: com.aicounseling.app.domain.session.report.repository.MessageReportRepository,
    ) : ChatSessionControllerBaseTest(
            mockMvc,
            objectMapper,
            jwtTokenProvider,
            userRepository,
            characterRepository,
            sessionRepository,
            messageRepository,
            messageReportRepository,
        ) {
        @Test
        @DisplayName("사용자는 자신의 세션 메시지를 신고할 수 있다")
        fun `should submit message report`() {
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        characterId = testCharacter.id,
                        title = "신고 세션",
                    ),
                )

            val message =
                messageRepository.save(
                    Message(
                        session = session,
                        senderType = SenderType.AI,
                        content = "부적절한 응답",
                    ),
                )

            val request =
                mapOf(
                    "reasonCode" to MessageReportReason.SPAM.name,
                    "detail" to "스팸성 답변 같아요.",
                )

            mockMvc.perform(
                post("/api/sessions/${session.id}/messages/${message.id}/report")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("메시지 신고가 접수되었습니다"))
                .andExpect(jsonPath("$.data.reportId").exists())
                .andExpect(jsonPath("$.data.reasonCode").value("SPAM"))
                .andExpect(jsonPath("$.data.messageId").value(message.id.toInt()))
                .andExpect(jsonPath("$.data.sessionId").value(session.id.toInt()))
                .andExpect(jsonPath("$.data.detail").value("스팸성 답변 같아요."))

            val reports = messageReportRepository.findAll()
            assert(reports.size == 1)
            val report = reports.first()
            assert(report.reporterUserId == testUser.id)
            assert(report.message.id == message.id)
            assert(report.reasonCode == MessageReportReason.SPAM)
            assert(report.detail == "스팸성 답변 같아요.")
        }

        @Test
        @DisplayName("같은 메시지를 중복 신고하면 400 코드가 반환된다")
        fun `should reject duplicate report`() {
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        characterId = testCharacter.id,
                    ),
                )
            val message =
                messageRepository.save(
                    Message(
                        session = session,
                        senderType = SenderType.AI,
                        content = "중복 신고 대상",
                    ),
                )

            val requestJson =
                objectMapper.writeValueAsString(
                    mapOf(
                        "reasonCode" to MessageReportReason.HARASSMENT.name,
                        "detail" to "중복 신고 테스트",
                    ),
                )

            mockMvc.perform(
                post("/api/sessions/${session.id}/messages/${message.id}/report")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            )
                .andExpect(status().isOk)

            mockMvc.perform(
                post("/api/sessions/${session.id}/messages/${message.id}/report")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-400"))
                .andExpect(jsonPath("$.msg").value("이미 신고한 메시지입니다"))
        }

        @Test
        @DisplayName("다른 세션의 메시지를 신고하면 404 코드가 반환된다")
        fun `should return 404 when message does not belong to session`() {
            val mySession =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        characterId = testCharacter.id,
                    ),
                )

            val otherSession =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        characterId = testCharacter.id,
                    ),
                )

            val message =
                messageRepository.save(
                    Message(
                        session = otherSession,
                        senderType = SenderType.AI,
                        content = "다른 세션 메시지",
                    ),
                )

            mockMvc.perform(
                post("/api/sessions/${mySession.id}/messages/${message.id}/report")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "reasonCode" to MessageReportReason.OTHER.name,
                                "detail" to "세션 불일치",
                            ),
                        ),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-404"))
                .andExpect(jsonPath("$.msg").value("메시지를 찾을 수 없습니다"))
        }
    }
