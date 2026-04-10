package io.github.lucaspaixaodev.poppin.application.user

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.service.GetCurrentUserService
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
class GetCurrentUserUseCaseTest {
    @Mock
    private lateinit var getCurrentUserService: GetCurrentUserService

    private val useCase by lazy { GetCurrentUserUseCase(getCurrentUserService) }

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
    fun `returns output with correct fields`() {
        val user = buildUser()
        `when`(getCurrentUserService.execute("user-123")).thenReturn(user)

        val output = useCase.execute("user-123")

        assertThat(output.id).isEqualTo("user-123")
        assertThat(output.name).isEqualTo("Lucas Paixão")
        assertThat(output.email).isEqualTo("lucas@email.com")
        assertThat(output.username).isEqualTo("lucaspaixao")
        assertThat(output.gender).isEqualTo(Gender.MALE)
        assertThat(output.active).isTrue()
        assertThat(output.location).isNull()
        assertThat(output.socialMedias).isEmpty()
    }

    @Test
    fun `propagates NotFound exception from service`() {
        `when`(getCurrentUserService.execute("ghost")).thenThrow(UserException.NotFound("ghost"))

        assertThatExceptionOfType(UserException.NotFound::class.java).isThrownBy {
            useCase.execute("ghost")
        }
    }
}
