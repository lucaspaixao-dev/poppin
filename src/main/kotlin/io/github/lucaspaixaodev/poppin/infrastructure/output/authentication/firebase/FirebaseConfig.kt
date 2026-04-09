package io.github.lucaspaixaodev.poppin.infrastructure.output.authentication.firebase

import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Date

@Configuration
class FirebaseConfig {
    @Value("\${firebase.project-id}")
    private lateinit var projectId: String

    @Value("\${firebase.emulator-host:#{null}}")
    private var emulatorHost: String? = null

    @Bean
    fun firebaseApp(): FirebaseApp {
        val credentials =
            if (emulatorHost != null) {
                object : GoogleCredentials(AccessToken("emulator-fake-token", Date(Long.MAX_VALUE))) {
                    override fun refreshAccessToken(): AccessToken = AccessToken("emulator-fake-token", Date(Long.MAX_VALUE))
                }
            } else {
                GoogleCredentials.getApplicationDefault()
            }

        val options =
            FirebaseOptions
                .builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()

        return if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
    }

    @Bean
    fun firebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(firebaseApp)
}
