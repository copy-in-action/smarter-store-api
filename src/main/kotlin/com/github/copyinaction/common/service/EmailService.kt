package com.github.copyinaction.common.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username}") private val senderEmail: String,
    @Value("\${app.frontend-url}") private val frontendUrl: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun sendVerificationEmail(toEmail: String, token: String) {
        val subject = "[Smarter Store] 이메일 인증을 완료해주세요."
        val verificationLink = "$frontendUrl/auth/email-verification?token=$token"

        val text = """
            안녕하세요, Smarter Store에 가입해 주셔서 감사합니다.

            이메일 주소를 확인하려면 아래 링크를 클릭해주세요:
            $verificationLink

            이 링크는 30분 동안 유효합니다.
            만약 본인이 요청한 것이 아니라면, 이 이메일을 무시해주세요.

            감사합니다.
            Smarter Store 팀 드림
        """.trimIndent()

        try {
            val message = SimpleMailMessage()
            message.setFrom(senderEmail)
            message.setTo(toEmail)
            message.setSubject(subject)
            message.setText(text)
            mailSender.send(message)
            logger.info("Verification email sent to $toEmail")
        } catch (e: Exception) {
            logger.error("Failed to send verification email to $toEmail", e)
        }
    }
}
