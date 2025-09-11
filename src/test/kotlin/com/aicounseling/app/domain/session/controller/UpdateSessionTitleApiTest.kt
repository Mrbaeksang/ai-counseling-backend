package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.session.dto.UpdateSessionTitleRequest
import com.aicounseling.app.domain.session.entity.ChatSession
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * PATCH /api/sessions/{id}/title - 세션 제목 수정 API 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PATCH /api/sessions/{id}/title - 세션 제목 수정")
class UpdateSessionTitleApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
        jwtTokenProvider: com.aicounseling.app.global.security.JwtTokenProvider,
        userRepository: com.aicounseling.app.domain.user.repository.UserRepository,
        counselorRepository: com.aicounseling.app.domain.counselor.repository.CounselorRepository,
        sessionRepository: com.aicounseling.app.domain.session.repository.ChatSessionRepository,
        messageRepository: com.aicounseling.app.domain.session.repository.MessageRepository,
    ) : ChatSessionControllerBaseTest(
            mockMvc,
            objectMapper,
            jwtTokenProvider,
            userRepository,
            counselorRepository,
            sessionRepository,
            messageRepository,
        ) {
        companion object {
            private val dotenv =
                io.github.cdimascio.dotenv.dotenv {
                    ignoreIfMissing = true
                }

            @JvmStatic
            @org.springframework.test.context.DynamicPropertySource
            fun properties(registry: org.springframework.test.context.DynamicPropertyRegistry) {
                val apiKey =
                    System.getenv("OPENROUTER_API_KEY")
                        ?: dotenv["OPENROUTER_API_KEY"]
                        ?: "test-api-key"

                registry.add("openrouter.api-key") { apiKey }
                registry.add("jwt.secret") {
                    System.getenv("JWT_SECRET")
                        ?: dotenv["JWT_SECRET"]
                        ?: "test-jwt-secret-key-for-jwt-auth-512-bits-long-2025-with-extra-characters-for-security"
                }
            }
        }

        @Test
        @DisplayName("인증된 사용자가 자신의 세션 제목을 수정할 수 있다")
        fun `should update session title for authenticated user`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "원래 제목",
                    ),
                )

            // 15자 이내
            val request =
                UpdateSessionTitleRequest(
                    title = "니체의 초인 사상",
                )

            // When & Then
            mockMvc.perform(
                patch("/api/sessions/${session.id}/title")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("세션 제목 변경 성공"))
                // controller returns null data
                .andExpect(jsonPath("$.data").doesNotExist())

            // DB 확인
            val updatedSession = sessionRepository.findById(session.id).orElseThrow()
            assert(updatedSession.title == "니체의 초인 사상")
        }

        @Test
        @DisplayName("빈 제목으로 수정 요청 시 400 에러를 반환한다")
        fun `should return 400 for empty title`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "원래 제목",
                    ),
                )

            val request = mapOf("title" to "")

            // When & Then
            mockMvc.perform(
                patch("/api/sessions/${session.id}/title")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                // ResponseAspect disabled in test profile
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-400"))
        }

        @Test
        @DisplayName("15자를 초과하는 제목으로 수정 시 400 에러를 반환한다")
        fun `should return 400 for title exceeding 15 characters`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "원래 제목",
                    ),
                )

            // 다양한 길이의 테스트 케이스
            // 15자 - 통과해야 함, 16자 - 실패해야 함, 17자 - 실패해야 함
            val testCases =
                listOf(
                    "열다섯자정확히되는제목입니다요" to true,
                    "열여섯자가넘는매우긴제목입니다요" to false,
                    "열일곱자가넘는매우긴제목입니다요요" to false,
                )

            testCases.forEach { (title, shouldPass) ->
                println("Testing title: '$title' (길이: ${title.length}, shouldPass: $shouldPass)")

                val request = mapOf("title" to title)

                val result =
                    mockMvc.perform(
                        patch("/api/sessions/${session.id}/title")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    ).andReturn()

                val responseContent = result.response.contentAsString
                val responseJson = objectMapper.readTree(responseContent)
                val resultCode = responseJson.get("resultCode").asText()

                println("Response for '$title': resultCode=$resultCode")

                if (shouldPass) {
                    assert(resultCode == "S-1") {
                        "15자 이하는 성공해야 하는데 실패함: $title (길이: ${title.length})"
                    }
                } else {
                    assert(resultCode == "F-400") {
                        "15자 초과는 실패해야 하는데 성공함: $title (길이: ${title.length})"
                    }
                }
            }
        }

        @Test
        @DisplayName("다른 사용자의 세션 제목 수정 시 500 에러를 반환한다")
        fun `should return 500 when updating other user session title`() {
            // Given
            val otherUser =
                userRepository.save(
                    com.aicounseling.app.domain.user.entity.User(
                        email = "other@example.com",
                        nickname = "다른유저",
                        authProvider = com.aicounseling.app.global.security.AuthProvider.GOOGLE,
                        providerId = "google-other-id",
                    ),
                )

            val otherSession =
                sessionRepository.save(
                    ChatSession(
                        userId = otherUser.id,
                        counselorId = testCounselor.id,
                        title = "다른 사용자의 세션",
                    ),
                )

            val request = UpdateSessionTitleRequest(title = "해킹 시도")

            // When & Then
            mockMvc.perform(
                patch("/api/sessions/${otherSession.id}/title")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                // ResponseAspect disabled in test profile
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-500"))
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 에러를 반환한다")
        fun `should return 401 for unauthenticated request`() {
            // Given
            val session =
                sessionRepository.save(
                    ChatSession(
                        userId = testUser.id,
                        counselorId = testCounselor.id,
                        title = "원래 제목",
                    ),
                )

            val request = UpdateSessionTitleRequest(title = "새 제목")

            // When & Then
            mockMvc.perform(
                patch("/api/sessions/${session.id}/title")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다"))
        }
    }
