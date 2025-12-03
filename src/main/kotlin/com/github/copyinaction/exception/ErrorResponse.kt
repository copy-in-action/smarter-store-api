package com.github.copyinaction.exception

/**
 * API 에러 발생 시 Body에 포함되는 표준 에러 응답 DTO
 * @param errorCode 직접 정의한 에러 코드 (ErrorCode Enum)
 * @param message 에러 메시지
 */
data class ErrorResponse(
    val errorCode: String,
    val message: String,
) {
    companion object {
        fun of(code: ErrorCode): ErrorResponse {
            return ErrorResponse(
                errorCode = code.name,
                message = code.message
            )
        }
    }
}
