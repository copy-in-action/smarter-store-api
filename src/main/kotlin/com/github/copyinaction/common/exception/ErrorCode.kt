package com.github.copyinaction.common.exception

import org.springframework.http.HttpStatus

/**
 * 전역 에러 코드를 관리하는 Enum
 * @param status HTTP 상태 코드
 * @param message 에러 메시지
 */
enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    // Booking
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예매를 찾을 수 없습니다."),
    BOOKING_EXPIRED(HttpStatus.GONE, "예매 시간이 만료되었습니다."),
    BOOKING_INVALID_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 예매 상태입니다."),
    PRICE_MISMATCH(HttpStatus.BAD_REQUEST, "요청된 가격이 서버 정보와 일치하지 않습니다."),

    // Venue
    VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연장을 찾을 수 없습니다."),
    VENUE_HAS_PERFORMANCES(HttpStatus.CONFLICT, "등록된 공연이 있는 공연장은 삭제할 수 없습니다."),
    SEAT_CAPACITY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 좌석 수 정보를 찾을 수 없습니다."),
    SEAT_CAPACITY_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 등급의 좌석 수가 이미 존재합니다."),

    // Performance
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연을 찾을 수 없습니다."),

    // Company
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 판매자를 찾을 수 없습니다."),
    COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다."),

    // Auth
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "등록되지 않은 이메일입니다."),
    EMAIL_ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증된 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "인증되지 않은 이메일입니다."),
    INVALID_EMAIL_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 이메일 인증 토큰입니다."),
    EXPIRED_EMAIL_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "만료된 이메일 인증 토큰입니다."),

    // Admin Auth
    ADMIN_LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 로그인 ID입니다."),
    ADMIN_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "로그인 ID 또는 비밀번호가 일치하지 않습니다."),

    // Performance Schedule
    PERFORMANCE_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연 회차를 찾을 수 없습니다."),
    PERFORMANCE_SCHEDULE_PERFORMANCE_CANNOT_CHANGE(HttpStatus.BAD_REQUEST, "공연 회차의 공연 정보는 변경할 수 없습니다."),

    // Ticket Option
    TICKET_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 좌석 등급을 찾을 수 없습니다."),

    // Schedule Ticket Stock
    SCHEDULE_TICKET_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 재고 정보를 찾을 수 없습니다."),
    SCHEDULE_TICKET_STOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 회차/좌석등급의 재고가 이미 존재합니다."),
    NOT_ENOUGH_SEATS(HttpStatus.BAD_REQUEST, "잔여 좌석이 부족합니다."),

    // Seat
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 좌석을 찾을 수 없습니다."),
    SEAT_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 좌석이 이미 존재합니다."),

    // Schedule Seat
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연 회차를 찾을 수 없습니다."),
    SCHEDULE_SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회차 좌석을 찾을 수 없습니다."),
    SEAT_NOT_AVAILABLE(HttpStatus.CONFLICT, "이미 선택된 좌석입니다."),
    SEAT_ALREADY_OCCUPIED(HttpStatus.CONFLICT, "이미 점유 또는 예약된 좌석입니다."),
    SEAT_ALREADY_HELD(HttpStatus.CONFLICT, "이미 점유된 좌석입니다."),
    SEAT_HOLD_EXPIRED(HttpStatus.BAD_REQUEST, "좌석 점유 시간이 만료되었습니다."),
    SEAT_NOT_HELD_BY_USER(HttpStatus.FORBIDDEN, "본인이 점유한 좌석이 아닙니다."),
    SEAT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "최대 선택 가능 좌석 수(4석)를 초과했습니다."),
    MAX_SEATS_EXCEEDED(HttpStatus.BAD_REQUEST, "최대 선택 가능 좌석 수를 초과했습니다."),
    NO_SEATS_TO_RESERVE(HttpStatus.BAD_REQUEST, "예약할 좌석이 없습니다. 먼저 좌석을 선택해주세요."),
    SCHEDULE_SEATS_ALREADY_INITIALIZED(HttpStatus.CONFLICT, "이미 좌석이 초기화된 회차입니다."),

    // Ticketing Notice
    TICKETING_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예매 안내사항을 찾을 수 없습니다."),
}
