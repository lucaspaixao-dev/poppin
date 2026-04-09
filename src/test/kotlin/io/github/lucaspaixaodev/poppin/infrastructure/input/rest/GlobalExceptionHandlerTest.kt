package io.github.lucaspaixaodev.poppin.infrastructure.input.rest

import io.github.lucaspaixaodev.poppin.domain.exception.AuthGatewayException
import io.github.lucaspaixaodev.poppin.domain.exception.RepositoryException
import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    private data class RequestStub(
        val email: String = "",
    )

    @Test
    fun `handleUserNotFound returns 404`() {
        val ex = UserException.NotFound("user-123")
        val detail = handler.handleUserNotFound(ex)
        assertThat(detail.status).isEqualTo(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `handleUserAlreadyExists returns 409`() {
        val ex = UserException.AlreadyExists("lucas@email.com")
        val detail = handler.handleUserAlreadyExists(ex)
        assertThat(detail.status).isEqualTo(HttpStatus.CONFLICT.value())
    }

    @Test
    fun `handleUsernameAlreadyExists returns 409`() {
        val ex = UserException.UsernameAlreadyExists("lucaspaixao")
        val detail = handler.handleUsernameAlreadyExists(ex)
        assertThat(detail.status).isEqualTo(HttpStatus.CONFLICT.value())
    }

    @Test
    fun `handleUserValidation returns 400 for InvalidEmail`() {
        val ex = UserException.InvalidEmail("bad-email")
        val detail = handler.handleUserValidation(ex)
        assertThat(detail.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `handleMethodArgumentNotValid returns 400`() {
        val bindingResult = BeanPropertyBindingResult(RequestStub(), "target")
        bindingResult.rejectValue("email", "NotBlank", "must not be blank")
        val methodParameter =
            org.springframework.core.MethodParameter(
                String::class.java.getMethod("length"),
                -1,
            )
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)
        val detail = handler.handleMethodArgumentNotValid(ex)
        assertThat(detail.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `handleHttpMessageNotReadable returns 400`() {
        val ex = HttpMessageNotReadableException("bad body", MockHttpInputMessage(ByteArray(0)))
        val detail = handler.handleHttpMessageNotReadable(ex)
        assertThat(detail.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(detail.detail).isEqualTo("Invalid request body")
    }

    @Test
    fun `handleRepositoryError returns 500`() {
        val ex = RepositoryException.CreationFailed("db error", RuntimeException("connection refused"))
        val detail = handler.handleRepositoryError(ex)
        assertThat(detail.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
    }

    @Test
    fun `handleAuthGatewayError returns 500`() {
        val ex = AuthGatewayException.UserCreationFailed("lucas@email.com", RuntimeException("firebase down"))
        val detail = handler.handleAuthGatewayError(ex)
        assertThat(detail.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
    }

    @Test
    fun `handleUnexpected returns 500`() {
        val ex = RuntimeException("something went wrong")
        val detail = handler.handleUnexpected(ex)
        assertThat(detail.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        assertThat(detail.detail).isEqualTo("An unexpected error occurred")
    }
}
