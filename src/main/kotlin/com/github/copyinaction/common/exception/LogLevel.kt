package com.github.copyinaction.common.exception

/**
 * 에러코드별 로그 레벨 정의
 * - ERROR: 시스템 오류 - Slack 알림 대상, 즉시 대응 필요
 * - WARN: 비즈니스 예외 - 모니터링 대상
 * - DEBUG: 정상 흐름 예외 - 로그만 기록 (토큰 만료, 로그인 실패 등 빈번히 발생)
 */
enum class LogLevel {
    ERROR,
    WARN,
    DEBUG
}
