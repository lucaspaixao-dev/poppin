package io.github.lucaspaixaodev.poppin.domain.user.output

import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.User
import java.time.LocalDate
import java.time.LocalDateTime

data class GetCurrentUserOutput(
    val id: String,
    val name: String,
    val email: String,
    val username: String,
    val gender: Gender,
    val birthdate: LocalDate,
    val socialName: String?,
    val profilePhoto: String?,
    val bio: String?,
    val location: LocationOutput?,
    val socialMedias: List<SocialMediaOutput>,
    val active: Boolean,
    val registeredAt: LocalDateTime,
) {
    constructor(user: User) : this(
        id = user.id,
        name = user.name,
        email = user.email,
        username = user.username,
        gender = user.gender,
        birthdate = user.birthdate,
        socialName = user.socialName,
        profilePhoto = user.profilePhoto,
        bio = user.bio,
        location = user.location?.let { LocationOutput(it.city, it.country) },
        socialMedias = user.socialMedias.map { SocialMediaOutput(it.platform, it.url) },
        active = user.active,
        registeredAt = user.registeredAt,
    )
}
