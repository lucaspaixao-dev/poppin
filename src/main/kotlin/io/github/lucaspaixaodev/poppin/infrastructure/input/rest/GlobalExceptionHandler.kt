package io.github.lucaspaixaodev.poppin.infrastructure.input.rest

import io.github.lucaspaixaodev.poppin.domain.exception.AuthGatewayException
import io.github.lucaspaixaodev.poppin.domain.exception.RepositoryException
import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    // ── User ────────────────────────────────────────────────────────────────

    @ExceptionHandler(UserException.NotFound::class)
    fun handleUserNotFound(ex: UserException.NotFound): ProblemDetail {
        log.warn("User not found: {}", ex.message)
        return problem(HttpStatus.NOT_FOUND, ex.message)
    }

    @ExceptionHandler(UserException.AlreadyExists::class)
    fun handleUserAlreadyExists(ex: UserException.AlreadyExists): ProblemDetail {
        log.warn("User already exists: {}", ex.message)
        return problem(HttpStatus.CONFLICT, ex.message)
    }

    @ExceptionHandler(UserException.UsernameAlreadyExists::class)
    fun handleUsernameAlreadyExists(ex: UserException.UsernameAlreadyExists): ProblemDetail {
        log.warn("Username already taken: {}", ex.message)
        return problem(HttpStatus.CONFLICT, ex.message)
    }

    @ExceptionHandler(
        UserException.InvalidEmail::class,
        UserException.InvalidName::class,
        UserException.InvalidSocialName::class,
        UserException.InvalidProfilePhoto::class,
        UserException.InvalidLocation::class,
        UserException.InvalidSocialMedia::class,
        UserException.InvalidUsername::class,
        UserException.InvalidBirthdate::class,
    )
    fun handleUserValidation(ex: UserException): ProblemDetail {
        log.warn("User validation error: {}", ex.message)
        return problem(HttpStatus.BAD_REQUEST, ex.message)
    }

    // ── Request binding / validation ────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ProblemDetail {
        val detail = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("Validation failed: {}", detail)
        return problem(HttpStatus.BAD_REQUEST, detail)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ProblemDetail {
        log.warn("Unreadable request body: {}", ex.message)
        return problem(HttpStatus.BAD_REQUEST, "Invalid request body")
    }

    // ── Infrastructure ──────────────────────────────────────────────────────

    @ExceptionHandler(RepositoryException::class)
    fun handleRepositoryError(ex: RepositoryException): ProblemDetail {
        log.error("Repository error: {}", ex.message, ex)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ex.message)
    }

    @ExceptionHandler(AuthGatewayException::class)
    fun handleAuthGatewayError(ex: AuthGatewayException): ProblemDetail {
        log.error("Auth gateway error: {}", ex.message, ex)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ex.message)
    }

    // ── Fallback ────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ProblemDetail {
        log.error("Unhandled exception: {}", ex.message, ex)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private fun problem(
        status: HttpStatus,
        detail: String?,
    ): ProblemDetail = ProblemDetail.forStatusAndDetail(status, detail ?: "No details available")
}
