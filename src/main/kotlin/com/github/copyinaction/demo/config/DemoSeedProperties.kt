package com.github.copyinaction.demo.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 데모 데이터 시딩 설정 (`demo.seed.*`)
 *
 * 배포 서버는 `SPRING_PROFILES_ACTIVE=prod`로 실행되므로 `@Profile`이 아닌
 * `enabled` 프로퍼티(`DEMO_SEED_ENABLED`)로 기능을 제어한다.
 */
@Component
@ConfigurationProperties(prefix = "demo.seed")
class DemoSeedProperties {
    /** 데모 시딩 전체 활성화 여부 */
    var enabled: Boolean = false

    /** 회차 롤링 기간(일) - 오늘 기준 몇 일 뒤까지 회차를 유지할지 */
    var rollingDays: Int = 56

    /** 주간 롤링 배치 cron */
    var rollingCron: String = "0 0 3 * * MON"

    /** 일간 봇 이력 정리 배치 cron */
    var cleanupCron: String = "0 30 4 * * *"

    /** 공용 체험 계정 초기화 배치 cron */
    var demoUserResetCron: String = "0 0 4 * * *"

    /** 과거 회차 봇 이력 보관 기간(일) */
    var retentionDays: Int = 14

    /** 실시간 좌석 시뮬레이터 활성화 여부 */
    var simulatorEnabled: Boolean = true

    /** 실시간 좌석 시뮬레이터 실행 간격(ms) */
    var simulatorIntervalMs: Long = 5000

    /** 공용 체험 계정 비밀번호 */
    var demoUserPassword: String = "DemoPass1234!"

    /** 봇 계정 비밀번호 (외부 로그인 용도가 아니므로 내부용) */
    var botUserPassword: String = "BotInternalPass1234!"

    /** 회차당 초기 시딩 최대 좌석 수 - 대형 공연장에서 행 폭발을 막기 위한 상한 */
    var maxSeatsPerSchedule: Int = 220
}
