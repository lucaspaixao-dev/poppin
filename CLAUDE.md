# Poppin

Rede social descontraída com foco em conexões reais entre pessoas.

---

## Stack

| Camada          | Tecnologia                   | Responsabilidade                                                |
|-----------------|------------------------------|-----------------------------------------------------------------|
| Backend         | Kotlin 2.2.0 + Spring Boot 4 | API REST                                                        |
| Auth            | Firebase Auth                | Autenticação de usuários                                        |
| Banco principal | PostgreSQL                   | Source of truth — users, posts, comments, likes, notifications  |
| Migrations      | Flyway                       | Versionamento de schema                                         |
| Grafo           | Neo4j                        | Friendships e sugestões de amizade                              |

---

## Arquitetura

Clean Architecture com DDD. Fluxo de dependência: `infrastructure.input → application → domain`. A camada de domínio não conhece nenhuma outra.

```
src/
  domain/
    {entidade}/
      {Entidade}.kt               → entidade com factory
      input/                      → DTOs de entrada do domínio
      output/                     → DTOs de saída do domínio
      exception/                  → exceptions específicas da entidade
      service/                    → lógica de negócio pura, sem dependências externas
      repository/                 → implementação do repositório (adapter de saída)
    exception/                    → exceptions genéricas de domínio (base para todas as entidades)

  application/
    {entidade}/
      {Ação}{Entidade}UseCase.kt  → orquestra domínio e repositórios

  infrastructure/
    config/                       → configurações Spring, beans de domínio, Neo4j, etc.
    input/
      async/
        sqs/                         → listeners e configuração do SQS
          {entidade}/
            {Ação}{Entidade}Listener.kt  → listener SQS que injeta o use case
      rest/
        {entidade}/
          {Ação}{Entidade}Api.kt         → interface com anotações Spring MVC e OpenAPI
          {Ação}{Entidade}Controller.kt  → implementação, injeta o use case
          request/                       → DTOs de entrada com validações Jakarta
          response/                      → DTOs de saída para o cliente
    output/
      database/                          → implementação de repositórios de banco de dados
         postgres/                       → entidades JPA, repositórios Spring Data
            {entidade}/
              {Entidade}Entity.kt               → entidade JPA
              Postgres-{Entidade}Repository.kt  → interface Spring Data JPA
         graph/                   → entidades Neo4j, repositórios Spring Data
            {entidade}/
              {Entidade}Node.kt                 → entidade Neo4j
              Neo4j-{Entidade}Repository.kt     → interface Spring Data Neo4j
              {Entidade}Neo4jRepository         → implementação do repositório de domínio
      authentication/             → cliente Firebase e configuração
      async/
        sqs/                         → configuração do SQS e produtor de mensagens
          {entidade}/
            {Ação}{Entidade}Producer.kt  → produtor SQS que publica mensagens após ações de domínio
```

### Fluxo de dados por camada

```
REST - Request (infrastructure) → Input (domain) → UseCase → Output (domain) → Response (infrastructure)
```

### Injeção de dependência de serviços de domínio

Serviços de domínio não têm anotações Spring (`@Service`, `@Component`, etc.). São registrados como `@Bean` em `infrastructure/config/`.

```kotlin
@Configuration
class UserDomainConfig {
    @Bean
    fun createUserUseCase(userRepository: UserRepository): CreateUserUseCase =
        CreateUserUseCase(userRepository)
}
```

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

Like / CommentLike
  ├── user → User
  └── post → Post / comment → Comment

Friendship
  ├── fromUser → User
  ├── toUser → User
  └── status → FriendshipStatus (PENDING, ACCEPTED, BLOCKED)

Notification
  ├── recipient → User
  ├── type → NotificationType
  └── referenceId → String
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

> Rules detalhadas em `.claude/rules/` — domain, exceptions, infrastructure, testing, quality.
