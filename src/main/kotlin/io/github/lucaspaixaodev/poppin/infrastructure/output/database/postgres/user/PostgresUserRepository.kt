package io.github.lucaspaixaodev.poppin.infrastructure.output.database.postgres.user

import io.github.lucaspaixaodev.poppin.domain.exception.RepositoryException
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Repository

@Repository
class PostgresUserRepository(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(user: User) {
        log.info("Persisting user - id={} email={}", user.id, user.email)
        try {
            jpaRepository.save(UserEntity.fromDomain(user))
            log.info("User persisted - id={}", user.id)
        } catch (e: DataAccessException) {
            log.error("Failed to persist user - id={} error={}", user.id, e.message)
            throw RepositoryException.CreationFailed("Failed to create a new user", e)
        }
    }

    override fun existsByEmail(email: String): Boolean {
        val exists = jpaRepository.existsByEmail(email)
        log.info("existsByEmail - email={} result={}", email, exists)
        return exists
    }

    override fun existsByUsername(username: String): Boolean {
        val exists = jpaRepository.existsByUsername(username)
        log.info("existsByUsername - username={} result={}", username, exists)
        return exists
    }
}
