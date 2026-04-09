package io.github.lucaspaixaodev.poppin.domain.user

import io.github.lucaspaixaodev.poppin.domain.exception.UserException
import io.github.lucaspaixaodev.poppin.domain.user.input.CreateUserInput
import java.net.URI
import java.net.URISyntaxException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class User private constructor(
    val id: String,
    val name: String,
    val email: String,
    val username: String,
    val gender: Gender,
    val birthdate: LocalDate,
    val socialMedias: List<SocialMedia> = mutableListOf(),
    val registeredAt: LocalDateTime,
    private var _socialName: String? = null,
    private var _profilePhoto: String? = null,
    private var _location: Location? = null,
    private var _bio: String? = null,
    private var _active: Boolean = true,
) {
    val active: Boolean get() = _active
    val socialName: String? get() = _socialName
    val profilePhoto: String? get() = _profilePhoto
    val location: Location? get() = _location
    val bio: String? get() = _bio

    fun activate() {
        this._active = true
    }

    fun deactivate() {
        this._active = false
    }

    fun updateSocialName(socialName: String): Result<Unit> {
        val result = validateSocialName(socialName)
        if (result.isSuccess) {
            this._socialName = socialName
        }
        return result
    }

    fun updateProfilePhoto(profilePhoto: String): Result<Unit> {
        val result = validateUrl(profilePhoto)
        if (result.isSuccess) {
            this._profilePhoto = profilePhoto
        }
        return result
    }

    fun updateLocation(location: Location): Result<Unit> {
        this._location = location
        return Result.success(Unit)
    }

    companion object Factory {
        private const val MAX_BIO_LENGTH = 500
        private const val MAX_SOCIAL_MEDIAS = 5

        fun create(input: CreateUserInput): Result<User> =
            validate(input).map {
                User(
                    id = UUID.randomUUID().toString(),
                    name = input.name,
                    email = input.email,
                    username = input.username,
                    gender = input.gender,
                    birthdate = input.birthdate,
                    registeredAt = LocalDateTime.now(),
                    socialMedias = input.socialMedias.map { SocialMedia(it.platform, it.url) },
                    _socialName = input.socialName,
                    _profilePhoto = input.profilePhoto,
                    _location = input.location?.let { Location(it.city, it.country) },
                    _bio = input.bio,
                    _active = true,
                )
            }

        fun reconstitute(
            id: String,
            name: String,
            socialName: String?,
            email: String,
            username: String,
            gender: Gender,
            birthdate: LocalDate,
            profilePhoto: String?,
            bio: String?,
            active: Boolean,
            location: Location?,
            socialMedias: List<SocialMedia> = emptyList(),
            registeredAt: LocalDateTime,
        ): User =
            User(
                id = id,
                name = name,
                email = email,
                username = username,
                gender = gender,
                birthdate = birthdate,
                socialMedias = socialMedias,
                registeredAt = registeredAt,
                _socialName = socialName,
                _profilePhoto = profilePhoto,
                _location = location,
                _bio = bio,
                _active = active,
            )

        private fun validateName(name: String): Result<Unit> {
            if (name.isBlank()) {
                return Result.failure(UserException.InvalidName("Name cannot be blank"))
            }
            if (name.length < 2) {
                return Result.failure(UserException.InvalidName("Name must be at least 2 characters"))
            }
            if (name.length > 100) {
                return Result.failure(UserException.InvalidName("Name too long"))
            }
            return Result.success(Unit)
        }

        private fun validateEmail(email: String): Result<Unit> {
            if (!Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(email)) {
                return Result.failure(UserException.InvalidEmail(email))
            }
            return Result.success(Unit)
        }

        private fun validateSocialName(socialName: String): Result<Unit> {
            if (!Regex("^[a-z0-9_]{3,30}$").matches(socialName)) {
                return Result.failure(UserException.InvalidSocialName(socialName))
            }
            return Result.success(Unit)
        }

        private fun validateUrl(url: String): Result<Unit> {
            try {
                URI(url)
            } catch (_: URISyntaxException) {
                return Result.failure(UserException.InvalidProfilePhoto(url))
            }
            return Result.success(Unit)
        }

        private fun validateSocialMediaUrl(url: String): Result<Unit> {
            try {
                URI(url)
            } catch (_: URISyntaxException) {
                return Result.failure(UserException.InvalidSocialMedia("Invalid social media URL: $url"))
            }
            return Result.success(Unit)
        }

        private fun validateUsername(username: String): Result<Unit> {
            if (!Regex("^[a-z0-9_]{3,30}$").matches(username)) {
                return Result.failure(UserException.InvalidUsername(username))
            }
            return Result.success(Unit)
        }

        private fun validateBirthdate(birthdate: LocalDate): Result<Unit> {
            if (!birthdate.isBefore(LocalDate.now())) {
                return Result.failure(UserException.InvalidBirthdate("Birthdate must be in the past"))
            }
            return Result.success(Unit)
        }

        private fun validateBio(bio: String): Result<Unit> {
            if (bio.length > MAX_BIO_LENGTH) {
                return Result.failure(UserException.InvalidName("Bio cannot exceed $MAX_BIO_LENGTH characters"))
            }
            return Result.success(Unit)
        }

        private fun validate(input: CreateUserInput): Result<Unit> {
            val nameResult = validateName(input.name)
            if (nameResult.isFailure) return nameResult
            val emailResult = validateEmail(input.email)
            if (emailResult.isFailure) return emailResult
            val usernameResult = validateUsername(input.username)
            if (usernameResult.isFailure) return usernameResult
            val birthdateResult = validateBirthdate(input.birthdate)
            if (birthdateResult.isFailure) return birthdateResult
            input.bio?.let {
                val bioResult = validateBio(it)
                if (bioResult.isFailure) return bioResult
            }
            input.socialName?.let {
                val socialNameResult = validateSocialName(it)
                if (socialNameResult.isFailure) return socialNameResult
            }
            input.profilePhoto?.let {
                val urlResult = validateUrl(it)
                if (urlResult.isFailure) return urlResult
            }
            input.location?.let { loc ->
                if (loc.city.isBlank() || loc.country.isBlank()) {
                    return Result.failure(UserException.InvalidLocation("Location city and country cannot be blank"))
                }
            }
            input.socialMedias.forEach { sm ->
                val smUrlResult = validateSocialMediaUrl(sm.url)
                if (smUrlResult.isFailure) return smUrlResult
            }
            if (input.socialMedias.size > MAX_SOCIAL_MEDIAS) {
                return Result.failure(UserException.InvalidSocialMedia("Too many social medias"))
            }
            return Result.success(Unit)
        }
    }
}

enum class Gender { MALE, FEMALE, OTHER }

data class Location(
    val city: String,
    val country: String,
) {
    init {
        if (city.isBlank()) {
            throw UserException.InvalidLocation("City cannot be blank")
        }

        if (city.length > 100) {
            throw UserException.InvalidLocation("City name too long")
        }

        if (country.isBlank()) {
            throw UserException.InvalidLocation("Country cannot be blank")
        }

        if (country.length > 100) {
            throw UserException.InvalidLocation("Country name too long")
        }
    }
}

data class SocialMedia(
    val platform: Platform,
    val url: String,
) {
    init {
        if (url.isBlank()) {
            throw UserException.InvalidSocialMedia("Social media URL cannot be blank")
        }
        try {
            URI(url)
        } catch (_: URISyntaxException) {
            throw UserException.InvalidSocialMedia("Invalid social media URL: $url")
        }
    }

    enum class Platform {
        FACEBOOK,
        TWITTER,
        INSTAGRAM,
        LINKEDIN,
        OTHER,
    }
}
