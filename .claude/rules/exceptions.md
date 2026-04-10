---
paths:
    - "src/main/kotlin/**/exception/**/*.kt"
    - "src/main/kotlin/**/handler/**/*.kt"
    - "src/main/kotlin/**/*Exception*.kt"
    - "src/main/kotlin/**/*Handler*.kt"
---

# Rule: Exceptions

Aplicada ao criar ou editar exceptions, handlers e use cases.

---

## Hierarquia

A raiz é `BaseException : RuntimeException` — **não** `Exception` (Mockito rejeita `thenThrow` em checked exceptions).

```
BaseException (sealed) : RuntimeException
  ├── RepositoryException (sealed)          → falhas de persistência — 500
  │     └── CreationFailed(message, cause)
  ├── AuthGatewayException (sealed)         → falhas de comunicação com Firebase — 500
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

---

## Mapeamento HTTP

Handler global: `GlobalExceptionHandler` (`@RestControllerAdvice`).

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

`MethodArgumentNotValidException` (Bean Validation em `@RequestBody`) e `HttpMessageNotReadableException` (JSON inválido ou campo ausente) **devem sempre ter handler explícito** — caso contrário caem no fallback 500.
