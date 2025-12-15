package com.github.copyinaction.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.beans.factory.annotation.Value

import java.util.Properties

@Configuration
class MailConfig(
    @Value("\${spring.mail.host}") private val host: String,
    @Value("\${spring.mail.port}") private val port: Int,
    @Value("\${spring.mail.username}") private val username: String,
    @Value("\${spring.mail.password}") private val password: String,
    @Value("\${spring.mail.properties.mail.smtp.auth}") private val smtpAuth: Boolean,
    @Value("\${spring.mail.properties.mail.smtp.starttls.enable}") private val startTlsEnable: Boolean,
    @Value("\${spring.mail.properties.mail.smtp.connectiontimeout}") private val connectionTimeout: Int,
    @Value("\${spring.mail.properties.mail.smtp.timeout}") private val timeout: Int,
    @Value("\${spring.mail.properties.mail.smtp.writetimeout}") private val writeTimeout: Int
) {

    @Bean
    fun javaMailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = host
        mailSender.port = port
        mailSender.username = username
        mailSender.password = password

        val props = mailSender.javaMailProperties
        props["mail.smtp.auth"] = smtpAuth
        props["mail.smtp.starttls.enable"] = startTlsEnable
        props["mail.smtp.connectiontimeout"] = connectionTimeout
        props["mail.smtp.timeout"] = timeout
        props["mail.smtp.writetimeout"] = writeTimeout

        return mailSender
    }
}
