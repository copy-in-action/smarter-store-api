package com.github.copyinaction.service

import com.github.copyinaction.config.jwt.JwtTokenProvider
import com.github.copyinaction.dto.LoginRequest
import com.github.copyinaction.dto.SignupRequest
import com.github.copyinaction.dto.TokenResponse
import com.github.copyinaction.exception.CustomException
import com.github.copyinaction.exception.ErrorCode
import com.github.copyinaction.repository.UserRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun signup(request: SignupRequest) {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }
        val user = request.toEntity(passwordEncoder)
        userRepository.save(user)
    }

    @Transactional
    fun login(request: LoginRequest): TokenResponse {
        val authenticationToken = UsernamePasswordAuthenticationToken(request.email, request.password)
        val authentication = authenticationManagerBuilder.`object`.authenticate(authenticationToken)
        val accessToken = jwtTokenProvider.createAccessToken(authentication)

        return TokenResponse(accessToken)
    }
}
