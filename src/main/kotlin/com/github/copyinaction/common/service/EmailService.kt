package com.github.copyinaction.common.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: SpringTemplateEngine,
    @Value("\${spring.mail.username}") private val senderEmail: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun sendVerificationEmail(toEmail: String, token: String) { // token is the OTP
        val subject = "[Smarter Store] 이메일 인증번호를 확인해주세요."
        
        try {
            // Create Thymeleaf context and set variables
            val context = Context()
            context.setVariable("otp", token)

            // Process the template to generate HTML
            val htmlBody = templateEngine.process("mail/verification", context)

            // Send the HTML email
            val mimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
            helper.setFrom(senderEmail)
            helper.setTo(toEmail)
            helper.setSubject(subject)
            helper.setText(htmlBody, true) // true indicates HTML content
            mailSender.send(mimeMessage)

            logger.debug("인증 이메일 발송 완료")
        } catch (e: Exception) {
            logger.error("인증 이메일 발송 실패", e)
        }
    }
}
