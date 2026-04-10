package io.github.lucaspaixaodev.poppin.infrastructure.input.rest.security

import io.github.lucaspaixaodev.poppin.domain.exception.AuthGatewayException
import io.github.lucaspaixaodev.poppin.domain.user.gateway.AuthGateway
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
class FirebaseTokenFilterTest {
    @Mock private lateinit var authGateway: AuthGateway

    @Mock private lateinit var request: HttpServletRequest

    @Mock private lateinit var response: HttpServletResponse

    @Mock private lateinit var chain: FilterChain

    @InjectMocks private lateinit var filter: FirebaseTokenFilter

    @BeforeEach
    fun setUp() {
        // OncePerRequestFilter.doFilter checks these before delegating to doFilterInternal
        whenever(request.getAttribute(any())).thenReturn(null)
        whenever(request.dispatcherType).thenReturn(DispatcherType.REQUEST)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `passes through when no Authorization header is present`() {
        whenever(request.getHeader("Authorization")).thenReturn(null)

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
        verifyNoInteractions(authGateway)
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `sets security context and continues chain for valid token`() {
        val uid = "firebase-uid-123"
        whenever(request.getHeader("Authorization")).thenReturn("Bearer valid-token")
        whenever(authGateway.verifyToken("valid-token")).thenReturn(uid)

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertThat(auth).isNotNull()
        assertThat(auth?.principal).isEqualTo(uid)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `returns 401 and stops chain for invalid token`() {
        val exception = mock<AuthGatewayException.TokenVerificationFailed>()
        whenever(request.getHeader("Authorization")).thenReturn("Bearer invalid-token")
        whenever(authGateway.verifyToken("invalid-token")).thenThrow(exception)

        filter.doFilter(request, response, chain)

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token")
        verifyNoInteractions(chain)
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }
}
