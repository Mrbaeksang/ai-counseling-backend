package com.aicounseling.app.global.controller

import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.global.rsData.RsData
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 헬스체크 및 디버깅용 컨트롤러
 * 의존성 없이 단순 응답만 반환하여 기본 설정 테스트
 */
@RestController
@RequestMapping("/api/health")
class HealthController(
    private val counselorRepository: CounselorRepository,
) {
    @GetMapping("/simple")
    fun simpleHealth(): RsData<Map<String, String>> {
        return RsData.of(
            "S-1",
            "헬스체크 성공",
            mapOf(
                "status" to "UP",
                "message" to "서버가 정상 작동 중입니다",
            ),
        )
    }

    @GetMapping("/error-test")
    fun errorTest(): RsData<String> {
        error("테스트 에러입니다")
    }

    @GetMapping("/db-test")
    fun dbTest(): RsData<Map<String, Any>> {
        return try {
            val count = counselorRepository.count()
            RsData.of(
                "S-1",
                "DB 연결 성공",
                mapOf(
                    "counselorCount" to count,
                    "dbStatus" to "connected",
                ),
            )
        } catch (e: org.springframework.dao.DataAccessException) {
            RsData.of(
                "F-500",
                "DB 연결 실패: ${e.message}",
                mapOf(
                    "error" to e.javaClass.simpleName,
                    "message" to (e.message ?: "Unknown error"),
                ),
            )
        }
    }
}
