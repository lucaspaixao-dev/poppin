package io.github.lucaspaixaodev.poppin.domain.user.service

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.gateway.AuthGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CreateAuthUserServiceTest {

    @Mock
    private lateinit var authGateway: AuthGateway

    private val service by lazy { CreateAuthUserService(authGateway) }

    private fun buildUser() = User.reconstitute(
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
    fun `creates auth user when email is not registered`() {
        val user = buildUser()
        `when`(authGateway.existsByEmail("lucas@email.com")).thenReturn(false)

        service.execute(user)

        verify(authGateway).createUser(user)
    }

    @Test
    fun `throws AlreadyExists when email is already in auth provider`() {
        val user = buildUser()
        `when`(authGateway.existsByEmail("lucas@email.com")).thenReturn(true)

        assertThrows<UserException.AlreadyExists> {
            service.execute(user)
        }

        verify(authGateway, never()).createUser(any())
    }
}
