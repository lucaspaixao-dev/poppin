# Poppin

Rede social descontraída com foco em conexões reais entre pessoas.

---

## Stack

| Camada          | Tecnologia          | Responsabilidade                                                |
|-----------------|---------------------|-----------------------------------------------------------------|
| Backend         | Kotlin + Spring Boot | API REST                                                       |
| Auth            | Firebase Auth        | Autenticação de usuários                                       |
| Banco principal | PostgreSQL           | Source of truth — users, posts, comments, likes, notifications |
| Migrations      | Flyway               | Versionamento de schema                                        |
| Grafo           | Neo4j                | Friendships e sugestões de amizade                             |

---

## Arquitetura

Clean Architecture com DDD.

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
```

Fluxo de dependência: `infrastructure.input → application → domain`. A camada de domínio não conhece nenhuma outra.

### Injeção de dependência de serviços de domínio

Serviços de domínio não têm anotações Spring (`@Service`, `@Component`, etc.). São registrados como `@Bean` em classes `@Configuration` em `infrastructure/config/`.

```kotlin
// ✅ correto
@Configuration
class UserDomainConfig {
    @Bean
    fun createUserUseCase(userRepository: UserRepository): CreateUserUseCase {
        return CreateUserUseCase(userRepository)
    }
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

### Inputs e Outputs do domínio

- **Inputs** (`domain/{entidade}/input/`) — DTOs de transporte para as factories e use cases
  - `data class` sem lógica e sem validação
  - Tipos próprios (`LocationInput`, `SocialMediaInput`) — nunca usar value objects de domínio diretamente
- **Outputs** (`domain/{entidade}/output/`) — DTOs de saída dos use cases para a camada de apresentação
  - `data class` sem lógica
  - Tipos próprios (`LocationOutput`, `SocialMediaOutput`) — nunca expor value objects ou entidades de domínio para fora

```
Request (infrastructure) → Input (domain) → UseCase → Output (domain) → Response (infrastructure)
```

---

## Exceptions

Hierarquia com `sealed class`. Toda exception de domínio herda de `DomainException`.

```
DomainException (sealed) : Exception
  └── UserException (sealed)
        ├── InvalidEmail(email)
        ├── InvalidName(message)
        ├── InvalidSocialName(socialName)
        ├── InvalidProfilePhoto(url)
        ├── InvalidLocation(message)
        ├── InvalidSocialMedia(message)
        ├── NotFound(id)
        └── AlreadyExists(email)
  └── PostException (sealed)
        ├── NotFound(id)
        ├── InvalidContent(message)
        └── Unauthorized(userId)
  └── FriendshipException (sealed)
        ├── AlreadyExists(message)
        ├── NotFound(message)
        └── CannotAddYourself
  └── CommentException (sealed)
        ├── NotFound(id)
        └── InvalidContent(message)
```

Mapeamento para HTTP em um `@ControllerAdvice` global:

| Exception       | Status HTTP |
|-----------------|-------------|
| `NotFound`      | 404         |
| `Invalid*`      | 400         |
| `AlreadyExists` | 409         |
| `Unauthorized`  | 403         |
| Qualquer outra  | 500         |

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
- Nenhuma outra entidade do domínio é persistida aqui

### Firebase Auth

- Responsável apenas por autenticação — nunca por autorização de negócio
- O `uid` do Firebase é usado como `id` do `User` no domínio
- Gateway em `infrastructure/output/authentication/` — nunca chamar o SDK do Firebase fora dessa camada

---

## Logging

- Framework: **Log4j2** via SLF4J (`LoggerFactory.getLogger(javaClass)`)
- **Logging apenas nas bordas** — controllers (`infrastructure.input.rest`) e repositórios/gateways (`infrastructure.output.*`)
- **Nunca logar no domínio** (`domain/`) nem na aplicação (`application/`)
- Configuração em `src/main/resources/log4j2-spring.xml`

| Nível   | Quando usar                                                        |
|---------|--------------------------------------------------------------------|
| `TRACE` | Rastreamento linha a linha — apenas desenvolvimento local          |
| `DEBUG` | Valores e fluxo interno — desenvolvimento e staging                |
| `INFO`  | Eventos normais de negócio — usuário criado, post publicado        |
| `WARN`  | Retry, fallback, lentidão — sistema se recuperou               |
| `ERROR` | Operação falhou, usuário impactado — requer investigação           |
| `FATAL` | Sistema comprometido — requer ação imediata                        |

```kotlin
// ✅ correto — INFO para operação normal
log.info("User created: userId=$userId")

// ✅ correto — WARN para situação inesperada recuperável
log.warn("Retry attempt $attempt for postId=$postId")

// ✅ correto — ERROR para falha com exception
log.error("Failed to save user: userId=$userId", exception)

// ❌ errado — erro de validação não é ERROR do sistema
log.error("Invalid email: $email")
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
- Nunca lançar exceptions genéricas — sempre usar a hierarquia de `DomainException`
- Nunca chamar o SDK do Firebase fora de `infrastructure/output/authentication/`
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