package io.github.lucaspaixaodev.poppin.infrastructure.output.database.postgres.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserEntity, String> {
    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean
}
