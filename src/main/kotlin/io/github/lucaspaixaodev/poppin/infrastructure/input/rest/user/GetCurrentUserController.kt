package io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user

import io.github.lucaspaixaodev.poppin.application.user.GetCurrentUserUseCase
import io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.api.GetCurrentUserApi
import io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.response.GetCurrentUserResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class GetCurrentUserController(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) : GetCurrentUserApi {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getCurrentUser(): ResponseEntity<GetCurrentUserResponse> {
        val userId = SecurityContextHolder.getContext().authentication!!.principal as String
        log.info("GET /users - userId={}", userId)
        val output = getCurrentUserUseCase.execute(userId)
        return ResponseEntity.ok(GetCurrentUserResponse.fromOutput(output))
    }
}
