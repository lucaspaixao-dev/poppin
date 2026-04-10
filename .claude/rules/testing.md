---
paths:
  - "src/test/**/*.kt"
---

# Rule: Testing

Aplicada ao editar ou criar arquivos de teste.

---

## Testes de Integração

Localizados em `src/test/kotlin/.../integration/`. Herdam de `AbstractIntegrationTest`.

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = [
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
@Testcontainers
abstract class AbstractIntegrationTest {

    @MockitoBean protected lateinit var firebaseApp: FirebaseApp
    @MockitoBean protected lateinit var firebaseAuth: FirebaseAuth
    @MockitoBean protected lateinit var authGateway: AuthGateway

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    protected val mockMvc: MockMvc by lazy {
        MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    companion object {
        @Container @ServiceConnection @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")

        @Container @JvmStatic
        val neo4j: Neo4jContainer<*> = Neo4jContainer("neo4j:5")

        @DynamicPropertySource @JvmStatic
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.neo4j.uri") { neo4j.boltUrl }
            registry.add("spring.neo4j.authentication.username") { "neo4j" }
            registry.add("spring.neo4j.authentication.password") { neo4j.adminPassword }
        }
    }
}
```

---

## Regras

- Flyway desabilitado — Hibernate cria/destrói o schema com `create-drop`
- `@ServiceConnection` funciona para PostgreSQL; Neo4j requer `@DynamicPropertySource` manual
- Firebase sempre mockado com `@MockitoBean` — evita chamada ao `GoogleCredentials.getApplicationDefault()`
- `ObjectMapper` instanciado diretamente (`ObjectMapper().findAndRegisterModules()`) — não disponível como bean no contexto `MOCK`
- `@MockitoBean` é o substituto do Spring Framework 6.2+ para o `@MockBean` removido

---

## Lazy loading em testes

Coleções `@ElementCollection(FetchType.LAZY)` lançam `LazyInitializationException` fora de sessão JPA. Solução: `@Transactional` no método de teste.

```kotlin
@Test
@Transactional
fun `creates user with all optional fields`() {
    val result = post("/api/v1/users", body).andExpect(status().isCreated()).andReturn()
    val entity = userJpaRepository.findById(id).get()
    assertThat(entity.socialMedias).hasSize(2) // sem LazyInitializationException
}
```
