package com.github.copyinaction.demo.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * 데모 시딩 기능 활성화(`demo.seed.enabled=true`) 시에만 빈을 등록하는 메타 어노테이션.
 *
 * `@Profile`이 아닌 프로퍼티로 제어하는 이유: 배포 서버가 `prod` 프로파일로 실행되기 때문에
 * 프로파일 기준으로는 데모 기능만 선택적으로 켜고 끌 수 없다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ConditionalOnProperty(prefix = "demo.seed", name = ["enabled"], havingValue = "true")
annotation class ConditionalOnDemoSeed
