package com.github.copyinaction.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * @Valid 어노테이션을 통한 유효성 검증에 실패했을 때 처리하는 핸들러
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.error("handleMethodArgumentNotValidException", e)
        val response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE)
        return ResponseEntity.badRequest().body(response)
    }

    /**
     * 직접 정의한 비즈니스 예외를 처리하는 핸들러
     */
    @ExceptionHandler(CustomException::class)
    protected fun handleCustomException(e: CustomException): ResponseEntity<ErrorResponse> {
        logger.error("handleCustomException", e)
        val response = ErrorResponse.of(e.errorCode)
        return ResponseEntity.status(e.errorCode.status).body(response)
    }

    /**
     * 접근 권한이 없을 때 처리하는 핸들러
     */
    @ExceptionHandler(AccessDeniedException::class)
    protected fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
        logger.error("handleAccessDeniedException", e)
        val response = ErrorResponse.of(ErrorCode.ACCESS_DENIED)
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.status).body(response)
    }
    
    /**
     * 위에 명시되지 않은 모든 예외를 처리하는 핸들러
     */
    @ExceptionHandler(Exception::class)
    protected fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("handleException", e)
        val response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)
        return ResponseEntity.internalServerError().body(response)
    }
}
