package io.github.lucaspaixaodev.poppin.infrastructure.output.authentication.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseConfig {

    @Value("\${firebase.project-id}")
    private lateinit var projectId: String

    @Value("\${firebase.emulator-host:#{null}}")
    private var emulatorHost: String? = null

    @Bean
    fun firebaseApp(): FirebaseApp {
        emulatorHost?.let {
            System.setProperty(FIREBASE_AUTH_EMULATOR_HOST, it)
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId(projectId)
            .build()

        return if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
    }

    @Bean
    fun firebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth =
        FirebaseAuth.getInstance(firebaseApp)

    companion object {
        const val FIREBASE_AUTH_EMULATOR_HOST = "FIREBASE_AUTH_EMULATOR_HOST"
    }
}
