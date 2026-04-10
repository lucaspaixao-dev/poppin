package io.github.lucaspaixaodev.poppin.domain.user.service

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class GetCurrentUserServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    private val service by lazy { GetCurrentUserService(userRepository) }

    private fun buildUser(id: String = "user-123") =
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
    fun `returns user when found`() {
        val user = buildUser()
        `when`(userRepository.findById("user-123")).thenReturn(user)

        val result = service.execute("user-123")

        assertThat(result.id).isEqualTo("user-123")
        assertThat(result.email).isEqualTo("lucas@email.com")
    }

    @Test
    fun `throws NotFound when user does not exist`() {
        `when`(userRepository.findById("unknown-id")).thenReturn(null)

        assertThatExceptionOfType(UserException.NotFound::class.java).isThrownBy {
            service.execute("unknown-id")
        }
    }
}
