package io.github.lucaspaixaodev.poppin.infrastructure.output.database.neo4j.user

import io.github.lucaspaixaodev.poppin.domain.exception.RepositoryException
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserGraphRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class Neo4jUserRepository(
    private val neo4jRepository: UserNeo4jRepository
) : UserGraphRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(userId: String) {
        log.info("Persisting user node - id={}", userId)
        try {
            neo4jRepository.save(UserNode(userId))
            log.info("User node persisted - id={}", userId)
        } catch (e: Exception) {
            log.error("Failed to persist user node - id={} error={}", userId, e.message)
            throw RepositoryException.CreationFailed("Failed to create user node in Neo4j", e.message ?: "unknown error")
        }
    }

    override fun existsById(userId: String): Boolean {
        val exists = neo4jRepository.existsById(userId)
        log.info("existsById (graph) - id={} result={}", userId, exists)
        return exists
    }
}
