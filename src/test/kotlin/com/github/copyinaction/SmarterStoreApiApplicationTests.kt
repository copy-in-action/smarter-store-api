package com.github.copyinaction

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import io.jsonwebtoken.security.Keys
import java.util.Base64

@SpringBootTest
class SmarterStoreApiApplicationTests {

	@Test
	fun contextLoads() {
		 // HS256 알고리즘을 위한 256비트(32바이트) 키를 생성합니다.
		 val key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256)
		 // 생성된 키를 Base64로 인코딩합니다.
		 val base64Key = Base64.getEncoder().encodeToString(key.encoded)
		 println("생성된 Base64 키: $base64Key")
	  }
}
