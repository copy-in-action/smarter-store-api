package com.github.copyinaction.common.encryption

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * JPA AttributeConverter - 엔티티 필드 자동 암호화/복호화
 * @Column에 @Convert(converter = EncryptedStringConverter::class) 적용하여 사용
 */
@Converter
class EncryptedStringConverter : AttributeConverter<String?, String?> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        return EncryptionUtil.getInstance().encrypt(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return EncryptionUtil.getInstance().decrypt(dbData)
    }
}
