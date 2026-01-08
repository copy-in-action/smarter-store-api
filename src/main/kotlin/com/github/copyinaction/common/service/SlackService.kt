package com.github.copyinaction.common.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Service
class SlackService(
    @Value("\${slack.webhook-url-deploy:}") private val webhookUrlDeploy: String,
    @Value("\${spring.profiles.active:local}") private val activeProfile: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = createRestTemplate()

    /**
     * RestTemplate 생성 (local 프로파일에서는 SSL 검증 우회)
     */
    private fun createRestTemplate(): RestTemplate {
        return if (activeProfile == "local") {
            log.info("로컬 환경: SSL 검증을 우회하는 RestTemplate 생성")
            createSslBypassRestTemplate()
        } else {
            log.info("운영 환경: 기본 RestTemplate 사용")
            RestTemplate()
        }
    }

    /**
     * SSL 검증을 우회하는 RestTemplate 생성 (로컬 개발 환경 전용)
     */
    private fun createSslBypassRestTemplate(): RestTemplate {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? = null
            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        // SimpleClientHttpRequestFactory를 상속하여 SSL 설정 커스터마이징
        val factory = object : SimpleClientHttpRequestFactory() {
            override fun prepareConnection(connection: java.net.HttpURLConnection, httpMethod: String) {
                if (connection is HttpsURLConnection) {
                    connection.sslSocketFactory = sslContext.socketFactory
                    connection.setHostnameVerifier { _, _ -> true }
                }
                super.prepareConnection(connection, httpMethod)
            }
        }

        return RestTemplate(factory)
    }

    /**
     * Slack 메시지 전송 (배포 채널용)
     */
    private fun sendDeployMessage(message: String) {
        sendMessageToUrl(webhookUrlDeploy, message)
    }

    /**
     * 지정된 Webhook URL로 메시지 전송
     */
    private fun sendMessageToUrl(url: String, message: String) {
        if (url.isBlank() || url == "http://localhost") {
            log.debug("Slack webhook URL이 설정되지 않아 메시지를 전송하지 않습니다. URL: {}", url)
            return
        }

        try {
            val payload = mapOf(
                "text" to message
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val entity = HttpEntity(payload, headers)
            log.debug("Slack 메시지 전송 시도: URL={}", url)

            val response = restTemplate.postForEntity(url, entity, String::class.java)

            log.info("Slack 메시지 전송 성공: status={}, body={}", response.statusCode, response.body)
        } catch (e: Exception) {
            log.error("Slack 메시지 전송 실패: URL={}, message={}", url, e.message, e)
        }
    }

    /**
     * 테스트 메시지 전송
     */
    fun sendTestMessage(message: String = "Slack 연동 테스트 메시지입니다.") {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val testMessage = """
            :test_tube: *Slack 연동 테스트*

            • 메시지: `$message`
            • 전송 시각: `$now`
            • 프로파일: `$activeProfile`
        """.trimIndent()

        sendDeployMessage(testMessage)
    }

    /**
     * 일별 통계 집계 성공 알림
     */
    fun sendDailyStatsSuccess(date: LocalDate, paymentCount: Int) {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val message = """
            :white_check_mark: *일별 통계 집계 완료*

            • 집계 일자: `$dateStr`
            • 처리 건수: `$paymentCount`건
            • 완료 시각: `$now`
        """.trimIndent()

        sendDeployMessage(message)
    }

    /**
     * 일별 통계 집계 실패 알림
     */
    fun sendDailyStatsFailure(date: LocalDate, errorMessage: String) {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val message = """
            :x: *일별 통계 집계 실패*

            • 집계 일자: `$dateStr`
            • 실패 시각: `$now`
            • 오류 내용: `$errorMessage`
        """.trimIndent()

        sendDeployMessage(message)
    }

    /**
     * 일별 통계 집계 스킵 알림 (집계 대상 없음)
     */
    fun sendDailyStatsSkipped(date: LocalDate) {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val message = """
            :information_source: *일별 통계 집계 스킵*

            • 집계 일자: `$dateStr`
            • 사유: 집계 대상 결제 건 없음
            • 확인 시각: `$now`
        """.trimIndent()

        sendDeployMessage(message)
    }
}
