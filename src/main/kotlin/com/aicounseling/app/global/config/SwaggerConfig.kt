package com.aicounseling.app.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .openapi("3.1.0") // OpenAPI 버전 명시
            .info(
                Info()
                    .title("AI Counseling API")
                    .version("1.0.0")
                    .description("AI 철학 상담 서비스 API"),
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"),
                    ),
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList("bearerAuth"),
            )
    }
}
