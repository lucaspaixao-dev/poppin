package io.github.lucaspaixaodev.poppin.infrastructure.output.authentication.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import io.github.lucaspaixaodev.poppin.domain.exception.AuthGatewayException
import io.github.lucaspaixaodev.poppin.domain.user.User
import io.github.lucaspaixaodev.poppin.domain.user.gateway.AuthGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FirebaseAuthGateway(
    private val firebaseAuth: FirebaseAuth,
) : AuthGateway {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun createUser(user: User): String {
        log.info("Creating Firebase user - uid={} email={}", user.id, user.email)
        val request =
            UserRecord
                .CreateRequest()
                .setUid(user.id)
                .setEmail(user.email)
                .setDisplayName(user.name)
                .setPhotoUrl(user.profilePhoto)
                .setDisabled(false)

        return try {
            val uid = firebaseAuth.createUser(request).uid
            log.info("Firebase user created - uid={}", uid)
            uid
        } catch (e: FirebaseAuthException) {
            log.error("Failed to create Firebase user - email={} error={}", user.email, e.message)
            throw AuthGatewayException.UserCreationFailed(user.email, e)
        }
    }

    override fun existsByEmail(email: String): Boolean {
        log.info("Checking Firebase user existence - email={}", email)
        return try {
            firebaseAuth.getUserByEmail(email)
            true
        } catch (e: FirebaseAuthException) {
            if (e.authErrorCode.name == USER_NOT_FOUND) {
                false
            } else {
                log.error("Failed to check Firebase user existence - email={} error={}", email, e.message)
                throw AuthGatewayException.UserUpdateFailed(email, e)
            }
        }
    }

    override fun enableUser(uid: String) {
        log.info("Enabling Firebase user - uid={}", uid)
        val request = UserRecord.UpdateRequest(uid).setDisabled(false)
        try {
            firebaseAuth.updateUser(request)
            log.info("Firebase user enabled - uid={}", uid)
        } catch (e: FirebaseAuthException) {
            log.error("Failed to enable Firebase user - uid={} error={}", uid, e.message)
            throw AuthGatewayException.UserUpdateFailed(uid, e)
        }
    }

    override fun disableUser(uid: String) {
        log.info("Disabling Firebase user - uid={}", uid)
        val request = UserRecord.UpdateRequest(uid).setDisabled(true)
        try {
            firebaseAuth.updateUser(request)
            log.info("Firebase user disabled - uid={}", uid)
        } catch (e: FirebaseAuthException) {
            log.error("Failed to disable Firebase user - uid={} error={}", uid, e.message)
            throw AuthGatewayException.UserUpdateFailed(uid, e)
        }
    }

    companion object {
        const val USER_NOT_FOUND = "USER_NOT_FOUND"
    }
}
