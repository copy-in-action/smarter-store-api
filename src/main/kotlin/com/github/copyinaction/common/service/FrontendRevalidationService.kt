package com.github.copyinaction.common.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Service
class FrontendRevalidationService(
    @Value("\${app.frontend.revalidate-url}") private val revalidateUrl: String,
    @Value("\${app.frontend.revalidate-token}") private val revalidateToken: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    /**
     * 프론트엔드(Next.js)의 캐시를 무효화(Revalidate)합니다.
     *
     * @param tag 무효화할 캐시 태그 (예: "performance-sitemap")
     */
    @Async
    fun revalidateCache(tag: String) {
        if (revalidateUrl.isBlank() || revalidateToken.isBlank()) {
            logger.warn("Frontend revalidation skipped. URL or Token is missing.")
            return
        }

        val uri = UriComponentsBuilder.fromHttpUrl(revalidateUrl)
            .queryParam("secret", revalidateToken)
            .queryParam("tag", tag)
            .build()
            .toUri()

        try {
            logger.info("Triggering frontend revalidation for tag: '{}' to {}", tag, uri)
            val response = restClient.get()
                .uri(uri)
                .retrieve()
                .toBodilessEntity()

            if (response.statusCode.is2xxSuccessful) {
                logger.info("Frontend revalidation successful for tag: '{}'", tag)
            } else {
                logger.error("Frontend revalidation failed for tag: '{}'. Status: {}", tag, response.statusCode)
            }
        } catch (e: Exception) {
            logger.error("Error during frontend revalidation for tag: '{}'", tag, e)
        }
    }
}
