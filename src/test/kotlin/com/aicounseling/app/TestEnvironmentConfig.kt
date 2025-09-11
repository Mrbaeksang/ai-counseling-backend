package com.aicounseling.app

import io.github.cdimascio.dotenv.dotenv
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Profile

/**
 * 테스트 환경에서 .env 파일을 로드하기 위한 설정
 * API 키와 같은 민감한 정보를 Git에 커밋하지 않고 사용
 */
@TestConfiguration
@Profile("test")
class TestEnvironmentConfig {
    init {
        // .env 파일 로드 및 시스템 프로퍼티로 설정
        val dotenv =
            dotenv {
                directory = "./"
                ignoreIfMissing = true
            }

        // OpenRouter API 키를 시스템 프로퍼티로 설정
        dotenv.get("OPENROUTER_API_KEY")?.let {
            System.setProperty("OPENROUTER_API_KEY", it)
        }

        // JWT Secret도 설정
        dotenv.get("JWT_SECRET")?.let {
            System.setProperty("JWT_SECRET", it)
        }
    }
}
