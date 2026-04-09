package io.github.lucaspaixaodev.poppin.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import io.github.lucaspaixaodev.poppin.domain.user.gateway.AuthGateway
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.neo4j.Neo4jContainer
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@Testcontainers
abstract class AbstractIntegrationTest {
    @MockitoBean
    protected lateinit var firebaseApp: FirebaseApp

    @MockitoBean
    protected lateinit var firebaseAuth: FirebaseAuth

    @MockitoBean
    protected lateinit var authGateway: AuthGateway

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    protected val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

    protected val mockMvc: MockMvc by lazy {
        MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    protected fun setupAuthGatewayDefaults() {
        whenever(authGateway.existsByEmail(any())).thenReturn(false)
        whenever(authGateway.createUser(any())).thenReturn("mocked-firebase-uid")
    }

    protected fun post(
        url: String,
        body: String,
    ): ResultActions =
        mockMvc.perform(
            MockMvcRequestBuilders
                .post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @JvmStatic
        val neo4j: Neo4jContainer = Neo4jContainer("neo4j:5")

        @DynamicPropertySource
        @JvmStatic
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.neo4j.uri") { neo4j.boltUrl }
            registry.add("spring.neo4j.authentication.username") { "neo4j" }
            registry.add("spring.neo4j.authentication.password") { neo4j.adminPassword }
        }
    }
}
