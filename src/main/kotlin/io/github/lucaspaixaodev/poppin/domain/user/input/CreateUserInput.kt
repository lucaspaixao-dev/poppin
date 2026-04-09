package io.github.lucaspaixaodev.poppin.domain.user.input

import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.SocialMedia
import java.time.LocalDate

data class CreateUserInput(
    val name: String,
    val email: String,
    val username: String,
    val gender: Gender,
    val birthdate: LocalDate,
    val bio: String? = null,
    val socialName: String? = null,
    val profilePhoto: String? = null,
    val location: LocationInput? = null,
    val socialMedias: List<SocialMediaInput> = emptyList(),
)

data class LocationInput(
    val city: String,
    val country: String,
)

data class SocialMediaInput(
    val platform: SocialMedia.Platform,
    val url: String,
)
