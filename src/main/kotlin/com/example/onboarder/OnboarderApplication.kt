package com.example.onboarder

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OnboarderApplication

fun main(args: Array<String>) {
    runApplication<OnboarderApplication>(*args)
}
