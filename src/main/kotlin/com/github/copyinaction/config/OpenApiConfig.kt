package com.github.copyinaction.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun userApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("1. User API")
            .pathsToMatch("/api/**")
            .pathsToExclude("/api/admin/**")
            .build()
    }

    @Bean
    fun adminApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("2. Admin API")
            .pathsToMatch("/api/admin/**")
            .build()
    }

    @Bean
    fun allApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("3. All API")
            .pathsToMatch("/api/**")
            .build()
    }

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
