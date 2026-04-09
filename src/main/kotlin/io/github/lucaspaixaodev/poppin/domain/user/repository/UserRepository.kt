package io.github.lucaspaixaodev.poppin.domain.user.repository

import io.github.lucaspaixaodev.poppin.domain.user.User

interface UserRepository {
    fun create(user: User)

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean
}
