package com.github.copyinaction.audit.domain

/**
 * Audit 카테고리
 */
enum class AuditCategory {
    AUTH,       // 인증
    ACCOUNT,    // 계정
    BOOKING,    // 예매
    PAYMENT,    // 결제
    ADMIN,      // 관리자
    SEAT        // 좌석
}

/**
 * Audit 대상 타입
 */
enum class AuditTargetType {
    USER,
    BOOKING,
    PAYMENT,
    PERFORMANCE,
    SCHEDULE,
    VENUE,
    SEAT,
    NOTICE
}

/**
 * Audit 액션
 */
enum class AuditAction(val category: AuditCategory, val description: String) {
    // 인증
    LOGIN(AuditCategory.AUTH, "로그인"),
    LOGIN_FAILED(AuditCategory.AUTH, "로그인 실패"),
    LOGOUT(AuditCategory.AUTH, "로그아웃"),
    TOKEN_REFRESH(AuditCategory.AUTH, "토큰 갱신"),

    // 계정
    SIGNUP(AuditCategory.ACCOUNT, "회원가입"),
    PASSWORD_CHANGE(AuditCategory.ACCOUNT, "비밀번호 변경"),
    PROFILE_UPDATE(AuditCategory.ACCOUNT, "프로필 수정"),
    ACCOUNT_DELETE(AuditCategory.ACCOUNT, "회원탈퇴"),
    EMAIL_VERIFY(AuditCategory.ACCOUNT, "이메일 인증"),

    // 예매
    BOOKING_START(AuditCategory.BOOKING, "예매 시작"),
    BOOKING_CONFIRM(AuditCategory.BOOKING, "예매 확정"),
    BOOKING_CANCEL(AuditCategory.BOOKING, "예매 취소"),

    // 결제
    PAYMENT_REQUEST(AuditCategory.PAYMENT, "결제 요청"),
    PAYMENT_COMPLETE(AuditCategory.PAYMENT, "결제 완료"),
    PAYMENT_FAIL(AuditCategory.PAYMENT, "결제 실패"),
    REFUND_REQUEST(AuditCategory.PAYMENT, "환불 요청"),
    REFUND_COMPLETE(AuditCategory.PAYMENT, "환불 완료"),

    // 관리자 - 공연
    PERFORMANCE_CREATE(AuditCategory.ADMIN, "공연 등록"),
    PERFORMANCE_UPDATE(AuditCategory.ADMIN, "공연 수정"),
    PERFORMANCE_DELETE(AuditCategory.ADMIN, "공연 삭제"),

    // 관리자 - 회차
    SCHEDULE_CREATE(AuditCategory.ADMIN, "회차 등록"),
    SCHEDULE_UPDATE(AuditCategory.ADMIN, "회차 수정"),
    SCHEDULE_DELETE(AuditCategory.ADMIN, "회차 삭제"),

    // 관리자 - 공연장
    VENUE_CREATE(AuditCategory.ADMIN, "공연장 등록"),
    VENUE_UPDATE(AuditCategory.ADMIN, "공연장 수정"),
    VENUE_DELETE(AuditCategory.ADMIN, "공연장 삭제"),
    VENUE_SEATING_CHART_UPDATE(AuditCategory.ADMIN, "좌석 배치도 수정"),

    // 관리자 - 공지사항
    NOTICE_CREATE(AuditCategory.ADMIN, "공지사항 등록"),
    NOTICE_UPDATE(AuditCategory.ADMIN, "공지사항 수정"),
    NOTICE_DELETE(AuditCategory.ADMIN, "공지사항 삭제"),

    // 좌석
    SEAT_HOLD(AuditCategory.SEAT, "좌석 점유"),
    SEAT_RELEASE(AuditCategory.SEAT, "좌석 해제")
}
