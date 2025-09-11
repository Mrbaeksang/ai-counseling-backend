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
 * GET /api/counselors/favorites API 테스트
 * 즐겨찾기 상담사 목록 조회 기능 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("GET /api/counselors/favorites - 즐겨찾기 상담사 목록 조회")
class GetFavoriteCounselorsApiTest
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
        @DisplayName("성공: 즐겨찾기 상담사 목록 조회")
        fun getFavoriteCounselors_withFavorites_returnsList() {
            // given: 즐겨찾기와 평가 데이터 생성
            createFavoriteCounselor(testUser, testCounselor1)
            createFavoriteCounselor(testUser, testCounselor2)
            createTestSessionWithRating(testUser, testCounselor1, 9)
            createTestSessionWithRating(testUser2, testCounselor1, 7)
            createTestSessionWithRating(testUser, testCounselor2, 10)

            // when & then
            mockMvc.perform(
                get("/api/counselors/favorites")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("즐겨찾기 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(testCounselor2.id))
                .andExpect(jsonPath("$.data.content[0].name").value("공자"))
                .andExpect(jsonPath("$.data.content[0].title").value("동양 철학의 아버지"))
                .andExpect(jsonPath("$.data.content[0].avatarUrl").value("https://example.com/confucius.jpg"))
                .andExpect(jsonPath("$.data.content[0].averageRating").value(10))
                .andExpect(jsonPath("$.data.content[1].id").value(testCounselor1.id))
                .andExpect(jsonPath("$.data.content[1].name").value("소크라테스"))
                .andExpect(jsonPath("$.data.content[1].averageRating").value(8))
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(2))
        }

        @Test
        @DisplayName("성공: 즐겨찾기가 없는 경우 빈 목록 반환")
        fun getFavoriteCounselors_withoutFavorites_returnsEmpty() {
            // when & then
            mockMvc.perform(
                get("/api/counselors/favorites")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("즐겨찾기 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0))
        }

        @Test
        @DisplayName("성공: 페이징 파라미터 적용")
        fun getFavoriteCounselors_withPaging_returnsPagedResult() {
            // given: 여러 즐겨찾기 생성
            createFavoriteCounselor(testUser, testCounselor1)
            createFavoriteCounselor(testUser, testCounselor2)

            // when & then
            mockMvc.perform(
                get("/api/counselors/favorites")
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
        @DisplayName("성공: 다른 사용자의 즐겨찾기는 조회되지 않음")
        fun getFavoriteCounselors_withOtherUserFavorites_returnsOnlyOwn() {
            // given: 각 사용자별 즐겨찾기
            createFavoriteCounselor(testUser, testCounselor1)
            createFavoriteCounselor(testUser2, testCounselor2)

            // when & then: testUser는 자신의 즐겨찾기만 조회
            mockMvc.perform(
                get("/api/counselors/favorites")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("소크라테스"))

            // when & then: testUser2는 자신의 즐겨찾기만 조회
            mockMvc.perform(
                get("/api/counselors/favorites")
                    .header("Authorization", "Bearer $authToken2"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("공자"))
        }

        @Test
        @DisplayName("성공: 비활성 상담사는 즐겨찾기 목록에서 제외")
        fun getFavoriteCounselors_withInactiveCounselor_excludesInactive() {
            // given: 활성 및 비활성 상담사 즐겨찾기
            createFavoriteCounselor(testUser, testCounselor1)
            createFavoriteCounselor(testUser, testCounselor3) // 니체는 비활성

            // when & then
            mockMvc.perform(
                get("/api/counselors/favorites")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("소크라테스"))
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        fun getFavoriteCounselors_withoutAuth_returns401() {
            // when & then
            mockMvc.perform(
                get("/api/counselors/favorites"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다"))
        }
    }
