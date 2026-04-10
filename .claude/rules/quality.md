---
paths:
  - "*.gradle.kts"
  - "build.gradle.kts"
  - "config/detekt.yml"
  - ".editorconfig"
---

# Rule: Quality

Aplicada ao rodar análise de qualidade ou configurar ferramentas de build.

---

## Ferramentas

| Ferramenta | Versão | Propósito                             | Comando                   |
|------------|--------|---------------------------------------|---------------------------|
| detekt     | 1.23.8 | Análise estática — bugs e code smells | `./gradlew detekt`        |
| ktlint     | 14.2.0 | Formatação e estilo Kotlin            | `./gradlew ktlintCheck`   |
| Kover      | 0.9.1  | Cobertura de testes (mín. 90%)        | `./gradlew koverVerify`   |

---

## detekt

Configuração em `config/detekt.yml`.

---

## ktlint

Sem configuração customizada — usa os defaults do ktlint (estilo Kotlin oficial).

```
./gradlew ktlintCheck    # verifica
./gradlew ktlintFormat   # corrige automaticamente
```

Hook de pre-commit: `./gradlew addKtlintFormatGitPreCommitHook`

---

## Cobertura (Kover)

Threshold mínimo: **90% de linhas** — `./gradlew koverVerify`.

Relatório HTML: `./gradlew koverHtmlReport` → `build/reports/kover/html/`

**Excluídos da cobertura:**
- `PoppinApplicationKt` — entry point sem lógica
- `*.infrastructure.config.*` — beans de configuração Spring
- `*.infrastructure.input.rest.*.request.*` e `*.response.*` — DTOs puros
- `*Entity`, `*Embeddable` — mapeamento JPA
- `*.infrastructure.output.graph.*` — Neo4j (auto-implementado pelo Spring Data)
- `*.infrastructure.output.authentication.*` — gateway Firebase (sempre mockado em testes)

**O que deve ser coberto:**
- Todo `domain/` — entidades, value objects, factories
- Todo `application/` — use cases
- Controllers e repositórios PostgreSQL — cobertos pelos testes de integração
