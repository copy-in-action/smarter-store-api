package com.github.copyinaction.admin.controller

import com.github.copyinaction.admin.dto.AdminLoginRequest
import com.github.copyinaction.admin.dto.AdminResponse
import com.github.copyinaction.admin.dto.AdminSignupRequest
import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.admin.service.AdminAuthService
import com.github.copyinaction.auth.service.CookieService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "admin-auth", description = "관리자 인증 API - 관리자 회원가입 및 로그인 (인증 불필요)")
@SecurityRequirements
@RestController
@RequestMapping("/api/admin/auth")
class AdminAuthController(
    private val adminAuthService: AdminAuthService,
    private val cookieService: CookieService
) {

    @Operation(summary = "관리자 회원가입", description = "새로운 관리자를 생성합니다.\n\n**권한: 누구나**")
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
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminResponse.Companion.from(admin))
    }

    @Operation(summary = "관리자 로그인", description = "로그인 ID와 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.\n\n**권한: 누구나**")
    @ApiResponses(
        ApiResponse(
            responseCode = "200", description = "로그인 성공 (Access Token 쿠키로 발급)",
            headers = [
                Header(name = HttpHeaders.SET_COOKIE, description = "Access Token 쿠키 (HttpOnly, Secure, SameSite=Lax)")
            ]
        ),
        ApiResponse(
            responseCode = "401", description = "인증 실패 (로그인 ID 또는 비밀번호 불일치)",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AdminLoginRequest,
        @RequestHeader(value = "Origin", required = false) origin: String?,
        @RequestHeader(value = "Host", required = false) host: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val authTokenInfo = adminAuthService.login(request)
        cookieService.addAdminAuthCookie(response, authTokenInfo, origin, host)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "관리자 로그아웃", description = "관리자 세션을 종료하고 인증 쿠키를 삭제합니다.\n\n**권한: 누구나**")
    @ApiResponses(
        ApiResponse(
            responseCode = "200", description = "로그아웃 성공 (Access Token 쿠키 삭제)",
            headers = [
                Header(name = HttpHeaders.SET_COOKIE, description = "Access Token 쿠키 (삭제)")
            ]
        )
    )
    @PostMapping("/logout")
    fun logout(
        @RequestHeader(value = "Origin", required = false) origin: String?,
        @RequestHeader(value = "Host", required = false) host: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        cookieService.clearAdminAuthCookie(response, origin, host)
        return ResponseEntity.ok().build()
    }
}