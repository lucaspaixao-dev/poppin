package io.github.lucaspaixaodev.poppin.infrastructure.output.database.postgres.user

import io.github.lucaspaixaodev.poppin.domain.user.Gender
import io.github.lucaspaixaodev.poppin.domain.user.Location
import io.github.lucaspaixaodev.poppin.domain.user.SocialMedia
import io.github.lucaspaixaodev.poppin.domain.user.User
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class UserEntity(
    @Id
    val id: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false, unique = true)
    val username: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val gender: Gender,

    @Column(nullable = false)
    val birthdate: LocalDate,

    @Column(name = "social_name")
    val socialName: String?,

    @Column(name = "profile_photo")
    val profilePhoto: String?,

    @Column(columnDefinition = "TEXT")
    val bio: String?,

    @Embedded
    val location: LocationEmbeddable?,

    @Column(nullable = false)
    val active: Boolean,

    @Column(name = "registered_at", nullable = false)
    val registeredAt: LocalDateTime,

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_social_medias", joinColumns = [JoinColumn(name = "user_id")])
    val socialMedias: List<SocialMediaEmbeddable> = emptyList()
) {
    fun toDomain(): User = User.reconstitute(
        id = id,
        name = name,
        email = email,
        username = username,
        gender = gender,
        birthdate = birthdate,
        socialName = socialName,
        profilePhoto = profilePhoto,
        bio = bio,
        active = active,
        location = location?.toDomain(),
        socialMedias = socialMedias.map { it.toDomain() },
        registeredAt = registeredAt
    )

    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            username = user.username,
            gender = user.gender,
            birthdate = user.birthdate,
            socialName = user.socialName,
            profilePhoto = user.profilePhoto,
            bio = user.bio,
            location = user.location?.let { LocationEmbeddable.fromDomain(it) },
            active = user.active,
            registeredAt = user.registeredAt,
            socialMedias = user.socialMedias.map { SocialMediaEmbeddable.fromDomain(it) }
        )
    }
}

@Embeddable
class LocationEmbeddable(
    @Column(name = "location_city")
    val city: String = "",

    @Column(name = "location_country")
    val country: String = ""
) {
    fun toDomain(): Location = Location(city = city, country = country)

    companion object {
        fun fromDomain(location: Location): LocationEmbeddable = LocationEmbeddable(
            city = location.city,
            country = location.country
        )
    }
}

@Embeddable
class SocialMediaEmbeddable(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val platform: SocialMedia.Platform = SocialMedia.Platform.OTHER,

    @Column(nullable = false)
    val url: String = ""
) {
    fun toDomain(): SocialMedia = SocialMedia(platform = platform, url = url)

    companion object {
        fun fromDomain(socialMedia: SocialMedia): SocialMediaEmbeddable = SocialMediaEmbeddable(
            platform = socialMedia.platform,
            url = socialMedia.url
        )
    }
}
