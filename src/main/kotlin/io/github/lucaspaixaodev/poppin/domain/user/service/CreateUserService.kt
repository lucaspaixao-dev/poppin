package io.github.lucaspaixaodev.poppin.domain.user.service

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.input.CreateUserInput
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserRepository

class CreateUserService(
    private val userRepository: UserRepository,
) {

    fun execute(input: CreateUserInput): User {
        if (userRepository.existsByEmail(input.email)) {
            throw UserException.AlreadyExists(input.email)
        }
        if (userRepository.existsByUsername(input.username)) {
            throw UserException.UsernameAlreadyExists(input.username)
        }

        val user = User.create(input = input).getOrThrow()

        userRepository.create(user)
        return user
    }
}