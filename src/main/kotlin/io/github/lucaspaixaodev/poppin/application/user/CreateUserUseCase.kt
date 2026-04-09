package io.github.lucaspaixaodev.poppin.application.user

import io.github.lucaspaixaodev.poppin.domain.user.input.CreateUserInput
import io.github.lucaspaixaodev.poppin.domain.user.output.CreateUserOutput
import io.github.lucaspaixaodev.poppin.domain.user.service.CreateAuthUserService
import io.github.lucaspaixaodev.poppin.domain.user.service.CreateUserGraphService
import io.github.lucaspaixaodev.poppin.domain.user.service.CreateUserService
import org.springframework.stereotype.Component

@Component
class CreateUserUseCase(
    private val createUserService: CreateUserService,
    private val createAuthUserService: CreateAuthUserService,
    private val createUserGraphService: CreateUserGraphService,
) {
    fun execute(input: CreateUserInput): CreateUserOutput {
        val user = createUserService.execute(input)

        createAuthUserService.execute(user)

        createUserGraphService.execute(user)

        return CreateUserOutput(user)
    }
}
