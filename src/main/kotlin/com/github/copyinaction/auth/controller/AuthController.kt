package com.github.copyinaction.auth.controller

import com.github.copyinaction.auth.dto.LoginRequest
import com.github.copyinaction.auth.dto.RefreshTokenRequest
import com.github.copyinaction.auth.dto.SignupRequest
import com.github.copyinaction.auth.dto.UserResponse
import com.github.copyinaction.auth.dto.EmailVerificationRequest
import com.github.copyinaction.auth.dto.OtpConfirmationRequest // Import the new DTO
import org.springframework.http.HttpStatus
import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.auth.service.AuthService
import com.github.copyinaction.auth.service.CookieService
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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "auth", description = "인증 API - 사용자 회원가입, 로그인 및 이메일 인증")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val cookieService: CookieService
) {

    @Operation(summary = "회원가입", description = "이메일 인증(OTP 확인)이 완료된 후 새로운 사용자를 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "회원가입 성공"),
        ApiResponse(
            responseCode = "400", description = "잘못된 입력 값 또는 이메일 인증이 완료되지 않음",
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
        @RequestHeader(value = "Host", required = false) host: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val authTokenInfo = authService.login(request)
        cookieService.addAuthCookies(response, authTokenInfo, origin, host)
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
        @RequestHeader(value = "Host", required = false) host: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val authTokenInfo = authService.refresh(request.refreshToken)
        cookieService.addAuthCookies(response, authTokenInfo, origin, host)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "이메일 인증 요청 (OTP 발송)", description = "회원가입을 위한 이메일 인증번호(OTP)를 요청합니다. 지정된 이메일 주소로 OTP가 발송됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "인증 OTP 이메일 전송 성공"),
        ApiResponse(
            responseCode = "409", description = "이미 가입된 이메일",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/email-verification/request")
    fun requestEmailVerification(@Valid @RequestBody request: EmailVerificationRequest): ResponseEntity<Void> {
        authService.requestEmailVerification(request.email)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "OTP 확인", description = "이메일로 발송된 6자리 OTP를 확인합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "OTP 확인 성공"),
        ApiResponse(
            responseCode = "400", description = "유효하지 않거나 만료된 OTP",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/confirm-otp")
    fun confirmOtp(@Valid @RequestBody request: OtpConfirmationRequest): ResponseEntity<Void> {
        authService.confirmOtp(request.email, request.otp)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "로그아웃", description = "사용자 세션을 종료하고 인증 쿠키를 삭제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그아웃 성공")
    )
    @PostMapping("/logout")
    fun logout(
        @RequestHeader(value = "Origin", required = false) origin: String?,
        @RequestHeader(value = "Host", required = false) host: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        cookieService.clearAuthCookies(response, origin, host)
        return ResponseEntity.ok().build()
    }
}