package io.github.lucaspaixaodev.poppin.domain.user.output

import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.SocialMedia
import io.github.lucaspaixaodev.poppin.domain.user.User
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateUserOutput(
    val id: String,
    val name: String,
    val email: String,
    val socialName: String?,
    val bio: String?,
    val gender: Gender,
    val birthdate: LocalDate,
    val profilePhoto: String?,
    val location: LocationOutput?,
    val socialMedias: List<SocialMediaOutput>,
    val active: Boolean,
    val registeredAt: LocalDateTime,
    val username: String,
) {
    constructor(user: User) : this(
        id = user.id,
        name = user.name,
        email = user.email,
        socialName = user.socialName,
        profilePhoto = user.profilePhoto,
        location = user.location?.let { LocationOutput(it.city, it.country) },
        socialMedias = user.socialMedias.map { SocialMediaOutput(it.platform, it.url) },
        active = user.active,
        registeredAt = user.registeredAt,
        birthdate = user.birthdate,
        bio = user.bio,
        gender = user.gender,
        username = user.username,
    )
}

data class LocationOutput(
    val city: String,
    val country: String
)

data class SocialMediaOutput(
    val platform: SocialMedia.Platform,
    val url: String
)
