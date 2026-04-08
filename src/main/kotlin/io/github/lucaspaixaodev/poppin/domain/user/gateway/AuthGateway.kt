package io.github.lucaspaixaodev.poppin.domain.user.gateway

import io.github.lucaspaixaodev.poppin.domain.user.User

interface AuthGateway {

    fun createUser(user: User): String

    fun existsByEmail(email: String): Boolean

    fun enableUser(uid: String)

    fun disableUser(uid: String)
}
