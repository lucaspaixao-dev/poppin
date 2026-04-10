package io.github.lucaspaixaodev.poppin.integration.user

import io.github.lucaspaixaodev.poppin.infrastructure.output.database.neo4j.user.UserNeo4jRepository
import io.github.lucaspaixaodev.poppin.infrastructure.output.database.postgres.user.UserJpaRepository
import io.github.lucaspaixaodev.poppin.integration.AbstractIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class GetCurrentUserIT : AbstractIntegrationTest() {
    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var userNeo4jRepository: UserNeo4jRepository

    @BeforeEach
    fun setUp() {
        userJpaRepository.deleteAll()
        userNeo4jRepository.deleteAll()
        setupAuthGatewayDefaults()
    }

    private fun createUser(
        email: String = "lucas@email.com",
        username: String = "lucaspaixao",
    ): String {
        val body =
            """
            {
              "name": "Lucas Paixão",
              "email": "$email",
              "username": "$username",
              "gender": "MALE",
              "birthdate": "1995-01-01"
            }
            """.trimIndent()

        val result =
            post("/api/v1/users", body)
                .andExpect(status().isCreated())
                .andReturn()

        @Suppress("UNCHECKED_CAST")
        return (objectMapper.readValue(result.response.contentAsString, Map::class.java) as Map<String, Any>)["id"] as String
    }

    @Nested
    inner class Success {
        @Test
        fun `returns current user profile when authenticated`() {
            val userId = createUser()
            mockFirebaseToken(userId)

            get("/api/v1/users", "valid-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Lucas Paixão"))
                .andExpect(jsonPath("$.email").value("lucas@email.com"))
                .andExpect(jsonPath("$.username").value("lucaspaixao"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.birthdate").value("1995-01-01"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.registeredAt").isNotEmpty())
        }
    }

    @Nested
    inner class AuthErrors {
        @Test
        fun `returns 401 when no token is provided`() {
            get("/api/v1/users")
                .andExpect(status().isUnauthorized())
        }
    }

    @Nested
    inner class NotFoundErrors {
        @Test
        fun `returns 404 when user does not exist in database`() {
            mockFirebaseToken("non-existent-uid")

            get("/api/v1/users", "valid-token")
                .andExpect(status().isNotFound())
        }
    }
}
