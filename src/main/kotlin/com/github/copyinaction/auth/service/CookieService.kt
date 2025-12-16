package com.github.copyinaction.auth.service

import com.github.copyinaction.auth.dto.AuthTokenInfo
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * ## CookieService
 *
 * 이 서비스는 애플리케이션의 모든 HTTP 쿠키 관련 작업을 중앙에서 처리합니다.
 * JWT 기반 인증에 사용되는 Access Token 및 Refresh Token 쿠키의 생성, 추가, 삭제 로직을 캡슐화하여
 * 컨트롤러 계층의 중복 코드를 제거하고 쿠키 관리를 일관되게 유지합니다.
 *
 * ### 주요 특징:
 * - **환경별 쿠키 설정:** 로컬 개발 환경(`localhost`, `127.0.0.1`)과 프로덕션 환경을 구분하여 `Secure`, `SameSite`, `Domain` 속성을 동적으로 설정합니다.
 *   - 로컬 환경: `Secure=false`, `SameSite=None`, `Domain` 미설정
 *   - 프로덕션 환경: `Secure=true`, `SameSite` 정책 (`Lax` 또는 `Strict`), `Domain` 설정 (application.yml에서 주입)
 * - **중앙 집중식 관리:** 인증 및 로그아웃 시 필요한 모든 쿠키 작업을 이 서비스 하나로 처리하여 코드의 응집도를 높입니다.
 * - **확장성:** 새로운 쿠키 정책이나 속성 변경이 필요할 경우, 이 서비스만 수정하면 됩니다.
 */
@Service
class CookieService(
    @Value("\${app.cookie.domain:}") private val cookieDomain: String // application.yml에서 쿠키 도메인 값을 주입받습니다.
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)

    init {
        logger.info("CookieService initialized with cookieDomain: '$cookieDomain'")
    }

    /**
     * 일반 사용자(User)의 Access Token 및 Refresh Token 쿠키를 HTTP 응답 헤더에 추가합니다.
     *
     * @param response HTTP 응답 객체
     * @param authTokenInfo 발급된 JWT 토큰 정보 (Access Token, Refresh Token, 만료 시간 등)
     * @param origin 요청의 Origin 헤더 (로컬 환경 감지에 사용)
     * @param host 요청의 Host 헤더 (Origin이 없을 때 fallback으로 사용)
     */
    fun addAuthCookies(response: HttpServletResponse, authTokenInfo: AuthTokenInfo, origin: String?, host: String? = null) {
        val isLocalhost = isLocalhost(origin, host)
        logger.debug("addAuthCookies - origin: '$origin', host: '$host', isLocalhost: $isLocalhost, cookieDomain: '$cookieDomain'")
        response.addHeader(HttpHeaders.SET_COOKIE, createAccessTokenCookie(authTokenInfo.accessToken, authTokenInfo.accessTokenExpiresIn, isLocalhost).toString())
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(authTokenInfo.refreshToken, authTokenInfo.accessTokenExpiresIn * 7, isLocalhost).toString())
    }

    /**
     * 관리자(Admin)의 Access Token 쿠키를 HTTP 응답 헤더에 추가합니다.
     * 관리자는 Refresh Token을 사용하지 않으므로 Access Token만 추가합니다.
     *
     * @param response HTTP 응답 객체
     * @param authTokenInfo 발급된 JWT 토큰 정보 (Access Token, 만료 시간 등)
     * @param origin 요청의 Origin 헤더 (로컬 환경 감지에 사용)
     * @param host 요청의 Host 헤더 (Origin이 없을 때 fallback으로 사용)
     */
    fun addAdminAuthCookie(response: HttpServletResponse, authTokenInfo: AuthTokenInfo, origin: String?, host: String? = null) {
        val isLocalhost = isLocalhost(origin, host)
        response.addHeader(HttpHeaders.SET_COOKIE, createAccessTokenCookie(authTokenInfo.accessToken, authTokenInfo.accessTokenExpiresIn, isLocalhost).toString())
    }

    /**
     * 일반 사용자(User)의 Access Token 및 Refresh Token 쿠키를 만료시켜 삭제합니다.
     *
     * @param response HTTP 응답 객체
     * @param origin 요청의 Origin 헤더 (로컬 환경 감지에 사용)
     * @param host 요청의 Host 헤더 (Origin이 없을 때 fallback으로 사용)
     */
    fun clearAuthCookies(response: HttpServletResponse, origin: String?, host: String? = null) {
        val isLocalhost = isLocalhost(origin, host)
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("accessToken", isLocalhost, "Lax").toString())
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("refreshToken", isLocalhost, "Strict").toString())
    }

    /**
     * 관리자(Admin)의 Access Token 쿠키를 만료시켜 삭제합니다.
     *
     * @param response HTTP 응답 객체
     * @param origin 요청의 Origin 헤더 (로컬 환경 감지에 사용)
     * @param host 요청의 Host 헤더 (Origin이 없을 때 fallback으로 사용)
     */
    fun clearAdminAuthCookie(response: HttpServletResponse, origin: String?, host: String? = null) {
        val isLocalhost = isLocalhost(origin, host)
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("accessToken", isLocalhost, "Lax").toString())
    }

    /**
     * Access Token을 위한 ResponseCookie 객체를 생성합니다.
     *
     * @param token Access Token 문자열
     * @param maxAge 쿠키의 유효 기간 (초 단위)
     * @param isLocalhost 로컬 환경 여부
     * @return 설정된 ResponseCookie 객체
     */
    private fun createAccessTokenCookie(token: String, maxAge: Long, isLocalhost: Boolean): ResponseCookie {
        val builder = ResponseCookie.from("accessToken", token)
            .httpOnly(true) // JavaScript에서 접근 불가
            .secure(!isLocalhost) // HTTPS에서만 전송 (로컬이 아니면 Secure)
            .path("/") // 모든 경로에서 유효
            .maxAge(Duration.ofSeconds(maxAge)) // 유효 기간 설정
            .sameSite(getSameSite("Lax", isLocalhost)) // CSRF 방어 정책

        // 로컬 환경이 아니고 cookieDomain이 비어있지 않은 경우에만 Domain 속성 설정
        if (!isLocalhost && cookieDomain.isNotBlank()) {
            builder.domain(cookieDomain)
        }
        return builder.build()
    }

    /**
     * Refresh Token을 위한 ResponseCookie 객체를 생성합니다.
     * Access Token 쿠키와 유사하나, SameSite 정책이 "Strict"로 더 엄격합니다.
     *
     * @param token Refresh Token 문자열
     * @param maxAge 쿠키의 유효 기간 (초 단위)
     * @param isLocalhost 로컬 환경 여부
     * @return 설정된 ResponseCookie 객체
     */
    private fun createRefreshTokenCookie(token: String, maxAge: Long, isLocalhost: Boolean): ResponseCookie {
        val builder = ResponseCookie.from("refreshToken", token)
            .httpOnly(true) // JavaScript에서 접근 불가
            .secure(!isLocalhost) // HTTPS에서만 전송 (로컬이 아니면 Secure)
            .path("/") // 모든 경로에서 유효
            .maxAge(Duration.ofSeconds(maxAge)) // 유효 기간 설정
            .sameSite(getSameSite("Strict", isLocalhost)) // CSRF 방어 정책 (더 엄격)

        // 로컬 환경이 아니고 cookieDomain이 비어있지 않은 경우에만 Domain 속성 설정
        if (!isLocalhost && cookieDomain.isNotBlank()) {
            builder.domain(cookieDomain)
        }
        return builder.build()
    }

    /**
     * 지정된 이름의 만료된(삭제될) ResponseCookie 객체를 생성합니다.
     * `maxAge`를 0으로 설정하여 즉시 만료되도록 합니다.
     *
     * @param name 삭제할 쿠키의 이름
     * @param isLocalhost 로컬 환경 여부
     * @param sameSitePolicy 만료된 쿠키에 적용할 SameSite 정책 (기본값 "Lax")
     * @return 설정된 ResponseCookie 객체
     */
    private fun createExpiredCookie(name: String, isLocalhost: Boolean, sameSitePolicy: String): ResponseCookie {
        val builder = ResponseCookie.from(name, "") // 값은 비워둡니다.
            .httpOnly(true) // JavaScript에서 접근 불가
            .secure(!isLocalhost) // HTTPS에서만 전송 (로컬이 아니면 Secure)
            .path("/") // 모든 경로에서 유효
            .maxAge(0) // 즉시 만료
            .sameSite(getSameSite(sameSitePolicy, isLocalhost)) // CSRF 방어 정책

        // 로컬 환경이 아니고 cookieDomain이 비어있지 않은 경우에만 Domain 속성 설정
        if (!isLocalhost && cookieDomain.isNotBlank()) {
            builder.domain(cookieDomain)
        }
        return builder.build()
    }
    
    /**
     * 환경에 따라 `SameSite` 정책을 반환합니다.
     * 로컬 환경일 경우 "None"을 반환하여 Cross-Site 요청을 허용하고,
     * 그 외 환경에서는 지정된 정책을 따릅니다.
     *
     * @param policy 기본 SameSite 정책 ("Lax" 또는 "Strict")
     * @param isLocalhost 로컬 환경 여부
     * @return 적용할 SameSite 정책 문자열
     */
    private fun getSameSite(policy: String, isLocalhost: Boolean): String {
        return if (isLocalhost) "None" else policy
    }

    /**
     * 요청의 Origin 헤더를 분석하여 로컬 개발 환경인지 여부를 판단합니다.
     * Origin이 없는 경우(Swagger 등) Host 헤더로 fallback합니다.
     *
     * @param origin 요청의 Origin 헤더 문자열
     * @param host 요청의 Host 헤더 문자열
     * @return Origin 또는 Host가 localhost/127.0.0.1을 포함하면 true, 아니면 false
     */
    private fun isLocalhost(origin: String?, host: String?): Boolean {
        val target = origin ?: host
        return target?.contains("localhost") == true || target?.contains("127.0.0.1") == true
    }
}
