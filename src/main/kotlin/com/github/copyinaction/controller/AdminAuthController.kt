package com.github.copyinaction.controller

import com.github.copyinaction.dto.AdminLoginRequest
import com.github.copyinaction.dto.AdminResponse
import com.github.copyinaction.dto.AdminSignupRequest
import com.github.copyinaction.dto.AuthTokenInfo
import com.github.copyinaction.exception.ErrorResponse
import com.github.copyinaction.service.AdminAuthService
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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

@Tag(name = "admin-auth", description = "관리자 인증 API - 관리자 회원가입 및 로그인 (인증 불필요)")
@SecurityRequirements
@RestController
@RequestMapping("/api/admin/auth")
class AdminAuthController(
    private val adminAuthService: AdminAuthService,
) {

    @Operation(summary = "관리자 회원가입", description = "새로운 관리자를 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "관리자 회원가입 성공"),
        ApiResponse(
            responseCode = "400", description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "이미 존재하는 로그인 ID",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: AdminSignupRequest): ResponseEntity<AdminResponse> {
        val admin = adminAuthService.signup(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminResponse.from(admin))
    }

    @Operation(summary = "관리자 로그인", description = "로그인 ID와 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그인 성공 (Access Token 쿠키로 발급)",
            headers = [
                io.swagger.v3.oas.annotations.headers.Header(name = HttpHeaders.SET_COOKIE, description = "Access Token 쿠키 (HttpOnly, Secure, SameSite=Lax)")
            ]),
        ApiResponse(
            responseCode = "401", description = "인증 실패 (로그인 ID 또는 비밀번호 불일치)",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AdminLoginRequest, response: HttpServletResponse): ResponseEntity<Void> {
        val authTokenInfo = adminAuthService.login(request)

        response.addHeader(HttpHeaders.SET_COOKIE, createAccessTokenCookie(authTokenInfo.accessToken, authTokenInfo.accessTokenExpiresIn).toString())

        return ResponseEntity.ok().build()
    }

    private fun createAccessTokenCookie(accessToken: String, expiresIn: Long): ResponseCookie {
        return ResponseCookie.from("accessToken", accessToken)
            .httpOnly(true)
            .secure(true) // HTTPS에서만 전송
            .path("/")
            .maxAge(Duration.ofSeconds(expiresIn))
            .sameSite("Lax") // CSRF 보호
            .build()
    }

    @Operation(summary = "관리자 로그아웃", description = "관리자 세션을 종료하고 인증 쿠키를 삭제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그아웃 성공 (Access Token 쿠키 삭제)",
            headers = [
                io.swagger.v3.oas.annotations.headers.Header(name = HttpHeaders.SET_COOKIE, description = "Access Token 쿠키 (삭제)")
            ])
    )
    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        // Access Token 쿠키 삭제
        val accessTokenCookie = ResponseCookie.from("accessToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0) // 즉시 만료
            .sameSite("Lax")
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())

        return ResponseEntity.ok().build()
    }
}
