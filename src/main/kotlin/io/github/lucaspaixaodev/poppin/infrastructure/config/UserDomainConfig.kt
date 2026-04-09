package io.github.lucaspaixaodev.poppin.infrastructure.config

import io.github.lucaspaixaodev.poppin.domain.user.gateway.AuthGateway
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserGraphRepository
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserRepository
import io.github.lucaspaixaodev.poppin.domain.user.service.CreateAuthUserService
import io.github.lucaspaixaodev.poppin.domain.user.service.CreateUserGraphService
import io.github.lucaspaixaodev.poppin.domain.user.service.CreateUserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserDomainConfig {
    @Bean
    fun createUserService(userRepository: UserRepository): CreateUserService = CreateUserService(userRepository)

    @Bean
    fun createAuthUserService(authGateway: AuthGateway): CreateAuthUserService = CreateAuthUserService(authGateway)

    @Bean
    fun createUserGraphService(userGraphRepository: UserGraphRepository): CreateUserGraphService =
        CreateUserGraphService(userGraphRepository)
}
