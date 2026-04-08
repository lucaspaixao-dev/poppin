package io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.response

import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.SocialMedia
import io.github.lucaspaixaodev.poppin.domain.user.output.CreateUserOutput
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateUserResponse(
    val id: String,
    val name: String,
    val email: String,
    val socialName: String?,
    val profilePhoto: String?,
    val location: LocationResponse?,
    val socialMedias: List<SocialMediaResponse>,
    val active: Boolean,
    val registeredAt: LocalDateTime,
    val username: String,
    val bio: String?,
    val birthdate: LocalDate,
    val gender: Gender
) {
    companion object {
        fun fromOutput(output: CreateUserOutput) = CreateUserResponse(
            id = output.id,
            name = output.name,
            email = output.email,
            socialName = output.socialName,
            profilePhoto = output.profilePhoto,
            location = output.location?.let { LocationResponse(it.city, it.country) },
            socialMedias = output.socialMedias.map { SocialMediaResponse(it.platform, it.url) },
            active = output.active,
            registeredAt = output.registeredAt,
            bio = output.bio,
            username = output.username,
            birthdate = output.birthdate,
            gender = output.gender
        )
    }
}

data class LocationResponse(
    val city: String,
    val country: String
)

data class SocialMediaResponse(
    val platform: SocialMedia.Platform,
    val url: String
)
