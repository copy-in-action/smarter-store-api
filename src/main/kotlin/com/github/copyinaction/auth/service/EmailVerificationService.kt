package com.github.copyinaction.auth.service

import com.github.copyinaction.auth.domain.EmailVerificationToken
import com.github.copyinaction.auth.repository.EmailVerificationTokenRepository
import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.common.service.EmailService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 이메일 인증(OTP) 담당 서비스
 */
@Service
class EmailVerificationService(
    private val userRepository: UserRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val emailService: EmailService
) {

    /**
     * 이메일 인증 요청 - OTP 발송
     */
    @Transactional
    fun requestVerification(email: String) {
        if (userRepository.findByEmail(email).isPresent) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }

        emailVerificationTokenRepository.deleteByEmail(email)
        emailVerificationTokenRepository.flush()

        val verificationToken = EmailVerificationToken.create(email)
        emailVerificationTokenRepository.save(verificationToken)

        emailService.sendVerificationEmail(email, verificationToken.token)
    }

    /**
     * OTP 확인
     */
    @Transactional
    fun confirmOtp(email: String, otp: String) {
        val verificationToken = emailVerificationTokenRepository.findByEmailAndToken(email, otp)
            .orElseThrow { CustomException(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN) }

        verificationToken.validate(email, otp)
        verificationToken.confirm()
        emailVerificationTokenRepository.save(verificationToken)
    }

    /**
     * 이메일 인증 완료 여부 검증 (회원가입 시 사용)
     */
    @Transactional
    fun validateAndConsumeVerification(email: String) {
        val confirmedToken = emailVerificationTokenRepository.findByEmailAndIsConfirmed(email, true)
            .orElseThrow { CustomException(ErrorCode.EMAIL_NOT_VERIFIED) }

        if (confirmedToken.isExpired()) {
            throw CustomException(ErrorCode.EXPIRED_EMAIL_VERIFICATION_TOKEN)
        }

        emailVerificationTokenRepository.delete(confirmedToken)
    }
}
