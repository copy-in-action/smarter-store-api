package com.github.copyinaction.common.service

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

/**
 * SlackService 통합 테스트
 *
 * 실제 Slack으로 메시지를 발송하는 테스트입니다.
 * .env 파일에 SLACK_WEBHOOK_URL_DEPLOY가 설정되어 있어야 합니다.
 */
@SpringBootTest
@ActiveProfiles("local")
class SlackServiceTest {

    @Autowired
    private lateinit var slackService: SlackService

    @Test
    @DisplayName("일별 통계 성공 알림 메시지가 배포 채널로 전송된다")
    fun sendDailyStatsSuccess() {
        // Given
        val testDate = LocalDate.of(2026, 1, 6)
        val paymentCount = 42

        // When & Then
        // 실제 Slack으로 메시지 전송 (환경변수가 설정되어 있어야 함)
        slackService.sendDailyStatsSuccess(testDate, paymentCount)

        println("✅ 일별 통계 성공 알림 테스트 완료 (Slack 확인 필요)")
    }

    @Test
    @DisplayName("일별 통계 실패 알림 메시지가 배포 채널로 전송된다")
    fun sendDailyStatsFailure() {
        // Given
        val testDate = LocalDate.of(2026, 1, 6)
        val errorMessage = "[테스트] Database connection failed"

        // When & Then
        // 실제 Slack으로 메시지 전송
        slackService.sendDailyStatsFailure(testDate, errorMessage)

        println("✅ 일별 통계 실패 알림 테스트 완료 (Slack 확인 필요)")
    }

    @Test
    @DisplayName("일별 통계 스킵 알림 메시지가 배포 채널로 전송된다")
    fun sendDailyStatsSkipped() {
        // Given
        val testDate = LocalDate.of(2026, 1, 6)

        // When & Then
        // 실제 Slack으로 메시지 전송
        slackService.sendDailyStatsSkipped(testDate)

        println("✅ 일별 통계 스킵 알림 테스트 완료 (Slack 확인 필요)")
    }

    // 아래 테스트들은 Spring Context 없이 직접 인스턴스 생성
    @Test
    @DisplayName("Webhook URL이 설정되지 않으면 메시지를 전송하지 않는다")
    fun doNotSendWhenWebhookUrlIsEmpty() {
        // Given - Spring Context 없이 직접 생성
        val slackServiceWithoutUrl = SlackService("", "local")

        // When & Then
        // 예외가 발생하지 않음을 확인
        slackServiceWithoutUrl.sendDailyStatsSuccess(LocalDate.now(), 10)

        println("✅ 빈 URL 처리 테스트 완료")
    }

    @Test
    @DisplayName("Webhook URL이 localhost면 메시지를 전송하지 않는다")
    fun doNotSendWhenWebhookUrlIsLocalhost() {
        // Given - Spring Context 없이 직접 생성
        val slackServiceWithLocalhost = SlackService("http://localhost", "local")

        // When & Then
        // 예외가 발생하지 않음을 확인
        slackServiceWithLocalhost.sendDailyStatsSuccess(LocalDate.now(), 10)

        println("✅ localhost URL 처리 테스트 완료")
    }
}
