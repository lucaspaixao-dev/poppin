package io.github.lucaspaixaodev.poppin.application.user

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.input.CreateUserInput
import io.github.lucaspaixaodev.poppin.domain.user.service.CreateAuthUserService
import io.github.lucaspaixaodev.poppin.domain.user.service.CreateUserGraphService
import io.github.lucaspaixaodev.poppin.domain.user.service.CreateUserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CreateUserUseCaseTest {
    @Mock
    private lateinit var createUserService: CreateUserService

    @Mock
    private lateinit var createAuthUserService: CreateAuthUserService

    @Mock
    private lateinit var createUserGraphService: CreateUserGraphService

    private val useCase by lazy {
        CreateUserUseCase(createUserService, createAuthUserService, createUserGraphService)
    }

    private fun validInput() =
        CreateUserInput(
            name = "Lucas Paixão",
            email = "lucas@email.com",
            username = "lucaspaixao",
            gender = Gender.MALE,
            birthdate = LocalDate.of(1995, 1, 1),
        )

    private fun buildUser() =
        User.reconstitute(
            id = "user-id-123",
            name = "Lucas Paixão",
            email = "lucas@email.com",
            username = "lucaspaixao",
            gender = Gender.MALE,
            birthdate = LocalDate.of(1995, 1, 1),
            socialName = null,
            profilePhoto = null,
            bio = null,
            active = true,
            location = null,
            registeredAt = LocalDateTime.now(),
        )

    @Test
    fun `executes all services and returns output`() {
        val input = validInput()
        val user = buildUser()
        `when`(createUserService.execute(input)).thenReturn(user)

        val output = useCase.execute(input)

        assertThat(output.id).isEqualTo("user-id-123")
        assertThat(output.email).isEqualTo("lucas@email.com")
        verify(createUserService).execute(input)
        verify(createAuthUserService).execute(user)
        verify(createUserGraphService).execute(user)
    }

    @Test
    fun `propagates exception from createUserService without calling downstream services`() {
        val input = validInput()
        `when`(createUserService.execute(input))
            .thenThrow(UserException.AlreadyExists("lucas@email.com"))

        assertThatExceptionOfType(UserException.AlreadyExists::class.java).isThrownBy {
            useCase.execute(input)
        }

        verify(createAuthUserService, never()).execute(any())
        verify(createUserGraphService, never()).execute(any())
    }

    @Test
    fun `propagates exception from createAuthUserService without calling graph service`() {
        val input = validInput()
        val user = buildUser()
        `when`(createUserService.execute(input)).thenReturn(user)
        doThrow(UserException.AlreadyExists("lucas@email.com"))
            .`when`(createAuthUserService)
            .execute(user)

        assertThatExceptionOfType(UserException.AlreadyExists::class.java).isThrownBy {
            useCase.execute(input)
        }

        verify(createUserGraphService, never()).execute(any())
    }
}
