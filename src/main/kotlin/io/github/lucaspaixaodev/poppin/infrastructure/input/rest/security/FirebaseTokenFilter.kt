package io.github.lucaspaixaodev.poppin.infrastructure.input.rest.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class FirebaseTokenFilter(
    private val firebaseAuth: FirebaseAuth,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val header = request.getHeader(AUTHORIZATION_HEADER)
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            val token = header.removePrefix(BEARER_PREFIX)
            try {
                val decoded = firebaseAuth.verifyIdToken(token)
                val auth = UsernamePasswordAuthenticationToken(decoded.uid, null, emptyList())
                SecurityContextHolder.getContext().authentication = auth
                log.debug("Firebase token verified - uid={}", decoded.uid)
            } catch (e: FirebaseAuthException) {
                log.warn("Invalid Firebase token: {}", e.message)
                SecurityContextHolder.clearContext()
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token")
                return
            }
        }
        chain.doFilter(request, response)
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
