package com.aicounseling.app.global.config

import com.linecorp.kotlinjdsl.support.spring.data.jpa.autoconfigure.KotlinJdslAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Configuration

/**
 * JDSL (Kotlin JDSL) 설정 클래스
 *
 * JDSL 3.5.5 버전과 Spring Boot 3.5 호환성을 위한 명시적 설정
 * KotlinJdslJpqlExecutor Bean을 자동으로 등록합니다.
 */
@Configuration
@ImportAutoConfiguration(KotlinJdslAutoConfiguration::class)
class JdslConfig
