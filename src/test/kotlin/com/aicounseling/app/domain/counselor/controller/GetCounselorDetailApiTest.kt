package com.aicounseling.app.domain.counselor.controller

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * GET /api/counselors/{id} API 테스트
 * 상담사 상세 정보 조회 기능 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("GET /api/counselors/{id} - 상담사 상세 정보 조회")
class GetCounselorDetailApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
        jwtTokenProvider: com.aicounseling.app.global.security.JwtTokenProvider,
        userRepository: com.aicounseling.app.domain.user.repository.UserRepository,
        counselorRepository: com.aicounseling.app.domain.counselor.repository.CounselorRepository,
        counselorRatingRepository: com.aicounseling.app.domain.counselor.repository.CounselorRatingRepository,
        favoriteCounselorRepository: com.aicounseling.app.domain.counselor.repository.FavoriteCounselorRepository,
        sessionRepository: com.aicounseling.app.domain.session.repository.ChatSessionRepository,
    ) : CounselorControllerBaseTest(
            mockMvc,
            objectMapper,
            jwtTokenProvider,
            userRepository,
            counselorRepository,
            counselorRatingRepository,
            favoriteCounselorRepository,
            sessionRepository,
        ) {
        @Test
        @DisplayName("성공: 활성 상담사의 상세 정보 조회")
        fun getCounselorDetail_withValidId_returnsDetail() {
            // given: 평가 데이터와 즐겨찾기 생성
            createTestSessionWithRating(testUser, testCounselor1, 8)
            createTestSessionWithRating(testUser2, testCounselor1, 10)
            createFavoriteCounselor(testUser, testCounselor1)

            // when & then
            mockMvc.perform(
                get("/api/counselors/${testCounselor1.id}")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("상담사 정보 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(testCounselor1.id))
                .andExpect(jsonPath("$.data.name").value("소크라테스"))
                .andExpect(jsonPath("$.data.title").value("고대 그리스의 철학자"))
                .andExpect(jsonPath("$.data.description").value("대화법과 산파술의 창시자"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/socrates.jpg"))
                .andExpect(jsonPath("$.data.totalRatings").value(2))
                .andExpect(jsonPath("$.data.averageRating").value(90))
                .andExpect(jsonPath("$.data.totalSessions").value(2))
                .andExpect(jsonPath("$.data.isFavorite").value(true))
        }

        @Test
        @DisplayName("성공: 즐겨찾기하지 않은 상담사 조회")
        fun getCounselorDetail_withoutFavorite_returnsFalseIsFavorite() {
            // when & then
            mockMvc.perform(
                get("/api/counselors/${testCounselor2.id}")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.name").value("공자"))
                .andExpect(jsonPath("$.data.isFavorite").value(false))
        }

        @Test
        @DisplayName("성공: 평가가 없는 상담사 조회")
        fun getCounselorDetail_withoutRatings_returnsZeroRating() {
            // when & then
            mockMvc.perform(
                get("/api/counselors/${testCounselor2.id}")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.averageRating").value(0))
                .andExpect(jsonPath("$.data.totalSessions").value(0))
        }

        @Test
        @DisplayName("실패: 존재하지 않는 상담사 ID")
        fun getCounselorDetail_withInvalidId_returns404() {
            // when & then
            mockMvc.perform(
                get("/api/counselors/99999")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-404"))
                .andExpect(jsonPath("$.msg").value("상담사를 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("실패: 비활성 상담사 조회")
        fun getCounselorDetail_withInactiveCounselor_returns404() {
            // when & then
            mockMvc.perform(
                get("/api/counselors/${testCounselor3.id}")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-404"))
                .andExpect(jsonPath("$.msg").value("상담사를 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("성공: 인증 없이도 조회 가능 (즐겨찾기 정보 없음)")
        fun getCounselorDetail_withoutAuth_returnsSuccessWithoutFavorite() {
            // when & then
            mockMvc.perform(
                get("/api/counselors/${testCounselor1.id}"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("상담사 정보 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(testCounselor1.id))
                .andExpect(jsonPath("$.data.isFavorite").value(false))
        }
    }
