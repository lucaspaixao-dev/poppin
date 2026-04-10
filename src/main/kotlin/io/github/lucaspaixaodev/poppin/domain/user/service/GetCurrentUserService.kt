package io.github.lucaspaixaodev.poppin.domain.user.service

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserRepository

class GetCurrentUserService(
    private val userRepository: UserRepository,
) {
    fun execute(id: String): User = userRepository.findById(id) ?: throw UserException.NotFound(id)
}
