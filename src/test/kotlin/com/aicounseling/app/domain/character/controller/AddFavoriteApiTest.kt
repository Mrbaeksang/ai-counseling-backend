package com.aicounseling.app.domain.character.controller

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

/**
 * POST /api/characters/{id}/favorite API 테스트
 * 캐릭터 즐겨찾기 추가 기능 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("POST /api/characters/{id}/favorite - 캐릭터 즐겨찾기 추가")
class AddFavoriteApiTest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
        jwtTokenProvider: com.aicounseling.app.global.security.JwtTokenProvider,
        userRepository: com.aicounseling.app.domain.user.repository.UserRepository,
        characterRepository: com.aicounseling.app.domain.character.repository.CharacterRepository,
        characterRatingRepository: com.aicounseling.app.domain.character.repository.CharacterRatingRepository,
        favoriteCharacterRepository: com.aicounseling.app.domain.character.repository.FavoriteCharacterRepository,
        sessionRepository: com.aicounseling.app.domain.session.repository.ChatSessionRepository,
    ) : CharacterControllerBaseTest(
            mockMvc,
            objectMapper,
            jwtTokenProvider,
            userRepository,
            characterRepository,
            characterRatingRepository,
            favoriteCharacterRepository,
            sessionRepository,
        ) {
        @Test
        @DisplayName("성공: 새로운 즐겨찾기 추가")
        fun addFavorite_withValidCounselor_createsNewFavorite() {
            // given: 즐겨찾기 추가 전 확인
            val favorites = favoriteCharacterRepository.findAll()
            assert(favorites.isEmpty())

            // when & then
            mockMvc.perform(
                post("/api/characters/${testCharacter1.id}/favorite")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("즐겨찾기가 추가되었습니다"))
                .andExpect(jsonPath("$.data").value("즐겨찾기 추가 성공"))

            // then: 즐겨찾기 추가 확인
            val updatedFavorites = favoriteCharacterRepository.findAll()
            assert(updatedFavorites.size == 1)
            assert(updatedFavorites[0].user.id == testUser.id)
            assert(updatedFavorites[0].character.id == testCharacter1.id)
        }

        @Test
        @DisplayName("성공: 이미 즐겨찾기된 캐릭터 중복 추가 시도")
        fun addFavorite_withExistingFavorite_returnsAlreadyExists() {
            // given: 이미 즐겨찾기 추가
            createFavoriteCharacter(testUser, testCharacter1)

            // when & then
            mockMvc.perform(
                post("/api/characters/${testCharacter1.id}/favorite")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-409"))
                .andExpect(jsonPath("$.msg").value("이미 즐겨찾기한 캐릭터입니다"))

            // then: 중복 추가되지 않음 확인
            val favorites = favoriteCharacterRepository.findAll()
            assert(favorites.size == 1)
        }

        @Test
        @DisplayName("성공: 다른 사용자가 같은 캐릭터 즐겨찾기 가능")
        fun addFavorite_withDifferentUser_createsSeperateFavorite() {
            // given: testUser가 먼저 즐겨찾기
            createFavoriteCharacter(testUser, testCharacter1)

            // when & then: testUser2도 같은 캐릭터 즐겨찾기
            mockMvc.perform(
                post("/api/characters/${testCharacter1.id}/favorite")
                    .header("Authorization", "Bearer $authToken2")
                    .contentType(MediaType.APPLICATION_JSON),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.msg").value("즐겨찾기가 추가되었습니다"))

            // then: 각 사용자별로 즐겨찾기 생성됨
            val favorites = favoriteCharacterRepository.findAll()
            assert(favorites.size == 2)
            assert(favorites.any { it.user.id == testUser.id })
            assert(favorites.any { it.user.id == testUser2.id })
        }

        @Test
        @DisplayName("실패: 존재하지 않는 캐릭터 즐겨찾기 시도")
        fun addFavorite_withInvalidCounselor_returns404() {
            // when & then
            mockMvc.perform(
                post("/api/characters/99999/favorite")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-404"))
                .andExpect(jsonPath("$.msg").value("캐릭터를 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("실패: 비활성 캐릭터 즐겨찾기 시도")
        fun addFavorite_withInactiveCounselor_returns404() {
            // when & then
            mockMvc.perform(
                post("/api/characters/${testCharacter3.id}/favorite")
                    .header("Authorization", "Bearer $authToken")
                    .contentType(MediaType.APPLICATION_JSON),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("F-400"))
                .andExpect(jsonPath("$.msg").value("비활성화된 캐릭터입니다"))
        }

        @Test
        @DisplayName("실패: 인증 없이 즐겨찾기 시도")
        fun addFavorite_withoutAuth_returns401() {
            // when & then
            mockMvc.perform(
                post("/api/characters/${testCharacter1.id}/favorite")
                    .contentType(MediaType.APPLICATION_JSON),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("F-401"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다"))
        }
    }
