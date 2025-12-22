package com.github.copyinaction.common.exception

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "API 에러 응답")
data class ErrorResponse(
    @Schema(description = "에러 코드", example = "INVALID_INPUT")
    val errorCode: String,

    @Schema(description = "에러 메시지", example = "잘못된 입력 값입니다.")
    val message: String,
) {
    companion object {
        fun of(code: ErrorCode): ErrorResponse {
            return ErrorResponse(
                errorCode = code.name,
                message = code.message
            )
        }

        fun of(code: ErrorCode, message: String): ErrorResponse {
            return ErrorResponse(
                errorCode = code.name,
                message = message
            )
        }

        fun of(exception: CustomException): ErrorResponse {
            return ErrorResponse(
                errorCode = exception.errorCode.name,
                message = exception.message ?: exception.errorCode.message
            )
        }
    }
}
