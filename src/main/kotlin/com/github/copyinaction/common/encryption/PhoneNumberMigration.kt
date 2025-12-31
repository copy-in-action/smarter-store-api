package com.github.copyinaction.common.encryption

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value

/**
 * 기존 평문 전화번호를 암호화하는 일회성 마이그레이션
 *
 * 실행 방법: application.yml에서 migration.encrypt-phone-number=true 설정
 * 완료 후 반드시 false로 변경할 것
 */
@Component
class PhoneNumberMigration(
    private val jdbcTemplate: JdbcTemplate,
    private val encryptionUtil: EncryptionUtil,
    @Value("\${migration.encrypt-phone-number:false}") private val enabled: Boolean
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments?) {
        if (!enabled) {
            return
        }

        log.info("=== 전화번호 암호화 마이그레이션 시작 ===")

        val users = jdbcTemplate.queryForList(
            "SELECT id, phone_number FROM users WHERE phone_number IS NOT NULL"
        )

        var migrated = 0
        var skipped = 0

        users.forEach { row ->
            val id = row["id"] as Long
            val phoneNumber = row["phone_number"] as String?

            if (phoneNumber.isNullOrBlank()) {
                return@forEach
            }

            // 이미 암호화된 데이터인지 확인 (Base64 형식 + 최소 길이)
            if (isAlreadyEncrypted(phoneNumber)) {
                skipped++
                return@forEach
            }

            try {
                val encrypted = encryptionUtil.encrypt(phoneNumber)
                jdbcTemplate.update(
                    "UPDATE users SET phone_number = ? WHERE id = ?",
                    encrypted, id
                )
                migrated++
                log.debug("User ID {} 전화번호 암호화 완료", id)
            } catch (e: Exception) {
                log.error("User ID {} 암호화 실패: {}", id, e.message)
            }
        }

        log.info("=== 마이그레이션 완료: 암호화 {}건, 스킵 {}건 ===", migrated, skipped)
        log.warn("마이그레이션 완료 후 migration.encrypt-phone-number=false 로 변경하세요!")
    }

    /**
     * 이미 암호화된 데이터인지 확인
     * - Base64 문자열이고 길이가 긴 경우 암호화된 것으로 판단
     */
    private fun isAlreadyEncrypted(value: String): Boolean {
        // 평문 전화번호는 보통 10-11자리 숫자
        // 암호화된 값은 Base64로 최소 40자 이상
        if (value.length < 30) return false

        // Base64 패턴 확인
        return value.matches(Regex("^[A-Za-z0-9+/]+=*$"))
    }
}
