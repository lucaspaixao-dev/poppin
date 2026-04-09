# Poppin

Rede social descontraída com foco em conexões reais entre pessoas.

---

## Stack

| Camada          | Tecnologia                   | Responsabilidade                                                |
|-----------------|------------------------------|-----------------------------------------------------------------|
| Backend         | Kotlin 2.2.0 + Spring Boot 4 | API REST                                                        |
| Auth            | Firebase Auth        | Autenticação de usuários                                        |
| Banco principal | PostgreSQL           | Source of truth — users, posts, comments, likes, notifications  |
| Migrations      | Flyway               | Versionamento de schema                                         |
| Grafo           | Neo4j                | Friendships e sugestões de amizade                              |

---

## Arquitetura

Clean Architecture com DDD. Fluxo de dependência: `infrastructure.input → application → domain`. A camada de domínio não conhece nenhuma outra.

```
src/
  domain/
    {entidade}/
      {Entidade}.kt               → entidade com factory
      {Entidade}Repository.kt     → interface de repositório (port de saída)
      input/                      → DTOs de entrada do domínio
      output/                     → DTOs de saída do domínio
      exception/                  → exceptions específicas da entidade

  application/
    {entidade}/
      {Ação}{Entidade}UseCase.kt  → orquestra domínio e repositórios

  infrastructure/
    config/                       → @Configuration beans — registra serviços de domínio sem anotações Spring
    input/
      rest/
        {entidade}/
          {Ação}{Entidade}Api.kt         → interface com anotações Spring MVC e OpenAPI
          {Ação}{Entidade}Controller.kt  → implementação, injeta o use case
          request/                       → DTOs de entrada com validações Jakarta
          response/                      → DTOs de saída para o cliente
    output/
      database/                   → entidades JPA, repositórios Spring Data
      authentication/             → cliente Firebase
      graph/                      → nodes e repositórios Neo4j
```

### Fluxo de dados por camada

```
Request (infrastructure) → Input (domain) → UseCase → Output (domain) → Response (infrastructure)
```

- **Inputs** (`domain/{entidade}/input/`) — DTOs de transporte para factories e use cases. `data class` sem lógica. Tipos próprios (`LocationInput`, `SocialMediaInput`) — nunca usar value objects de domínio diretamente.
- **Outputs** (`domain/{entidade}/output/`) — DTOs de saída dos use cases. `data class` sem lógica. Tipos próprios (`LocationOutput`, `SocialMediaOutput`) — nunca expor value objects ou entidades de domínio para fora.

### Injeção de dependência de serviços de domínio

Serviços de domínio não têm anotações Spring (`@Service`, `@Component`, etc.). São registrados como `@Bean` em classes `@Configuration` em `infrastructure/config/`.

```kotlin
@Configuration
class UserDomainConfig {
    @Bean
    fun createUserUseCase(userRepository: UserRepository): CreateUserUseCase =
        CreateUserUseCase(userRepository)
}
```

---

## Domínio — Regras obrigatórias

### Entidades

- Construtores sempre `private` — nenhuma classe externa instancia diretamente
- Toda entidade expõe dois métodos estáticos na `companion object Factory`:
  - `create(input)` → valida e cria. Retorna `Result<T>`
  - `reconstitute(...)` → reconstrói do banco sem validação. Não retorna `Result`
- Estado mutável nunca exposto como `var` público — alterações só via métodos da própria classe
- Comparação por identidade (`id`), não por valor dos campos — não usar `data class` para entidades
- `equals` e `hashCode` baseados apenas no `id`

```kotlin
// ✅ correto
class User private constructor(...) {
    private var _active: Boolean = true
    val active: Boolean get() = _active

    fun activate() { _active = true }
    fun deactivate() { _active = false }

    companion object Factory {
        fun create(input: CreateUserInput): Result<User> { ... }
        fun reconstitute(...): User { ... }
    }
}

// ❌ construtor público
class User(val id: String, var active: Boolean)

// ❌ var público
class User private constructor(var active: Boolean)
```

### Value Objects

- Usar `data class` — dois value objects com os mesmos dados são iguais
- Validação no bloco `init` — lança `DomainException` diretamente
- Imutáveis — apenas `val`, nunca `var`

```kotlin
data class Location(val city: String, val country: String) {
    init {
        if (city.isBlank()) throw UserException.InvalidLocation("City cannot be blank")
        if (country.isBlank()) throw UserException.InvalidLocation("Country cannot be blank")
    }
}
```

---

## Exceptions

Hierarquia com `sealed class`. A raiz é `BaseException : RuntimeException` — **não** `Exception` (Mockito rejeita `thenThrow` em checked exceptions).

```
BaseException (sealed) : RuntimeException
  ├── RepositoryException (sealed)          → falhas de persistência — mapeado para 500
  │     └── CreationFailed(message, cause)
  ├── AuthGatewayException (sealed)         → falhas de comunicação com Firebase — mapeado para 500
  │     ├── UserCreationFailed(email, cause)
  │     └── UserUpdateFailed(uid, cause)
  └── UserException (sealed)
        ├── InvalidEmail(email)             → 400
        ├── InvalidName(message)            → 400
        ├── InvalidSocialName(socialName)   → 400
        ├── InvalidProfilePhoto(url)        → 400
        ├── InvalidLocation(message)        → 400
        ├── InvalidSocialMedia(message)     → 400
        ├── InvalidUsername(username)       → 400
        ├── InvalidBirthdate(message)       → 400
        ├── NotFound(id)                    → 404
        ├── AlreadyExists(email)            → 409
        └── UsernameAlreadyExists(username) → 409
```

Mapeamento para HTTP em `GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception                          | Status HTTP |
|------------------------------------|-------------|
| `NotFound`                         | 404         |
| `Invalid*`                         | 400         |
| `AlreadyExists` / `*AlreadyExists` | 409         |
| `Unauthorized`                     | 403         |
| `RepositoryException`              | 500         |
| `AuthGatewayException`             | 500         |
| `MethodArgumentNotValidException`  | 400         |
| `HttpMessageNotReadableException`  | 400         |
| Qualquer outra                     | 500         |

`MethodArgumentNotValidException` (Bean Validation em `@RequestBody`) e `HttpMessageNotReadableException` (corpo JSON inválido ou campo obrigatório ausente) **devem sempre ter handler explícito** — caso contrário caem no fallback 500.

---

## Infraestrutura

### JPA (PostgreSQL)

- Classes `@Entity` nunca são `data class` — `equals`/`hashCode` gerado pelo Kotlin quebraria o Hibernate
- Nome da tabela sempre explícito no `@Table(name = "...")` — evitar palavras reservadas do PostgreSQL (ex: `"users"` em vez de `"user"`)
- No-arg constructor gerado pelo plugin `kotlin("plugin.jpa")` — nunca escrever manualmente
- Coleções sempre `FetchType.LAZY` — nunca eager
- Toda entity expõe:
  - `toDomain()` → converte para entidade de domínio via `reconstitute`
  - `fromDomain(entity)` no `companion object` → converte do domínio para JPA entity
- Value objects do domínio mapeados como `@Embeddable` — nunca adicionar anotações JPA ao domínio
- Campos de `@Embeddable` com valores default (`= ""`) para satisfazer o no-arg constructor
- Nomes de colunas explícitos em embeddables para evitar colisão (ex: `location_city`, `location_country`)
- Listas de value objects mapeadas com `@ElementCollection(fetch = FetchType.LAZY)`
- Enums persistidos como `@Enumerated(EnumType.STRING)`

```kotlin
// entity
@Entity
@Table(name = "users")
class UserEntity(
    @Id val id: String,
    @Column(nullable = false) val name: String,
    @Embedded val location: LocationEmbeddable? = null,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_social_medias", joinColumns = [JoinColumn(name = "user_id")])
    val socialMedias: List<SocialMediaEmbeddable> = emptyList()
) {
    fun toDomain(): User = User.reconstitute(...)
    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(...)
    }
}

// embeddable
@Embeddable
class LocationEmbeddable(
    @Column(name = "location_city") val city: String = "",
    @Column(name = "location_country") val country: String = ""
) {
    fun toDomain(): Location = Location(city, country)
    companion object {
        fun fromDomain(l: Location) = LocationEmbeddable(l.city, l.country)
    }
}
```

### Flyway (Migrations)

- Padrão de nomenclatura: `V{YYYYMMDDHHMMSS}__{descrição}.sql`
  - Exemplo: `V20260407152100__create_initial_schema.sql`
- Uma migration por conjunto coeso de mudanças — não criar arquivos separados para alterações relacionadas
- Nunca editar uma migration já aplicada em qualquer ambiente — criar uma nova

### Neo4j

- Apenas nodes de `User` e edges de `Friendship` vivem no Neo4j
- Node de `User` armazena **somente o `id`** — nunca desnormalizar nome, foto ou outros dados do PostgreSQL
- Nenhuma outra entidade do domínio é persistida aqui
- Node: `@Node("Label") class XNode(@Id val id: String)` — sem campos extras além do id de referência
- Repositório Spring Data: `interface XNeo4jRepository : Neo4jRepository<XNode, String>`
- Implementação de domínio: classe `@Repository` que injeta o repositório Spring Data e implementa a interface de domínio (`UserGraphRepository`)

#### Configuração de Transaction Manager (JPA + Neo4j)

Quando JPA e Neo4j coexistem, o auto-configure do Spring Boot **não** cria `Neo4jTransactionManager` porque `JpaTransactionManager` já satisfaz `@ConditionalOnMissingBean(PlatformTransactionManager)`. Sem o `Neo4jTransactionManager`, qualquer escrita pelo `Neo4jTemplate` lança `NullPointerException`.

**Solução obrigatória**: declarar ambos explicitamente em `Neo4jConfig`:

```kotlin
@Configuration
@EnableNeo4jRepositories(
    basePackages = ["io.github.lucaspaixaodev.poppin.infrastructure.output.graph"],
    transactionManagerRef = "neo4jTransactionManager"
)
class Neo4jConfig {

    @Bean
    @Primary
    fun transactionManager(entityManagerFactory: EntityManagerFactory): JpaTransactionManager =
        JpaTransactionManager(entityManagerFactory)

    @Bean
    fun neo4jTransactionManager(driver: Driver): Neo4jTransactionManager =
        Neo4jTransactionManager(driver)
}
```

`@Primary` no `JpaTransactionManager` garante que JPA repositories continuam usando o manager correto por padrão. `@EnableNeo4jRepositories(transactionManagerRef = "neo4jTransactionManager")` direciona os repositórios Neo4j para o manager específico.

### Firebase Auth

- Responsável apenas por autenticação — nunca por autorização de negócio
- O `uid` do Firebase é usado como `id` do `User` no domínio
- Gateway em `infrastructure/output/authentication/` — nunca chamar o SDK do Firebase fora dessa camada

### Jackson + Kotlin (Spring Boot 4)

Spring Boot 4 usa Jackson 3 (`tools.jackson`). O módulo Kotlin **não** é incluído automaticamente:

```kotlin
implementation("tools.jackson.module:jackson-module-kotlin")
```

Sem esse módulo, Jackson 3 passa `null` para parâmetros Kotlin não-nulos ausentes no JSON, causando `NullPointerException` no construtor mesmo que o campo tenha valor default — toda request sem campo obrigatório retorna 500 ao invés de 400.

### Request DTOs (infrastructure/input/rest)

- Campos opcionais com valor default (`= emptyList()`, `= null`) funcionam corretamente com o módulo Kotlin registrado
- Campos obrigatórios não-nulos sem default (`val name: String`) resultam em `HttpMessageNotReadableException` quando ausentes — tratado como 400 pelo `GlobalExceptionHandler`
- Validações Jakarta ficam nas anotações do DTO (`@NotBlank`, `@Email`, `@Size`, `@Past`) — gera `MethodArgumentNotValidException` → 400
---

## Qualidade de código

### Ferramentas

| Ferramenta | Versão   | Propósito                              | Executar com                      |
|------------|----------|----------------------------------------|-----------------------------------|
| detekt     | 1.23.8   | Análise estática — bugs e code smells  | `./gradlew detekt`                |
| ktlint     | 14.2.0   | Formatação e estilo Kotlin             | `./gradlew ktlintCheck`           |
| Kover      | 0.9.1    | Cobertura de testes (mín. 90%)         | `./gradlew koverVerify`           |

Para gerar relatório HTML de cobertura: `./gradlew koverHtmlReport` → `build/reports/kover/html/`.

### detekt

Configuração em `config/detekt.yml`. Ajustes relevantes feitos para o projeto:

### ktlint

Integrado via plugin `org.jlleitschuh.gradle.ktlint`. Sem arquivo de configuração customizado — usa os defaults do ktlint (estilo Kotlin oficial). Comandos:

```
./gradlew ktlintCheck   # verifica
./gradlew ktlintFormat  # corrige automaticamente
```

O hook de formatação pode ser instalado via `./gradlew addKtlintFormatGitPreCommitHook`.

### Cobertura (Kover)

Threshold mínimo: **90% de linhas** — verificado por `./gradlew koverVerify`.

**Excluídos da cobertura** (infraestrutura sem lógica testável isoladamente):
- `PoppinApplicationKt` — entry point, sem lógica
- `*.infrastructure.config.*` — beans de configuração Spring
- `*.infrastructure.input.rest.*.request.*` e `*.response.*` — DTOs puros
- `*Entity`, `*Embeddable` — mapeamento JPA
- `*.infrastructure.output.graph.*`, `*.infrastructure.output.database.neo4j.*` — Neo4j (auto-implementado pelo Spring Data)
- `*.infrastructure.output.authentication.*` — gateway Firebase (requer credenciais reais; sempre mockado em testes)

**O que deve ser coberto**:
- Todo o `domain/` — entidades, value objects, serviços, factories
- Todo o `application/` — use cases
- Controllers e repositórios de banco (PostgreSQL) — cobertos pelos testes de integração

---

## Testes

### Testes de Integração

Localizados em `src/test/kotlin/.../integration/`. Herdam de `AbstractIntegrationTest`.

**Configuração base** (`AbstractIntegrationTest`):

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

**Regras**:

- Flyway desabilitado em testes — Hibernate cria/destrói o schema com `create-drop`
- `@ServiceConnection` funciona para PostgreSQL; Neo4j requer `@DynamicPropertySource` manual
- Firebase mockado com `@MockitoBean` — evita chamada ao `GoogleCredentials.getApplicationDefault()`
- `ObjectMapper` instanciado diretamente (`ObjectMapper().findAndRegisterModules()`) — não disponível como bean no contexto `MOCK`
- `@MockitoBean` é o substituto do Spring Framework 6.2+ para o `@MockBean` removido

**Lazy loading em testes**:

Coleções `@ElementCollection(FetchType.LAZY)` lançam `LazyInitializationException` quando acessadas fora de sessão JPA. Solução: anotar o método de teste com `@Transactional`:

```kotlin
@Test
@Transactional
fun `creates user with all optional fields`() {
    val result = post("/api/v1/users", body).andExpect(status().isCreated()).andReturn()
    val entity = userJpaRepository.findById(id).get()
    assertThat(entity.socialMedias).hasSize(2) // sem LazyInitializationException
}
```

---

## O que nunca fazer

- Nunca instanciar entidades diretamente — sempre via factory
- Nunca expor `var` público em entidades de domínio
- Nunca validar no `init` de entidades — validação fica na factory
- Nunca usar `data class` para entidades de domínio ou JPA entities
- Nunca carregar coleções com eager loading
- Nunca adicionar anotações JPA ou Spring em classes do domínio
- Nunca acessar o domínio direto da camada de apresentação — sempre via use case
- Nunca lançar exceptions genéricas — sempre usar a hierarquia de `BaseException`
- Nunca chamar o SDK do Firebase fora de `infrastructure/output/authentication/`
- Nunca salvar dados além do `id` no node Neo4j
- Nunca editar uma migration já aplicada

---

## Modelos do domínio

```
User
  ├── Location (value object, opcional)
  └── SocialMedia (value object, lista opcional, max 5)

Post
  ├── author → User
  └── type → PostType (TEXT, IMAGE, VIDEO)

Comment
  ├── author → User
  └── post → Post

Like
  ├── user → User
  └── post → Post

CommentLike
  ├── user → User
  └── comment → Comment

Friendship
  ├── fromUser → User
  ├── toUser → User
  └── status → FriendshipStatus (PENDING, ACCEPTED, BLOCKED)

Notification
  ├── recipient → User
  ├── type → NotificationType
  └── referenceId → String (id da entidade relacionada)
```
