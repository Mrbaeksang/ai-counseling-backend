plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" // Kotlin 린터
    id("io.gitlab.arturbosch.detekt") version "1.23.6" // 코드 품질 분석
}

group = "com.aicounseling"
version = "0.0.1-SNAPSHOT"
description = "app"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    // Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // OAuth2 Client
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // WebFlux (OAuth 토큰 검증 등 비동기 HTTP 호출)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:1.0.0-M6")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // JDSL (Type-safe JPQL) - 3.5.5 버전으로 업그레이드
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.5.5")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.5.5")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.5.5")

    // API Documentation (Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // .env 파일 지원
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Database
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()

    // JDK 21+ 호환성을 위한 JVM 옵션
    jvmArgs(
        // 동적 agent 로딩 허용
        "-XX:+EnableDynamicAgentLoading",
        // agent 사용 추적 비활성화
        "-Djdk.instrument.traceUsage=false",
    )
}

// bootRun 태스크에 UTF-8 인코딩 설정
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs =
        listOf(
            "-Dfile.encoding=UTF-8",
            "-Dconsole.encoding=UTF-8",
            "-Duser.language=ko",
            "-Duser.country=KR",
            "-Dsun.stdout.encoding=UTF-8",
            "-Dsun.stderr.encoding=UTF-8",
            "-Dspring.output.ansi.enabled=ALWAYS",
        )
    systemProperty("file.encoding", "UTF-8")
    systemProperty("console.encoding", "UTF-8")
    environment("JAVA_TOOL_OPTIONS", "-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8")
}

// Ktlint 설정
ktlint {
    version.set("1.0.1")
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(true)

    filter {
        exclude("**/generated/**")
        exclude("**/init/InitDataConfig.kt")
        include("**/kotlin/**")
    }
}

// Detekt 설정
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml") // 설정 파일 (나중에 생성)
    autoCorrect = true
    ignoreFailures = true
}

// Detekt를 위한 별도 Kotlin 컴파일러 버전 설정
configurations.named("detekt").configure {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.23") // detekt 1.23.6이 사용하는 Kotlin 버전
        }
    }
}

// ktlint 태스크에서 InitDataConfig 제외
tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask> {
    exclude("**/InitDataConfig.kt")
}

tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask> {
    exclude("**/InitDataConfig.kt")
}

// 통합 검사 task
tasks.register("check-all") {
    group = "verification"
    description = "모든 코드 품질 검사 실행"
    dependsOn("ktlintCheck", "detekt", "test")
}

// Git Hook 설치 task
tasks.register("installGitHooks") {
    group = "git hooks"
    description = "Git hooks 설치"
    doLast {
        val hookScript =
            """
            #!/bin/sh
            echo "🔍 코드 품질 검사 시작..."
            ./gradlew ktlintCheck --daemon
            if [ ${'$'}? -ne 0 ]; then
                echo "❌ Ktlint 검사 실패! 'gradlew ktlintFormat'으로 수정하세요."
                exit 1
            fi
            echo "✅ 코드 품질 검사 통과!"
            """.trimIndent()

        val hookFile = file(".git/hooks/pre-commit")
        hookFile.parentFile.mkdirs()
        hookFile.writeText(hookScript)
        hookFile.setExecutable(true)
        println("✅ Git pre-commit hook 설치 완료!")
    }
}
