package io.github.lucaspaixaodev.poppin.domain.user.service

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.input.CreateUserInput
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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

@ExtendWith(MockitoExtension::class)
class CreateUserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private val service by lazy { CreateUserService(userRepository) }

    private fun validInput() = CreateUserInput(
        name = "Lucas Paixão",
        email = "lucas@email.com",
        username = "lucaspaixao",
        gender = Gender.MALE,
        birthdate = LocalDate.of(1995, 1, 1),
    )

    @Test
    fun `creates user when email and username are not taken`() {
        `when`(userRepository.existsByEmail("lucas@email.com")).thenReturn(false)
        `when`(userRepository.existsByUsername("lucaspaixao")).thenReturn(false)

        val user = service.execute(validInput())

        assertNotNull(user.id)
        assertEquals("lucas@email.com", user.email)
        verify(userRepository).create(user)
    }

    @Test
    fun `throws AlreadyExists when email is already registered`() {
        `when`(userRepository.existsByEmail("lucas@email.com")).thenReturn(true)

        assertThrows<UserException.AlreadyExists> {
            service.execute(validInput())
        }

        verify(userRepository, never()).create(any())
    }

    @Test
    fun `throws UsernameAlreadyExists when username is taken`() {
        `when`(userRepository.existsByEmail("lucas@email.com")).thenReturn(false)
        `when`(userRepository.existsByUsername("lucaspaixao")).thenReturn(true)

        assertThrows<UserException.UsernameAlreadyExists> {
            service.execute(validInput())
        }

        verify(userRepository, never()).create(any())
    }
}
