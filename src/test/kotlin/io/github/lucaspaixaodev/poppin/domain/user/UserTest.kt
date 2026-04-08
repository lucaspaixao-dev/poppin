package io.github.lucaspaixaodev.poppin.domain.user

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.input.CreateUserInput
import io.github.lucaspaixaodev.poppin.domain.user.input.LocationInput
import io.github.lucaspaixaodev.poppin.domain.user.input.SocialMediaInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

            assertTrue(result.isSuccess)
            val user = result.getOrThrow()
            assertNotNull(user.id)
            assertEquals("Lucas Paixão", user.name)
            assertEquals("lucas@email.com", user.email)
            assertEquals("lucaspaixao", user.username)
            assertTrue(user.active)
            assertNull(user.socialName)
            assertNull(user.profilePhoto)
            assertNull(user.location)
            assertNull(user.bio)
        }

        @Test
        fun `creates user with all optional fields`() {
            val input = validInput().copy(
                socialName = "lucaspaixao",
                profilePhoto = "https://example.com/photo.jpg",
                location = LocationInput("São Paulo", "Brazil"),
                bio = "Just a person",
                socialMedias = listOf(
                    SocialMediaInput(SocialMedia.Platform.INSTAGRAM, "https://instagram.com/lucas")
                )
            )

            val result = User.create(input)

            assertTrue(result.isSuccess)
            val user = result.getOrThrow()
            assertEquals("lucaspaixao", user.socialName)
            assertEquals("https://example.com/photo.jpg", user.profilePhoto)
            assertEquals("São Paulo", user.location!!.city)
            assertEquals("Just a person", user.bio)
            assertEquals(1, user.socialMedias.size)
        }

        @Test
        fun `fails when name is blank`() {
            val result = User.create(validInput(name = "  "))

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidName> { result.getOrThrow() }
        }

        @Test
        fun `fails when name is shorter than 2 characters`() {
            val result = User.create(validInput(name = "A"))

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidName> { result.getOrThrow() }
        }

        @Test
        fun `fails when name exceeds 100 characters`() {
            val result = User.create(validInput(name = "A".repeat(101)))

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidName> { result.getOrThrow() }
        }

        @Test
        fun `fails when email format is invalid`() {
            val result = User.create(validInput(email = "not-an-email"))

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidEmail> { result.getOrThrow() }
        }

        @Test
        fun `fails when username does not match pattern`() {
            val result = User.create(validInput(username = "Lu"))

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidUsername> { result.getOrThrow() }
        }

        @Test
        fun `fails when username contains uppercase letters`() {
            val result = User.create(validInput(username = "LucasPaixao"))

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidUsername> { result.getOrThrow() }
        }

        @Test
        fun `fails when birthdate is today`() {
            val result = User.create(validInput(birthdate = LocalDate.now()))

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidBirthdate> { result.getOrThrow() }
        }

        @Test
        fun `fails when birthdate is in the future`() {
            val result = User.create(validInput(birthdate = LocalDate.now().plusDays(1)))

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidBirthdate> { result.getOrThrow() }
        }

        @Test
        fun `fails when socialName does not match pattern`() {
            val input = validInput().copy(socialName = "Invalid Name!")

            val result = User.create(input)

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidSocialName> { result.getOrThrow() }
        }

        @Test
        fun `fails when location city is blank`() {
            val input = validInput().copy(location = LocationInput("", "Brazil"))

            val result = User.create(input)

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidLocation> { result.getOrThrow() }
        }

        @Test
        fun `fails when location country is blank`() {
            val input = validInput().copy(location = LocationInput("São Paulo", ""))

            val result = User.create(input)

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidLocation> { result.getOrThrow() }
        }

        @Test
        fun `fails when social medias exceed 5`() {
            val medias = (1..6).map {
                SocialMediaInput(SocialMedia.Platform.OTHER, "https://example.com/$it")
            }
            val input = validInput().copy(socialMedias = medias)

            val result = User.create(input)

            assertTrue(result.isFailure)
            assertThrows<UserException.InvalidSocialMedia> { result.getOrThrow() }
        }

        @Test
        fun `fails when bio exceeds 500 characters`() {
            val input = validInput().copy(bio = "A".repeat(501))

            val result = User.create(input)

            assertTrue(result.isFailure)
        }
    }

    @Nested
    inner class Mutations {

        private fun buildUser() = User.create(validInput()).getOrThrow()

        @Test
        fun `deactivate sets active to false`() {
            val user = buildUser()

            user.deactivate()

            assertFalse(user.active)
        }

        @Test
        fun `activate sets active to true after deactivation`() {
            val user = buildUser()
            user.deactivate()

            user.activate()

            assertTrue(user.active)
        }

        @Test
        fun `updateSocialName succeeds with valid value`() {
            val user = buildUser()

            val result = user.updateSocialName("new_name_01")

            assertTrue(result.isSuccess)
            assertEquals("new_name_01", user.socialName)
        }

        @Test
        fun `updateSocialName fails with invalid value`() {
            val user = buildUser()

            val result = user.updateSocialName("Invalid Name!")

            assertTrue(result.isFailure)
            assertNull(user.socialName)
        }

        @Test
        fun `updateProfilePhoto succeeds with valid url`() {
            val user = buildUser()

            val result = user.updateProfilePhoto("https://cdn.example.com/photo.png")

            assertTrue(result.isSuccess)
            assertEquals("https://cdn.example.com/photo.png", user.profilePhoto)
        }

        @Test
        fun `updateLocation updates location`() {
            val user = buildUser()
            val location = Location("Rio de Janeiro", "Brazil")

            val result = user.updateLocation(location)

            assertTrue(result.isSuccess)
            assertEquals("Rio de Janeiro", user.location!!.city)
        }
    }

    @Nested
    inner class LocationValueObject {

        @Test
        fun `throws when city is blank`() {
            assertThrows<UserException.InvalidLocation> {
                Location("", "Brazil")
            }
        }

        @Test
        fun `throws when country is blank`() {
            assertThrows<UserException.InvalidLocation> {
                Location("São Paulo", "")
            }
        }

        @Test
        fun `throws when city exceeds 100 characters`() {
            assertThrows<UserException.InvalidLocation> {
                Location("A".repeat(101), "Brazil")
            }
        }

        @Test
        fun `throws when country exceeds 100 characters`() {
            assertThrows<UserException.InvalidLocation> {
                Location("São Paulo", "B".repeat(101))
            }
        }

        @Test
        fun `creates valid location`() {
            val location = Location("São Paulo", "Brazil")

            assertEquals("São Paulo", location.city)
            assertEquals("Brazil", location.country)
        }
    }

    @Nested
    inner class SocialMediaValueObject {

        @Test
        fun `throws when url is blank`() {
            assertThrows<UserException.InvalidSocialMedia> {
                SocialMedia(SocialMedia.Platform.INSTAGRAM, "")
            }
        }

        @Test
        fun `creates valid social media`() {
            val sm = SocialMedia(SocialMedia.Platform.INSTAGRAM, "https://instagram.com/lucas")

            assertEquals(SocialMedia.Platform.INSTAGRAM, sm.platform)
            assertEquals("https://instagram.com/lucas", sm.url)
        }
    }
}
