package com.github.copyinaction.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "bearerAuth"

        return OpenAPI()
            .info(
                Info()
                    .title("Smarter Store API")
                    .description("""
                        |Smarter Store 백엔드 API 문서
                        |
                        |**관리자 대시보드**: [매출 현황 대시보드](/admin/dashboard.html)
                    """.trimMargin())
                    .version("1.0.0")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Access Token을 입력하세요. (Bearer 접두사 불필요)")
                    )
            )
    }

    @Bean
    fun sortSchemasCustomizer(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi ->
            openApi.components?.schemas?.let { schemas ->
                val sortedSchemas = schemas.toSortedMap(String.CASE_INSENSITIVE_ORDER)
                schemas.clear()
                schemas.putAll(sortedSchemas)
            }
        }
    }
}
