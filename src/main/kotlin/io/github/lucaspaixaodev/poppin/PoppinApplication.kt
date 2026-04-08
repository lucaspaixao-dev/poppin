package io.github.lucaspaixaodev.poppin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PoppinApplication

fun main(args: Array<String>) {
    runApplication<PoppinApplication>(*args)
}
