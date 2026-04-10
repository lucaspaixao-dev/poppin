---
paths:
    - "src/main/kotlin/**/infrastructure/**/*.kt"
    - "src/main/resources/**/*.sql"
    - "src/main/resources/**/*.xml"
    - "src/main/resources/**/*.yml"
    - "src/main/resources/**/*.yaml"
---

# Rule: Infrastructure

Aplicada ao editar qualquer arquivo em `infrastructure/`.

---

## JPA (PostgreSQL)

- Classes `@Entity` nunca são `data class` — `equals`/`hashCode` gerado pelo Kotlin quebraria o Hibernate
- Nome da tabela sempre explícito no `@Table(name = "...")` — evitar palavras reservadas do PostgreSQL (ex: `"users"` em vez de `"user"`)
- No-arg constructor gerado pelo plugin `kotlin("plugin.jpa")` — nunca escrever manualmente
- Coleções sempre `FetchType.LAZY` — nunca eager
- Toda entity expõe:
    - `toDomain()` → converte para entidade de domínio via `reconstitute`
    - `fromDomain(entity)` no `companion object` → converte do domínio para JPA entity
- Value objects mapeados como `@Embeddable` — nunca adicionar anotações JPA ao domínio
- Campos de `@Embeddable` com valores default (`= ""`) para satisfazer o no-arg constructor
- Nomes de colunas explícitos em embeddables para evitar colisão (ex: `location_city`, `location_country`)
- Listas de value objects: `@ElementCollection(fetch = FetchType.LAZY)`
- Enums: `@Enumerated(EnumType.STRING)`

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

---

## Flyway (Migrations)

- Padrão de nomenclatura: `V{YYYYMMDDHHMMSS}__{descrição}.sql`
    - Exemplo: `V20260407152100__create_initial_schema.sql`
- Uma migration por conjunto coeso de mudanças
- Nunca editar uma migration já aplicada — criar uma nova

---

## Neo4j

- Node de `User` armazena **somente o `id`** — nunca desnormalizar dados do PostgreSQL
- Apenas nodes de `User` e edges de `Friendship` vivem no Neo4j
- Node: `@Node("Label") class XNode(@Id val id: String)`
- Repositório Spring Data: `interface XNeo4jRepository : Neo4jRepository<XNode, String>`
- Implementação: classe `@Repository` que injeta o repositório Spring Data e implementa a interface de domínio

### Configuração de Transaction Manager (JPA + Neo4j)

O auto-configure do Spring Boot **não** cria `Neo4jTransactionManager` quando `JpaTransactionManager` já existe — sem ele, escritas Neo4j lançam `NullPointerException`. Declarar ambos explicitamente em `Neo4jConfig`:

```kotlin
@Configuration
@EnableNeo4jRepositories(
    basePackages = ["io.github.lucaspaixaodev.poppin.infrastructure.output.graph"],
    transactionManagerRef = "neo4jTransactionManager"
)
class Neo4jConfig {

    @Bean @Primary
    fun transactionManager(entityManagerFactory: EntityManagerFactory): JpaTransactionManager =
        JpaTransactionManager(entityManagerFactory)

    @Bean
    fun neo4jTransactionManager(driver: Driver): Neo4jTransactionManager =
        Neo4jTransactionManager(driver)
}
```

---

## Firebase Auth

- Responsável apenas por autenticação — nunca por autorização de negócio
- O `uid` do Firebase é usado como `id` do `User` no domínio
- Nunca chamar o SDK do Firebase fora de `infrastructure/output/authentication/`

---

## Jackson + Kotlin (Spring Boot 4)

Spring Boot 4 usa Jackson 3 (`tools.jackson`). O módulo Kotlin **não** é incluído automaticamente:

```kotlin
implementation("tools.jackson.module:jackson-module-kotlin")
```

Sem esse módulo, Jackson 3 passa `null` para parâmetros não-nulos ausentes no JSON — toda request sem campo obrigatório retorna 500 ao invés de 400.

---

## Request DTOs

- Campos opcionais com valor default (`= emptyList()`, `= null`) — funcionam corretamente com o módulo Kotlin
- Campos obrigatórios não-nulos sem default (`val name: String`) → `HttpMessageNotReadableException` quando ausentes → 400
- Validações Jakarta nas anotações do DTO (`@NotBlank`, `@Email`, `@Size`, `@Past`) → `MethodArgumentNotValidException` → 400
- Sempre devolver os exemplos de requests, responses e exceptions em JSON no OpenAPI, e tambem mapear as exceptions que pode devolver e colocar no openapi.

---

## Logging

- Framework: Log4j2 via SLF4J — configuração em `src/main/resources/log4j2-spring.xml`
- Logar apenas nas bordas — controllers e repositórios/gateways de infraestrutura
- Nunca logar em `domain/` nem em `application/`
- `INFO` para operações normais, `WARN` para retries/fallbacks, `ERROR` para falhas com exception
- Erros de validação de domínio não são `ERROR` — são fluxo esperado, usar `DEBUG` se necessário
