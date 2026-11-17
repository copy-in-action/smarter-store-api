package com.github.copyinaction.demo.service

import com.github.copyinaction.auth.domain.Role
import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.demo.config.DemoSeedProperties
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 데모용 계정 시드 제공자
 * - 봇 유저 20명: 좌석 시뮬레이터/초기 예매 시딩에서 "다른 사용자" 역할을 한다.
 * - 공용 체험 유저 3명: 외부 방문자가 로그인해서 마이페이지를 체험하는 계정
 */
@Service
@ConditionalOnDemoSeed
class DemoUserSeedProvider(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val properties: DemoSeedProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val BOT_COUNT = 20
        const val DEMO_USER_COUNT = 3

        /** 봇 계정 이메일 접두사 (정리 배치에서 봇 판별에 사용) */
        const val BOT_EMAIL_PREFIX = "demo-bot-"
        const val DEMO_USER_EMAIL_PREFIX = "demo-user-"
        const val EMAIL_DOMAIN = "@smarter.dev"

        fun botEmails(): List<String> =
            (1..BOT_COUNT).map { "$BOT_EMAIL_PREFIX${"%02d".format(it)}$EMAIL_DOMAIN" }

        fun demoUserEmails(): List<String> =
            (1..DEMO_USER_COUNT).map { "$DEMO_USER_EMAIL_PREFIX${"%02d".format(it)}$EMAIL_DOMAIN" }
    }

    /**
     * 봇/공용 체험 계정이 없으면 생성한다. (이미 존재하는 계정은 건드리지 않음)
     * @return 새로 생성된 계정 수
     */
    @Transactional
    fun ensureUsers(): Int {
        val existingEmails = userRepository.findAllByEmailIn(botEmails() + demoUserEmails())
            .map { it.email }
            .toSet()

        val newUsers = mutableListOf<User>()

        botEmails().forEachIndexed { index, email ->
            if (email !in existingEmails) {
                newUsers.add(
                    User.create(
                        email = email,
                        username = "데모봇${index + 1}",
                        rawPassword = botPassword(),
                        passwordEncoder = passwordEncoder,
                        phoneNumber = "010-9999-${"%04d".format(index + 1)}",
                        isEmailVerified = true,
                        role = Role.USER
                    )
                )
            }
        }

        demoUserEmails().forEachIndexed { index, email ->
            if (email !in existingEmails) {
                newUsers.add(
                    User.create(
                        email = email,
                        username = "체험유저${index + 1}",
                        rawPassword = demoPassword(),
                        passwordEncoder = passwordEncoder,
                        phoneNumber = "010-0000-${"%04d".format(index + 1)}",
                        isEmailVerified = true,
                        role = Role.USER
                    )
                )
            }
        }

        if (newUsers.isNotEmpty()) {
            userRepository.saveAll(newUsers)
            log.info("[DemoSeed] 데모 계정 {}개 생성 (봇+공용 체험 계정)", newUsers.size)
        }
        return newUsers.size
    }

    @Transactional(readOnly = true)
    fun findBotUserIds(): List<Long> =
        userRepository.findAllByEmailIn(botEmails()).map { it.id }

    @Transactional(readOnly = true)
    fun findDemoUsers(): List<User> =
        userRepository.findAllByEmailIn(demoUserEmails()).sortedBy { it.email }

    /** 환경변수가 빈 문자열로 주입돼도 기본값으로 안전하게 대체한다. */
    fun demoPassword(): String = properties.demoUserPassword.ifBlank { "DemoPass1234!" }

    fun botPassword(): String = properties.botUserPassword.ifBlank { "BotInternalPass1234!" }
}
