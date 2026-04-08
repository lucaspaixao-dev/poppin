package io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.request

import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.SocialMedia
import io.github.lucaspaixaodev.poppin.domain.user.input.CreateUserInput
import io.github.lucaspaixaodev.poppin.domain.user.input.LocationInput
import io.github.lucaspaixaodev.poppin.domain.user.input.SocialMediaInput
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateUserRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    val name: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Username is required")
    @field:Pattern(
        regexp = "^[a-z0-9_]{3,30}$",
        message = "Username must be 3–30 characters: lowercase letters, numbers and underscores only"
    )
    val username: String,

    @field:NotNull(message = "Gender is required")
    val gender: Gender,

    @field:NotNull(message = "Birthdate is required")
    @field:Past(message = "Birthdate must be in the past")
    val birthdate: LocalDate,

    @field:Size(max = 500, message = "Bio cannot exceed 500 characters")
    val bio: String? = null,

    @field:Pattern(
        regexp = "^[a-z0-9_]{3,30}$",
        message = "Social name must be 3–30 characters: lowercase letters, numbers and underscores only"
    )
    val socialName: String? = null,

    @field:Pattern(
        regexp = "^https?://.+",
        message = "Profile photo must be a valid URL"
    )
    val profilePhoto: String? = null,

    @field:Valid
    val location: LocationRequest? = null,

    @field:Valid
    @field:Size(max = 5, message = "A user can have at most 5 social media links")
    val socialMedias: List<SocialMediaRequest> = emptyList()
) {
    fun toInput() = CreateUserInput(
        name = name,
        email = email,
        username = username,
        gender = gender,
        birthdate = birthdate,
        bio = bio,
        socialName = socialName,
        profilePhoto = profilePhoto,
        location = location?.toInput(),
        socialMedias = socialMedias.map { it.toInput() }
    )
}

data class LocationRequest(
    @field:NotBlank(message = "City is required")
    @field:Size(max = 100, message = "City name too long")
    val city: String,

    @field:NotBlank(message = "Country is required")
    @field:Size(max = 100, message = "Country name too long")
    val country: String
) {
    fun toInput() = LocationInput(city = city, country = country)
}

data class SocialMediaRequest(
    val platform: SocialMedia.Platform,

    @field:NotBlank(message = "Social media URL is required")
    @field:Pattern(
        regexp = "^https?://.+",
        message = "Social media URL must be a valid URL"
    )
    val url: String
) {
    fun toInput() = SocialMediaInput(platform = platform, url = url)
}
