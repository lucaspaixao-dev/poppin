package io.github.lucaspaixaodev.poppin.domain.exception

sealed class BaseException(message: String) : RuntimeException(message)

sealed class RepositoryException(message: String) : BaseException(message) {
    class CreationFailed(message: String, cause: String) : RepositoryException("$message: $cause")
}

sealed class AuthGatewayException(message: String) : BaseException(message) {
    class UserCreationFailed(email: String, cause: String) :
        AuthGatewayException("Failed to create auth user for $email: $cause")

    class UserUpdateFailed(uid: String, cause: String) : AuthGatewayException("Failed to update auth user $uid: $cause")
}

sealed class UserException(message: String) : BaseException(message) {
    class InvalidEmail(email: String) : UserException("Invalid Email: $email")
    class InvalidName(message: String) : UserException(message)
    class InvalidSocialName(socialName: String) : UserException("Invalid Social Name: $socialName")
    class InvalidProfilePhoto(url: String) : UserException("Invalid profile photo URL: $url")
    class InvalidLocation(message: String) : UserException(message)
    class InvalidSocialMedia(message: String) : UserException(message)
    class InvalidUsername(username: String) : UserException("Invalid username: $username")
    class InvalidBirthdate(message: String) : UserException(message)
    class NotFound(id: String) : UserException("User not found: $id")
    class AlreadyExists(email: String) : UserException("Email already registered: $email")
    class UsernameAlreadyExists(username: String) : UserException("Username already taken: $username")
}
