package io.github.lucaspaixaodev.poppin.domain.user.repository

interface UserGraphRepository {
    fun create(userId: String)

    fun existsById(userId: String): Boolean
}
