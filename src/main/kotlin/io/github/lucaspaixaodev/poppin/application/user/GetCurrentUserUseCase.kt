package io.github.lucaspaixaodev.poppin.application.user

import io.github.lucaspaixaodev.poppin.domain.user.output.GetCurrentUserOutput
import io.github.lucaspaixaodev.poppin.domain.user.service.GetCurrentUserService
import org.springframework.stereotype.Component

@Component
class GetCurrentUserUseCase(
    private val getCurrentUserService: GetCurrentUserService,
) {
    fun execute(id: String): GetCurrentUserOutput = GetCurrentUserOutput(getCurrentUserService.execute(id))
}
