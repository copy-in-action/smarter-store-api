package com.github.copyinaction.performance.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import jakarta.persistence.*
import org.hibernate.annotations.Comment

@Entity
@Table(name = "company")
@Comment("판매자 정보")
class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("회사 ID")
    val id: Long = 0,

    @Column(nullable = false, length = 255)
    @Comment("상호")
    var name: String,

    @Column(length = 100)
    @Comment("대표자명")
    var ceoName: String?,

    @Column(nullable = false, unique = true, length = 50)
    @Comment("사업자등록번호")
    var businessNumber: String,

    @Column(length = 255)
    @Comment("이메일")
    var email: String?,

    @Column(length = 50)
    @Comment("연락처")
    var contact: String?,

    @Column(length = 500)
    @Comment("주소")
    var address: String?,

    @Column(length = 255)
    @Comment("공연 문의 연락처/이메일")
    var performanceInquiry: String?

) : BaseEntity() {

    companion object {
        fun create(
            name: String,
            ceoName: String?,
            businessNumber: String,
            email: String?,
            contact: String?,
            address: String?,
            performanceInquiry: String?
        ): Company {
            val company = Company(
                name = name,
                ceoName = ceoName,
                businessNumber = businessNumber,
                email = email,
                contact = contact,
                address = address,
                performanceInquiry = performanceInquiry
            )
            company.validate()
            return company
        }
    }

    fun update(
        name: String,
        ceoName: String?,
        businessNumber: String,
        email: String?,
        contact: String?,
        address: String?,
        performanceInquiry: String?
    ) {
        this.name = name
        this.ceoName = ceoName
        this.businessNumber = businessNumber
        this.email = email
        this.contact = contact
        this.address = address
        this.performanceInquiry = performanceInquiry
        validate()
    }

    private fun validate() {
        if (name.isBlank()) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE, "상호명은 필수입니다.")
        }
    }
}