package com.aicounseling.app.domain.counselor.controller

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * DELETE /api/counselors/{id}/favorite API 테스트
 * 상담사 즐겨찾기 삭제 기능 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("DELETE /api/counselors/{id}/favorite - 상담사 즐겨찾기 삭제")
class RemoveFavoriteApiTest
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
        @DisplayName("성공: 즐겨찾기 삭제")
        fun removeFavorite_withExistingFavorite_deletesSuccessfully() {
            // given: 즐겨찾기 추가
            createFavoriteCounselor(testUser, testCounselor1)
            assert(favoriteCounselorRepository.findAll().size == 1)

            // when & then
            mockMvc.perform(
                delete("/api/counselors/${testCounselor1.id}/favorite")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("즐겨찾기가 해제되었습니다"))
                .andExpect(jsonPath("$.data").value("즐겨찾기 제거 성공"))

            // then: 즐겨찾기 삭제 확인
            val favorites = favoriteCounselorRepository.findAll()
            assert(favorites.isEmpty())
        }

        @Test
        @DisplayName("성공: 즐겨찾기되지 않은 상담사 삭제 시도")
        fun removeFavorite_withNonExistingFavorite_returnsNotFound() {
            // when & then
            mockMvc.perform(
                delete("/api/counselors/${testCounselor1.id}/favorite")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-404"))
                .andExpect(jsonPath("$.msg").value("즐겨찾기하지 않은 상담사입니다"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        @DisplayName("성공: 다른 사용자의 즐겨찾기는 영향받지 않음")
        fun removeFavorite_withOtherUserFavorite_doesNotAffectOthers() {
            // given: 두 사용자가 같은 상담사 즐겨찾기
            createFavoriteCounselor(testUser, testCounselor1)
            createFavoriteCounselor(testUser2, testCounselor1)
            assert(favoriteCounselorRepository.findAll().size == 2)

            // when: testUser가 즐겨찾기 삭제
            mockMvc.perform(
                delete("/api/counselors/${testCounselor1.id}/favorite")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))

            // then: testUser2의 즐겨찾기는 유지됨
            val favorites = favoriteCounselorRepository.findAll()
            assert(favorites.size == 1)
            assert(favorites[0].user.id == testUser2.id)
        }

        @Test
        @DisplayName("성공: 여러 즐겨찾기 중 특정 상담사만 삭제")
        fun removeFavorite_withMultipleFavorites_deletesOnlySpecified() {
            // given: 여러 상담사 즐겨찾기
            createFavoriteCounselor(testUser, testCounselor1)
            createFavoriteCounselor(testUser, testCounselor2)
            assert(favoriteCounselorRepository.findAll().size == 2)

            // when: 특정 상담사만 삭제
            mockMvc.perform(
                delete("/api/counselors/${testCounselor1.id}/favorite")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))

            // then: 다른 즐겨찾기는 유지됨
            val favorites = favoriteCounselorRepository.findAll()
            assert(favorites.size == 1)
            assert(favorites[0].counselor.id == testCounselor2.id)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 상담사 즐겨찾기 삭제 시도")
        fun removeFavorite_withInvalidCounselor_returns404() {
            // when & then
            mockMvc.perform(
                delete("/api/counselors/99999/favorite")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-404"))
                .andExpect(jsonPath("$.msg").value("상담사를 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("실패: 비활성 상담사 즐겨찾기 삭제 시도")
        fun removeFavorite_withInactiveCounselor_returns404() {
            // when & then
            mockMvc.perform(
                delete("/api/counselors/${testCounselor3.id}/favorite")
                    .header("Authorization", "Bearer $authToken"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-400"))
                .andExpect(jsonPath("$.msg").value("비활성 상담사입니다"))
        }

        @Test
        @DisplayName("실패: 인증 없이 즐겨찾기 삭제 시도")
        fun removeFavorite_withoutAuth_returns401() {
            // when & then
            mockMvc.perform(
                delete("/api/counselors/${testCounselor1.id}/favorite"),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다"))
        }
    }
