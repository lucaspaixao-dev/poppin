---
paths:
    - "src/main/kotlin/**/domain/**/*.kt"
    - "src/main/kotlin/**/application/**/*.kt"
---

# Rule: Domain

Aplicada ao editar qualquer arquivo em `domain/`.

---

## Entidades

- Construtores sempre `private` — nenhuma classe externa instancia diretamente
- Toda entidade expõe dois métodos na `companion object Factory`:
    - `create(input)` → valida e cria. Retorna `Result<T>`
    - `reconstitute(...)` → reconstrói do banco sem validação. Não retorna `Result`
- Estado mutável nunca exposto como `var` público — alterações só via métodos internos
- Comparação por identidade (`id`) — não usar `data class` para entidades
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

---

## Value Objects

- Usar `data class` — dois value objects com os mesmos dados são iguais
- Validação no bloco `init` — lança a exception de domínio diretamente
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

## Inputs e Outputs

- **Inputs** (`domain/{entidade}/input/`) — DTOs de transporte para factories e use cases
    - `data class` sem lógica e sem validação
    - Tipos próprios (`LocationInput`, `SocialMediaInput`) — nunca usar value objects de domínio diretamente
- **Outputs** (`domain/{entidade}/output/`) — DTOs de saída dos use cases
    - `data class` sem lógica
    - Tipos próprios (`LocationOutput`, `SocialMediaOutput`) — nunca expor value objects ou entidades de domínio para fora
