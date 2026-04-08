package io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.api

import io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.request.CreateUserRequest
import io.github.lucaspaixaodev.poppin.infrastructure.input.rest.user.response.CreateUserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Users", description = "User management")
@RequestMapping("/api/v1/users")
interface CreateUserApi {

    @Operation(summary = "Create a new user")
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = [Content(schema = Schema(implementation = CreateUserResponse::class))]
        ),
        ApiResponse(responseCode = "400", description = "Validation error", content = [Content()]),
        ApiResponse(responseCode = "409", description = "Email already registered", content = [Content()])
    )
    @PostMapping
    fun create(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<CreateUserResponse>
}