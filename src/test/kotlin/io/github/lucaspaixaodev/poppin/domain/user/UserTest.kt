package io.github.lucaspaixaodev.poppin.domain.user

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.input.CreateUserInput
import io.github.lucaspaixaodev.poppin.domain.user.input.LocationInput
import io.github.lucaspaixaodev.poppin.domain.user.input.SocialMediaInput
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UserTest {
    private fun validInput(
        name: String = "Lucas Paixão",
        email: String = "lucas@email.com",
        username: String = "lucaspaixao",
        gender: Gender = Gender.MALE,
        birthdate: LocalDate = LocalDate.of(1995, 1, 1),
    ) = CreateUserInput(
        name = name,
        email = email,
        username = username,
        gender = gender,
        birthdate = birthdate,
    )

    @Nested
    inner class Create {
        @Test
        fun `creates user with valid minimal input`() {
            val result = User.create(validInput())

            assertThat(result.isSuccess).isTrue()
            val user = result.getOrThrow()
            assertThat(user.id).isNotNull()
            assertThat(user.name).isEqualTo("Lucas Paixão")
            assertThat(user.email).isEqualTo("lucas@email.com")
            assertThat(user.username).isEqualTo("lucaspaixao")
            assertThat(user.active).isTrue()
            assertThat(user.socialName).isNull()
            assertThat(user.profilePhoto).isNull()
            assertThat(user.location).isNull()
            assertThat(user.bio).isNull()
        }

        @Test
        fun `creates user with all optional fields`() {
            val input =
                validInput().copy(
                    socialName = "lucaspaixao",
                    profilePhoto = "https://example.com/photo.jpg",
                    location = LocationInput("São Paulo", "Brazil"),
                    bio = "Just a person",
                    socialMedias =
                        listOf(
                            SocialMediaInput(SocialMedia.Platform.INSTAGRAM, "https://instagram.com/lucas"),
                        ),
                )

            val result = User.create(input)

            assertThat(result.isSuccess).isTrue()
            val user = result.getOrThrow()
            assertThat(user.socialName).isEqualTo("lucaspaixao")
            assertThat(user.profilePhoto).isEqualTo("https://example.com/photo.jpg")
            assertThat(user.location!!.city).isEqualTo("São Paulo")
            assertThat(user.bio).isEqualTo("Just a person")
            assertThat(user.socialMedias).hasSize(1)
        }

        @Test
        fun `fails when name is blank`() {
            val result = User.create(validInput(name = "  "))

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidName::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when name is shorter than 2 characters`() {
            val result = User.create(validInput(name = "A"))

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidName::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when name exceeds 100 characters`() {
            val result = User.create(validInput(name = "A".repeat(101)))

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidName::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when email format is invalid`() {
            val result = User.create(validInput(email = "not-an-email"))

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidEmail::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when username does not match pattern`() {
            val result = User.create(validInput(username = "Lu"))

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidUsername::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when username contains uppercase letters`() {
            val result = User.create(validInput(username = "LucasPaixao"))

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidUsername::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when birthdate is today`() {
            val result = User.create(validInput(birthdate = LocalDate.now()))

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidBirthdate::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when birthdate is in the future`() {
            val result = User.create(validInput(birthdate = LocalDate.now().plusDays(1)))

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidBirthdate::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when socialName does not match pattern`() {
            val input = validInput().copy(socialName = "Invalid Name!")

            val result = User.create(input)

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidSocialName::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when location city is blank`() {
            val input = validInput().copy(location = LocationInput("", "Brazil"))

            val result = User.create(input)

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidLocation::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when location country is blank`() {
            val input = validInput().copy(location = LocationInput("São Paulo", ""))

            val result = User.create(input)

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidLocation::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when social medias exceed 5`() {
            val medias =
                (1..6).map {
                    SocialMediaInput(SocialMedia.Platform.OTHER, "https://example.com/$it")
                }
            val input = validInput().copy(socialMedias = medias)

            val result = User.create(input)

            assertThat(result.isFailure).isTrue()
            assertThatExceptionOfType(UserException.InvalidSocialMedia::class.java).isThrownBy { result.getOrThrow() }
        }

        @Test
        fun `fails when bio exceeds 500 characters`() {
            val input = validInput().copy(bio = "A".repeat(501))

            val result = User.create(input)

            assertThat(result.isFailure).isTrue()
        }
    }

    @Nested
    inner class Mutations {
        private fun buildUser() = User.create(validInput()).getOrThrow()

        @Test
        fun `deactivate sets active to false`() {
            val user = buildUser()

            user.deactivate()

            assertThat(user.active).isFalse()
        }

        @Test
        fun `activate sets active to true after deactivation`() {
            val user = buildUser()
            user.deactivate()

            user.activate()

            assertThat(user.active).isTrue()
        }

        @Test
        fun `updateSocialName succeeds with valid value`() {
            val user = buildUser()

            val result = user.updateSocialName("new_name_01")

            assertThat(result.isSuccess).isTrue()
            assertThat(user.socialName).isEqualTo("new_name_01")
        }

        @Test
        fun `updateSocialName fails with invalid value`() {
            val user = buildUser()

            val result = user.updateSocialName("Invalid Name!")

            assertThat(result.isFailure).isTrue()
            assertThat(user.socialName).isNull()
        }

        @Test
        fun `updateProfilePhoto succeeds with valid url`() {
            val user = buildUser()

            val result = user.updateProfilePhoto("https://cdn.example.com/photo.png")

            assertThat(result.isSuccess).isTrue()
            assertThat(user.profilePhoto).isEqualTo("https://cdn.example.com/photo.png")
        }

        @Test
        fun `updateLocation updates location`() {
            val user = buildUser()
            val location = Location("Rio de Janeiro", "Brazil")

            val result = user.updateLocation(location)

            assertThat(result.isSuccess).isTrue()
            assertThat(user.location!!.city).isEqualTo("Rio de Janeiro")
        }
    }

    @Nested
    inner class LocationValueObject {
        @Test
        fun `throws when city is blank`() {
            assertThatExceptionOfType(UserException.InvalidLocation::class.java).isThrownBy {
                Location("", "Brazil")
            }
        }

        @Test
        fun `throws when country is blank`() {
            assertThatExceptionOfType(UserException.InvalidLocation::class.java).isThrownBy {
                Location("São Paulo", "")
            }
        }

        @Test
        fun `throws when city exceeds 100 characters`() {
            assertThatExceptionOfType(UserException.InvalidLocation::class.java).isThrownBy {
                Location("A".repeat(101), "Brazil")
            }
        }

        @Test
        fun `throws when country exceeds 100 characters`() {
            assertThatExceptionOfType(UserException.InvalidLocation::class.java).isThrownBy {
                Location("São Paulo", "B".repeat(101))
            }
        }

        @Test
        fun `creates valid location`() {
            val location = Location("São Paulo", "Brazil")

            assertThat(location.city).isEqualTo("São Paulo")
            assertThat(location.country).isEqualTo("Brazil")
        }
    }

    @Nested
    inner class SocialMediaValueObject {
        @Test
        fun `throws when url is blank`() {
            assertThatExceptionOfType(UserException.InvalidSocialMedia::class.java).isThrownBy {
                SocialMedia(SocialMedia.Platform.INSTAGRAM, "")
            }
        }

        @Test
        fun `creates valid social media`() {
            val sm = SocialMedia(SocialMedia.Platform.INSTAGRAM, "https://instagram.com/lucas")

            assertThat(sm.platform).isEqualTo(SocialMedia.Platform.INSTAGRAM)
            assertThat(sm.url).isEqualTo("https://instagram.com/lucas")
        }
    }
}
