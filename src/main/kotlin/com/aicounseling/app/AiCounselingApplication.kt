package com.aicounseling.app

import io.github.cdimascio.dotenv.dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
@EnableJpaAuditing
class AiCounselingApplication

fun main(args: Array<String>) {
    // .env 파일 로드
    val dotenv =
        dotenv {
            ignoreIfMissing = true // .env 파일이 없어도 에러 안 남
        }

    // 시스템 프로퍼티로 설정
    dotenv.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
    }

    @Suppress("SpreadOperator") // Spring Boot에서 필수적으로 사용
    runApplication<AiCounselingApplication>(*args)
}
