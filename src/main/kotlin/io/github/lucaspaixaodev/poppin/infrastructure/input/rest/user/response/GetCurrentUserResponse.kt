package io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.response

import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.output.GetCurrentUserOutput
import java.time.LocalDate
import java.time.LocalDateTime

data class GetCurrentUserResponse(
    val id: String,
    val name: String,
    val email: String,
    val username: String,
    val gender: Gender,
    val birthdate: LocalDate,
    val socialName: String?,
    val profilePhoto: String?,
    val bio: String?,
    val location: LocationResponse?,
    val socialMedias: List<SocialMediaResponse>,
    val active: Boolean,
    val registeredAt: LocalDateTime,
) {
    companion object {
        fun fromOutput(output: GetCurrentUserOutput) =
            GetCurrentUserResponse(
                id = output.id,
                name = output.name,
                email = output.email,
                username = output.username,
                gender = output.gender,
                birthdate = output.birthdate,
                socialName = output.socialName,
                profilePhoto = output.profilePhoto,
                bio = output.bio,
                location = output.location?.let { LocationResponse(it.city, it.country) },
                socialMedias = output.socialMedias.map { SocialMediaResponse(it.platform, it.url) },
                active = output.active,
                registeredAt = output.registeredAt,
            )
    }
}
