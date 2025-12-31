package com.github.copyinaction.common.encryption

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * AES-256-GCM 암호화 유틸리티
 * 개인정보(전화번호 등) 암호화에 사용
 */
@Component
class EncryptionUtil(
    @Value("\${encryption.secret-key}") private val secretKey: String
) {
    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        @Volatile
        private var instance: EncryptionUtil? = null

        fun getInstance(): EncryptionUtil {
            return instance ?: throw IllegalStateException("EncryptionUtil이 초기화되지 않았습니다.")
        }
    }

    init {
        instance = this
    }

    private val keySpec: SecretKeySpec by lazy {
        val keyBytes = Base64.getDecoder().decode(secretKey)
        require(keyBytes.size == 32) { "암호화 키는 32바이트(256비트)여야 합니다." }
        SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrBlank()) return null

        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted

        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encryptedText: String?): String? {
        if (encryptedText.isNullOrBlank()) return null

        val combined = Base64.getDecoder().decode(encryptedText)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
