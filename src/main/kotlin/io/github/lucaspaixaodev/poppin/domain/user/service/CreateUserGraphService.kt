package io.github.lucaspaixaodev.poppin.domain.user.service

import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserGraphRepository

class CreateUserGraphService(
    private val userGraphRepository: UserGraphRepository
) {

    fun execute(user: User) {
        if (userGraphRepository.existsById(user.id)) return
        userGraphRepository.create(user.id)
    }
}
