package io.github.lucaspaixaodev.poppin.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.lucaspaixaodev.poppin.infrastructure.input.rest.security.FirebaseTokenFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val firebaseTokenFilter: FirebaseTokenFilter,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.authorizeHttpRequests { auth ->
            auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users")
                .permitAll()
                .anyRequest()
                .authenticated()
        }
        http.addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
        http.exceptionHandling { exceptions ->
            exceptions.authenticationEntryPoint { _, response, _ ->
                writeProblem(response, HttpStatus.UNAUTHORIZED, "Authentication required")
            }
            exceptions.accessDeniedHandler { _, response, _ ->
                writeProblem(response, HttpStatus.FORBIDDEN, "Access denied")
            }
        }
        return http.build()
    }

    private fun writeProblem(
        response: HttpServletResponse,
        status: HttpStatus,
        detail: String,
    ) {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.writer.write(objectMapper.writeValueAsString(problem))
    }
}
