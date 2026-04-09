package io.github.lucaspaixaodev.poppin.domain.user.service

import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserGraphRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CreateUserGraphServiceTest {
    @Mock
    private lateinit var userGraphRepository: UserGraphRepository

    private val service by lazy { CreateUserGraphService(userGraphRepository) }

    private fun buildUser(id: String = "user-id-123") =
        User.reconstitute(
            id = id,
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
    fun `creates node when user does not exist in graph`() {
        val user = buildUser()
        `when`(userGraphRepository.existsById("user-id-123")).thenReturn(false)

        service.execute(user)

        verify(userGraphRepository).create("user-id-123")
    }

    @Test
    fun `skips creation when user already exists in graph`() {
        val user = buildUser()
        `when`(userGraphRepository.existsById("user-id-123")).thenReturn(true)

        service.execute(user)

        verify(userGraphRepository, never()).create("user-id-123")
    }
}
