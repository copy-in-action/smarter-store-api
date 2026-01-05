package com.github.copyinaction.common.exception

import org.springframework.http.HttpStatus

/**
 * 전역 에러 코드를 관리하는 Enum
 * @param status HTTP 상태 코드
 * @param message 에러 메시지
 * @param logLevel 로그 레벨 (기본값: WARN)
 */
enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
    val logLevel: LogLevel = LogLevel.WARN,
) {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생했습니다.", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.", LogLevel.DEBUG),

    // Critical - Slack 알림 대상 (즉시 조치 필요)
    DATA_INTEGRITY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 정합성 오류가 발생했습니다.", LogLevel.ERROR),
    BOOKING_CONFIRM_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "예매 확정 처리 중 오류가 발생했습니다.", LogLevel.ERROR),
    EXTERNAL_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "외부 서비스 연동 중 오류가 발생했습니다.", LogLevel.ERROR),

    // Booking
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예매를 찾을 수 없습니다."),
    BOOKING_EXPIRED(HttpStatus.GONE, "예매 시간이 만료되었습니다."),
    BOOKING_INVALID_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 예매 상태입니다."),

    // Venue
    VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연장을 찾을 수 없습니다."),
    VENUE_HAS_PERFORMANCES(HttpStatus.CONFLICT, "등록된 공연이 있는 공연장은 삭제할 수 없습니다."),

    // Performance
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연을 찾을 수 없습니다."),

    // Company
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 판매자를 찾을 수 없습니다."),
    COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다."),

    // Auth
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다.", LogLevel.DEBUG),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.", LogLevel.DEBUG),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다.", LogLevel.DEBUG),
    EMAIL_ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증된 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "인증되지 않은 이메일입니다.", LogLevel.DEBUG),
    INVALID_EMAIL_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 이메일 인증 토큰입니다.", LogLevel.DEBUG),
    EXPIRED_EMAIL_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "만료된 이메일 인증 토큰입니다.", LogLevel.DEBUG),

    // Admin Auth
    ADMIN_LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 로그인 ID입니다."),
    ADMIN_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "로그인 ID 또는 비밀번호가 일치하지 않습니다.", LogLevel.DEBUG),

    // Performance Schedule
    PERFORMANCE_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연 회차를 찾을 수 없습니다."),
    DUPLICATE_SCHEDULE(HttpStatus.CONFLICT, "해당 시간에 이미 등록된 공연 회차가 있습니다."),
    SCHEDULE_ALREADY_BOOKED(HttpStatus.BAD_REQUEST, "이미 예매가 진행된 회차는 수정하거나 삭제할 수 없습니다."),

    // Seat
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연 회차를 찾을 수 없습니다."),
    SEAT_ALREADY_OCCUPIED(HttpStatus.CONFLICT, "이미 점유 또는 예약된 좌석입니다."),
    SEAT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "최대 선택 가능 좌석 수(4석)를 초과했습니다."),
    SEAT_GRADE_NOT_FOUND_IN_VENUE(HttpStatus.BAD_REQUEST, "공연장에 존재하지 않는 좌석 등급이 포함되어 있습니다."),

    // Notice
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공지사항을 찾을 수 없습니다."),

    // Audit
    AUDIT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 감사 로그를 찾을 수 없습니다."),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 결제 정보를 찾을 수 없습니다."),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 결제입니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "결제 취소에 실패했습니다."),
}
