package com.github.copyinaction.common.exception

/**
 * 비즈니스 로직 상의 커스텀 예외
 * @param errorCode 미리 정의된 에러 코드
 */
class CustomException(
    val errorCode: ErrorCode,
    message: String? = null
) : RuntimeException(message ?: errorCode.message)
