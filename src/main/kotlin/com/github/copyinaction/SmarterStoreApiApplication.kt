package com.github.copyinaction

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing
@EnableScheduling // 스케줄링 활성화
@SpringBootApplication
class SmarterStoreApiApplication

fun main(args: Array<String>) {
	runApplication<SmarterStoreApiApplication>(*args)
}
