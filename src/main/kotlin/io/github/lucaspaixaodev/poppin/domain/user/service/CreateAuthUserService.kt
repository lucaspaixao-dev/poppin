package io.github.lucaspaixaodev.poppin.domain.user.service

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.gateway.AuthGateway

class CreateAuthUserService(
    private val authGateway: AuthGateway
) {

    fun execute(user: User) {
        if (authGateway.existsByEmail(user.email)) {
            throw UserException.AlreadyExists(user.email)
        }
        authGateway.createUser(user)
    }
}
