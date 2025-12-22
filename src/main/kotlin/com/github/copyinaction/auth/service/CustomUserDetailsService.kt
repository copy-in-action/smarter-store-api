package com.github.copyinaction.auth.service

import com.github.copyinaction.auth.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("User not found with email: $username") }

        return CustomUserDetails(
            id = user.id,
            username = user.email,
            password = user.passwordHash,
            authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        )
    }
}
