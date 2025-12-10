package com.github.copyinaction.auth.controller

import com.github.copyinaction.auth.dto.LoginRequest
import com.github.copyinaction.auth.dto.RefreshTokenRequest
import com.github.copyinaction.auth.dto.SignupRequest
import com.github.copyinaction.auth.dto.UserResponse
import com.github.copyinaction.auth.dto.EmailVerificationRequest
import com.github.copyinaction.auth.dto.EmailVerificationConfirm
import org.springframework.http.HttpStatus
import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

@Tag(name = "auth", description = "인증 API - 사용자 회원가입, 로그인 및 이메일 인증")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {

    @Operation(summary = "회원가입", description = "새로운 사용자를 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "회원가입 성공"),
        ApiResponse(
            responseCode = "400", description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "이미 가입된 이메일",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<UserResponse> {
        val user = authService.signup(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user))
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그인 성공 (Access Token 및 Refresh Token 쿠키로 발급)",
            headers = [
                io.swagger.v3.oas.annotations.headers.Header(name = HttpHeaders.SET_COOKIE, description = "Access Token 쿠키 (HttpOnly, Secure, SameSite=Lax)"),
                io.swagger.v3.oas.annotations.headers.Header(name = HttpHeaders.SET_COOKIE, description = "Refresh Token 쿠키 (HttpOnly, Secure, SameSite=Strict)")
            ]),
        ApiResponse(
            responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 불일치)",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        @RequestHeader(value = "Origin", required = false) origin: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val authTokenInfo = authService.login(request)
        val isLocalhost = origin?.contains("localhost") == true || origin?.contains("127.0.0.1") == true

        response.addHeader(HttpHeaders.SET_COOKIE, createAccessTokenCookie(authTokenInfo.accessToken, authTokenInfo.accessTokenExpiresIn, isLocalhost).toString())
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(authTokenInfo.refreshToken, authTokenInfo.accessTokenExpiresIn * 7, isLocalhost).toString()) // Refresh Token은 Access Token의 7배 유효기간 (임시)

        return ResponseEntity.ok().build()
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "토큰 갱신 성공 (새로운 Access Token 및 Refresh Token 쿠키로 발급)",
            headers = [
                io.swagger.v3.oas.annotations.headers.Header(name = HttpHeaders.SET_COOKIE, description = "새로운 Access Token 쿠키 (HttpOnly, Secure, SameSite=Lax)"),
                io.swagger.v3.oas.annotations.headers.Header(name = HttpHeaders.SET_COOKIE, description = "새로운 Refresh Token 쿠키 (HttpOnly, Secure, SameSite=Strict)")
            ]),
        ApiResponse(
            responseCode = "401", description = "유효하지 않거나 만료된 리프레시 토큰",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
        @RequestHeader(value = "Origin", required = false) origin: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val authTokenInfo = authService.refresh(request.refreshToken)
        val isLocalhost = origin?.contains("localhost") == true || origin?.contains("127.0.0.1") == true

        response.addHeader(HttpHeaders.SET_COOKIE, createAccessTokenCookie(authTokenInfo.accessToken, authTokenInfo.accessTokenExpiresIn, isLocalhost).toString())
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(authTokenInfo.refreshToken, authTokenInfo.accessTokenExpiresIn * 7, isLocalhost).toString()) // Refresh Token은 Access Token의 7배 유효기간 (임시)

        return ResponseEntity.ok().build()
    }

    @Operation(summary = "이메일 인증 요청", description = "지정된 이메일 주소로 인증 이메일을 보냅니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "인증 이메일 전송 성공"),
        ApiResponse(
            responseCode = "404", description = "등록되지 않은 이메일",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "이미 인증된 이메일",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/email-verification/request")
    fun requestEmailVerification(@Valid @RequestBody request: EmailVerificationRequest): ResponseEntity<Void> {
        authService.requestEmailVerification(request.email)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "이메일 인증 확인", description = "받은 토큰으로 이메일 인증을 완료합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
        ApiResponse(
            responseCode = "400", description = "유효하지 않거나 만료된 토큰",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "이미 인증된 이메일",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/email-verification/confirm")
    fun confirmEmailVerification(@Valid @RequestBody request: EmailVerificationConfirm): ResponseEntity<Void> {
        authService.confirmEmailVerification(request.token)
        return ResponseEntity.ok().build()
    }

    private fun createAccessTokenCookie(accessToken: String, expiresIn: Long, isLocalhost: Boolean): ResponseCookie {
        return ResponseCookie.from("accessToken", accessToken)
            .httpOnly(true)
            .secure(!isLocalhost)
            .path("/")
            .maxAge(Duration.ofSeconds(expiresIn))
            .sameSite(if (isLocalhost) "None" else "Lax")
            .build()
    }

    private fun createRefreshTokenCookie(refreshToken: String, expiresIn: Long, isLocalhost: Boolean): ResponseCookie {
        return ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(!isLocalhost)
            .path("/")
            .maxAge(Duration.ofSeconds(expiresIn))
            .sameSite(if (isLocalhost) "None" else "Strict")
            .build()
    }

    @Operation(summary = "로그아웃", description = "사용자 세션을 종료하고 인증 쿠키를 삭제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그아웃 성공")
    )
    @PostMapping("/logout")
    fun logout(
        @RequestHeader(value = "Origin", required = false) origin: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val isLocalhost = origin?.contains("localhost") == true || origin?.contains("127.0.0.1") == true

        // Access Token 쿠키 삭제
        val accessTokenCookie = ResponseCookie.from("accessToken", "")
            .httpOnly(true)
            .secure(!isLocalhost)
            .path("/")
            .maxAge(0) // 즉시 만료
            .sameSite(if (isLocalhost) "None" else "Lax")
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())

        // Refresh Token 쿠키 삭제
        val refreshTokenCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(!isLocalhost)
            .path("/")
            .maxAge(0) // 즉시 만료
            .sameSite(if (isLocalhost) "None" else "Strict")
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())

        return ResponseEntity.ok().build()
    }
}