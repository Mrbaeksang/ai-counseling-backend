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
 * GET /api/counselors API 테스트
 * 상담사 목록 조회 기능 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("GET /api/counselors - 상담사 목록 조회")
class GetCounselorsApiTest
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
        @DisplayName("성공: 인증된 사용자가 활성 상담사 목록 조회")
        fun getCounselors_withAuth_returnsActiveCounselors() {
            // given: 평가 데이터 생성
            createTestSessionWithRating(testUser, testCounselor1, 8)
            createTestSessionWithRating(testUser2, testCounselor1, 10)
            createTestSessionWithRating(testUser, testCounselor2, 6)

            // when & then
            mockMvc.perform(
                get("/api/counselors")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("상담사 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(2)) // 니체는 비활성이라 제외
                // createdAt DESC 정렬이므로 공자(나중 생성)가 먼저, 소크라테스가 두 번째
                .andExpect(jsonPath("$.data.content[0].name").value("공자"))
                .andExpect(jsonPath("$.data.content[0].averageRating").value(60))
                .andExpect(jsonPath("$.data.content[0].totalSessions").value(1))
                .andExpect(jsonPath("$.data.content[1].name").value("소크라테스"))
                .andExpect(jsonPath("$.data.content[1].averageRating").value(90))
                .andExpect(jsonPath("$.data.content[1].totalSessions").value(2))
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(2))
        }

        @Test
        @DisplayName("성공: 페이징 파라미터 적용")
        fun getCounselors_withPaging_returnsPagedResult() {
            // when & then
            mockMvc.perform(
                get("/api/counselors")
                    .header("Authorization", "Bearer $authToken")
                    .param("page", "1")
                    .param("size", "1"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageInfo.pageSize").value(1))
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(2))
        }

        @Test
        @DisplayName("성공: 정렬 파라미터 적용")
        fun getCounselors_withSortParam_returnsSortedResult() {
            // when & then
            mockMvc.perform(
                get("/api/counselors")
                    .header("Authorization", "Bearer $authToken")
                    .param("sort", "popular"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.content").isArray)
        }

        @Test
        @DisplayName("성공: 평가 데이터가 없어도 상담사 목록 조회 가능")
        fun getCounselors_withoutRatings_returnsAllActiveCounselors() {
            // when & then
            mockMvc.perform(
                get("/api/counselors")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].averageRating").value(0))
                .andExpect(jsonPath("$.data.content[0].totalSessions").value(0))
        }

        @Test
        @DisplayName("성공: 인증 없이도 조회 가능")
        fun getCounselors_withoutAuth_returnsPublicData() {
            // when & then
            mockMvc.perform(
                get("/api/counselors"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("상담사 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray)
        }
    }
