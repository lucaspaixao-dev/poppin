package io.github.lucaspaixaodev.poppin.integration.user

import io.github.lucaspaixaodev.poppin.infrastructure.output.database.neo4j.user.UserNeo4jRepository
import io.github.lucaspaixaodev.poppin.infrastructure.output.database.postgres.user.UserJpaRepository
import io.github.lucaspaixaodev.poppin.integration.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

class CreateUserIT : AbstractIntegrationTest() {
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

    @Nested
    inner class Success {
        @Test
        @Transactional
        fun `creates user with minimal required fields`() {
            val body =
                """
                {
                  "name": "Lucas Paixão",
                  "email": "lucas@email.com",
                  "username": "lucaspaixao",
                  "gender": "MALE",
                  "birthdate": "1995-01-01"
                }
                """.trimIndent()

            val result =
                post("/api/v1/users", body)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("Lucas Paixão"))
                    .andExpect(jsonPath("$.email").value("lucas@email.com"))
                    .andExpect(jsonPath("$.username").value("lucaspaixao"))
                    .andExpect(jsonPath("$.gender").value("MALE"))
                    .andExpect(jsonPath("$.birthdate").value("1995-01-01"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.registeredAt").isNotEmpty())
                    .andExpect(jsonPath("$.socialName").doesNotExist())
                    .andExpect(jsonPath("$.profilePhoto").doesNotExist())
                    .andExpect(jsonPath("$.location").doesNotExist())
                    .andExpect(jsonPath("$.bio").doesNotExist())
                    .andExpect(jsonPath("$.socialMedias").isEmpty())
                    .andReturn()

            @Suppress("UNCHECKED_CAST")
            val id = (objectMapper.readValue(result.response.contentAsString, Map::class.java) as Map<String, Any>)["id"] as String

            // PostgreSQL
            val entity = userJpaRepository.findById(id).get()
            assertThat(entity.name).isEqualTo("Lucas Paixão")
            assertThat(entity.email).isEqualTo("lucas@email.com")
            assertThat(entity.username).isEqualTo("lucaspaixao")
            assertThat(entity.active).isTrue()
            assertThat(entity.location).isNull()
            assertThat(entity.socialMedias).isEmpty()

            // Neo4j
            assertThat(userNeo4jRepository.existsById(id)).isTrue()
        }

        @Test
        @Transactional
        fun `creates user with all optional fields`() {
            val body =
                """
                {
                  "name": "Ana Souza",
                  "email": "ana@email.com",
                  "username": "anasouza",
                  "gender": "FEMALE",
                  "birthdate": "1998-06-15",
                  "bio": "Just a person",
                  "socialName": "ana_dev",
                  "profilePhoto": "https://cdn.example.com/ana.jpg",
                  "location": { "city": "São Paulo", "country": "Brazil" },
                  "socialMedias": [
                    { "platform": "INSTAGRAM", "url": "https://instagram.com/ana" },
                    { "platform": "LINKEDIN",  "url": "https://linkedin.com/in/ana" }
                  ]
                }
                """.trimIndent()

            val result =
                post("/api/v1/users", body)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.bio").value("Just a person"))
                    .andExpect(jsonPath("$.socialName").value("ana_dev"))
                    .andExpect(jsonPath("$.profilePhoto").value("https://cdn.example.com/ana.jpg"))
                    .andExpect(jsonPath("$.location.city").value("São Paulo"))
                    .andExpect(jsonPath("$.location.country").value("Brazil"))
                    .andExpect(jsonPath("$.socialMedias.length()").value(2))
                    .andReturn()

            @Suppress("UNCHECKED_CAST")
            val id = (objectMapper.readValue(result.response.contentAsString, Map::class.java) as Map<String, Any>)["id"] as String

            // PostgreSQL — verify all optional fields persisted
            val entity = userJpaRepository.findById(id).get()
            assertThat(entity.bio).isEqualTo("Just a person")
            assertThat(entity.socialName).isEqualTo("ana_dev")
            assertThat(entity.profilePhoto).isEqualTo("https://cdn.example.com/ana.jpg")
            assertThat(entity.location).isNotNull()
            assertThat(entity.location!!.city).isEqualTo("São Paulo")
            assertThat(entity.location.country).isEqualTo("Brazil")
            assertThat(entity.socialMedias).hasSize(2)

            // Neo4j
            assertThat(userNeo4jRepository.existsById(id)).isTrue()
        }

        @Test
        fun `two different users can be created without interference`() {
            val first =
                """
                {
                  "name": "First User",
                  "email": "first@email.com",
                  "username": "firstuser",
                  "gender": "MALE",
                  "birthdate": "1990-01-01"
                }
                """.trimIndent()
            val second =
                """
                {
                  "name": "Second User",
                  "email": "second@email.com",
                  "username": "seconduser",
                  "gender": "FEMALE",
                  "birthdate": "1992-05-20"
                }
                """.trimIndent()

            post("/api/v1/users", first).andExpect(status().isCreated())
            post("/api/v1/users", second).andExpect(status().isCreated())

            assertThat(userJpaRepository.count()).isEqualTo(2)
            assertThat(userNeo4jRepository.count()).isEqualTo(2)
        }
    }

    @Nested
    inner class ConflictErrors {
        @Test
        fun `returns 409 when email is already registered`() {
            val body =
                """
                {
                  "name": "Lucas Paixão",
                  "email": "lucas@email.com",
                  "username": "lucaspaixao",
                  "gender": "MALE",
                  "birthdate": "1995-01-01"
                }
                """.trimIndent()
            post("/api/v1/users", body).andExpect(status().isCreated())

            val duplicate = body.replace("lucaspaixao", "outrouser")
            post("/api/v1/users", duplicate).andExpect(status().isConflict())

            assertThat(userJpaRepository.count()).isEqualTo(1)
        }

        @Test
        fun `returns 409 when username is already taken`() {
            val body =
                """
                {
                  "name": "Lucas Paixão",
                  "email": "lucas@email.com",
                  "username": "lucaspaixao",
                  "gender": "MALE",
                  "birthdate": "1995-01-01"
                }
                """.trimIndent()
            post("/api/v1/users", body).andExpect(status().isCreated())

            val duplicate = body.replace("lucas@email.com", "other@email.com")
            post("/api/v1/users", duplicate).andExpect(status().isConflict())

            assertThat(userJpaRepository.count()).isEqualTo(1)
        }
    }

    @Nested
    inner class ValidationErrors {
        @Test
        fun `returns 400 when name is missing`() {
            val body =
                """
                {
                  "email": "lucas@email.com",
                  "username": "lucaspaixao",
                  "gender": "MALE",
                  "birthdate": "1995-01-01"
                }
                """.trimIndent()

            post("/api/v1/users", body).andExpect(status().isBadRequest())

            assertThat(userJpaRepository.count()).isEqualTo(0)
        }

        @Test
        fun `returns 400 when email format is invalid`() {
            val body =
                """
                {
                  "name": "Lucas Paixão",
                  "email": "not-an-email",
                  "username": "lucaspaixao",
                  "gender": "MALE",
                  "birthdate": "1995-01-01"
                }
                """.trimIndent()

            post("/api/v1/users", body).andExpect(status().isBadRequest())

            assertThat(userJpaRepository.count()).isEqualTo(0)
        }

        @Test
        fun `returns 400 when username contains uppercase letters`() {
            val body =
                """
                {
                  "name": "Lucas Paixão",
                  "email": "lucas@email.com",
                  "username": "LucasPaixao",
                  "gender": "MALE",
                  "birthdate": "1995-01-01"
                }
                """.trimIndent()

            post("/api/v1/users", body).andExpect(status().isBadRequest())

            assertThat(userJpaRepository.count()).isEqualTo(0)
        }

        @Test
        fun `returns 400 when birthdate is in the future`() {
            val body =
                """
                {
                  "name": "Lucas Paixão",
                  "email": "lucas@email.com",
                  "username": "lucaspaixao",
                  "gender": "MALE",
                  "birthdate": "2099-01-01"
                }
                """.trimIndent()

            post("/api/v1/users", body).andExpect(status().isBadRequest())

            assertThat(userJpaRepository.count()).isEqualTo(0)
        }

        @Test
        fun `returns 400 when social medias exceed 5`() {
            val medias =
                (1..6).joinToString(",") {
                    """{ "platform": "OTHER", "url": "https://example.com/$it" }"""
                }
            val body =
                """
                {
                  "name": "Lucas Paixão",
                  "email": "lucas@email.com",
                  "username": "lucaspaixao",
                  "gender": "MALE",
                  "birthdate": "1995-01-01",
                  "socialMedias": [$medias]
                }
                """.trimIndent()

            post("/api/v1/users", body).andExpect(status().isBadRequest())

            assertThat(userJpaRepository.count()).isEqualTo(0)
        }
    }
}
