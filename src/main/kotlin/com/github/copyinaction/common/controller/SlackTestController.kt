package com.github.copyinaction.common.controller

import com.github.copyinaction.common.service.SlackService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test/slack")
@Tag(name = "test-slack", description = "Slack 연동 테스트 API")
class SlackTestController(
    private val slackService: SlackService
) {

    @PostMapping("/send")
    @Operation(
        summary = "Slack 테스트 메시지 발송",
        description = """
            Slack Webhook 연동을 테스트하기 위한 API입니다.

            **요청 예시:**
            ```json
            {
              "message": "테스트 메시지입니다."
            }
            ```

            메시지를 생략하면 기본 메시지가 전송됩니다.

            **권한: 누구나 (테스트용)**
        """
    )
    fun sendTestMessage(@RequestBody(required = false) request: SlackTestRequest?): SlackTestResponse {
        val message = request?.message ?: "Slack 연동 테스트 메시지입니다."

        return try {
            slackService.sendTestMessage(message)
            SlackTestResponse(
                success = true,
                message = "Slack 메시지 전송 요청이 완료되었습니다. Slack 채널을 확인하세요."
            )
        } catch (e: Exception) {
            SlackTestResponse(
                success = false,
                message = "Slack 메시지 전송 실패: ${e.message}"
            )
        }
    }

    data class SlackTestRequest(
        val message: String?
    )

    data class SlackTestResponse(
        val success: Boolean,
        val message: String
    )
}
