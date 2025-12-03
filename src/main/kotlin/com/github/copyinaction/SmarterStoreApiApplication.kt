package com.github.copyinaction

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class SmarterStoreApiApplication

fun main(args: Array<String>) {
	runApplication<SmarterStoreApiApplication>(*args)
}
