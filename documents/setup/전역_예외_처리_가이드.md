# 전역 예외 처리 가이드

이 문서는 REST API의 예외 처리 일관성과 가독성을 높이기 위해 구현된 전역 예외 처리 메커니즘에 대해 상세히 설명합니다.

## 1. 개요

프로젝트는 `@RestControllerAdvice`를 활용하여 애플리케이션 전반에서 발생하는 예외를 한곳에서 처리하고, 클라이언트에게 일관된 형식의 에러 응답(ErrorResponse)을 제공합니다. 이를 통해 클라이언트는 예상치 못한 에러에 대해 항상 동일한 구조의 응답을 받게 됩니다.

## 2. 주요 구성 요소

전역 예외 처리는 다음 파일들로 구성됩니다.

### 2.1. `ErrorCode.kt` (`exception/ErrorCode.kt`)

*   **역할**: 애플리케이션에서 발생할 수 있는 모든 비즈니스 예외에 대한 고유한 에러 코드, HTTP 상태 코드, 그리고 사용자에게 보여줄 메시지를 정의하는 `Enum` 클래스입니다.
*   **특징**: 에러 코드를 중앙 집중식으로 관리하여 일관성을 유지하고, 코드만 보고도 어떤 에러인지 파악하기 쉽게 합니다.

```kotlin
// ErrorCode.kt 예시
package com.github.copyinaction.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생했습니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."),

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
}
```

### 2.2. `ErrorResponse.kt` (`exception/ErrorResponse.kt`)

*   **역할**: 클라이언트에게 반환될 에러 응답의 표준 형식을 정의하는 DTO 클래스입니다. `ErrorCode`를 받아 `errorCode` 문자열과 `message`를 포함합니다.
*   **특징**: 모든 에러 응답이 일관된 구조를 가지므로 클라이언트 개발을 용이하게 합니다.

```kotlin
// ErrorResponse.kt 예시
package com.github.copyinaction.exception

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
```

### 2.3. `CustomException.kt` (`exception/CustomException.kt`)

*   **역할**: 애플리케이션 내에서 발생하는 비즈니스 로직 상의 특정 예외를 나타내는 커스텀 예외 클래스입니다. `ErrorCode` 객체를 인자로 받아 해당 에러의 정보를 포함합니다.
*   **특징**: 서비스 계층 등에서 `throw CustomException(ErrorCode.PRODUCT_NOT_FOUND)` 와 같이 사용되어 명확한 비즈니스 예외를 전달합니다.

```kotlin
// CustomException.kt 예시
package com.github.copyinaction.exception

class CustomException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)
```

### 2.4. `GlobalExceptionHandler.kt` (`exception/GlobalExceptionHandler.kt`)

*   **역할**: `@RestControllerAdvice` 어노테이션을 사용하여 애플리케이션 전반에서 발생하는 예외를 중앙 집중식으로 가로채 처리하는 핸들러입니다.
*   **특징**:
    -   `@ExceptionHandler(MethodArgumentNotValidException::class)`: `@Valid` 어노테이션을 통한 유효성 검증 실패 시 발생하는 예외를 처리하여 `INVALID_INPUT_VALUE` 에러를 반환합니다.
    -   `@ExceptionHandler(CustomException::class)`: `CustomException`을 catch하여 내부에 담긴 `ErrorCode`에 따라 적절한 HTTP 상태 코드와 `ErrorResponse`를 반환합니다.
    -   `@ExceptionHandler(Exception::class)`: 위에 명시되지 않은 모든 예측 불가능한 예외(서버 내부 에러)를 처리하여 `INTERNAL_SERVER_ERROR`를 반환합니다.

```kotlin
// GlobalExceptionHandler.kt 예시
package com.github.copyinaction.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice // 전역 예외 처리기임을 명시
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
     * 위에 명시되지 않은 모든 예외를 처리하는 핸들러 (최후의 보루)
     */
    @ExceptionHandler(Exception::class)
    protected fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("handleException", e)
        val response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)
        return ResponseEntity.internalServerError().body(response)
    }
}
```
