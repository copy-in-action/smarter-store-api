package com.github.copyinaction.common.exception

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestTimeoutException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * JSON 파싱 실패 또는 Enum 타입 불일치 등으로 인한 메시지 읽기 오류 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    protected fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.error("handleHttpMessageNotReadableException", e)

        val cause = e.cause
        if (cause is InvalidFormatException && cause.targetType.isEnum) {
            val invalidValue = cause.value
            val allowedValues = cause.targetType.enumConstants.joinToString(", ")
            val errorMessage = "입력된 값 '${invalidValue}'은(는) 유효하지 않습니다. 허용된 값: [$allowedValues]"
            val response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errorMessage)
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response)
        }

        val response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE)
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response)
    }

    /**
     * @Valid 어노테이션을 통한 유효성 검증에 실패했을 때 처리하는 핸들러
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.error("handleMethodArgumentNotValidException", e)
        val response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE)
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response)
    }

    /**
     * 직접 정의한 비즈니스 예외를 처리하는 핸들러
     * 에러코드의 logLevel에 따라 적절한 로그 레벨로 기록
     */
    @ExceptionHandler(CustomException::class)
    protected fun handleCustomException(e: CustomException): ResponseEntity<ErrorResponse> {
        when (e.errorCode.logLevel) {
            LogLevel.ERROR -> logger.error("handleCustomException", e)
            LogLevel.WARN -> logger.warn("handleCustomException: {}", e.message)
            LogLevel.DEBUG -> logger.debug("handleCustomException: {}", e.message)
        }
        val response = ErrorResponse.of(e)
        return ResponseEntity.status(e.errorCode.status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response)
    }

    /**
     * 접근 권한이 없을 때 처리하는 핸들러
     */
    @ExceptionHandler(AccessDeniedException::class)
    protected fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
        logger.error("handleAccessDeniedException", e)
        val response = ErrorResponse.of(ErrorCode.ACCESS_DENIED)
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response)
    }

    /**
     * 로그인 실패 (이메일 또는 비밀번호 불일치) 처리 핸들러
     */
    @ExceptionHandler(BadCredentialsException::class)
    protected fun handleBadCredentialsException(e: BadCredentialsException): ResponseEntity<ErrorResponse> {
        logger.warn("handleBadCredentialsException: {}", e.message)
        val response = ErrorResponse.of(ErrorCode.LOGIN_FAILED)
        return ResponseEntity.status(ErrorCode.LOGIN_FAILED.status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response)
    }

    /**
     * SSE 연결 타임아웃 처리 (text/event-stream에서 ErrorResponse 직렬화 불가 방지)
     */
    @ExceptionHandler(AsyncRequestTimeoutException::class)
    protected fun handleAsyncRequestTimeoutException(e: AsyncRequestTimeoutException): ResponseEntity<Void> {
        logger.debug("SSE 연결 타임아웃: {}", e.message)
        return ResponseEntity.noContent().build()
    }

    /**
     * 위에 명시되지 않은 모든 예외를 처리하는 핸들러
     */
    @ExceptionHandler(Exception::class)
    protected fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("handleException", e)
        val response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)
        return ResponseEntity.internalServerError()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response)
    }
}
