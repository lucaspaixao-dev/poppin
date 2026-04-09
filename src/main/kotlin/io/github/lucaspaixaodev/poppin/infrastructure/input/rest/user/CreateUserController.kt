package io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user

import io.github.lucaspaixaodev.poppin.application.user.CreateUserUseCase
import io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.api.CreateUserApi
import io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.request.CreateUserRequest
import io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.response.CreateUserResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CreateUserController(
    private val createUserUseCase: CreateUserUseCase,
) : CreateUserApi {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(request: CreateUserRequest): ResponseEntity<CreateUserResponse> {
        log.info("POST /users - email={}", request.email)
        val output = createUserUseCase.execute(request.toInput())
        log.info("User created - id={}", output.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateUserResponse.fromOutput(output))
    }
}
