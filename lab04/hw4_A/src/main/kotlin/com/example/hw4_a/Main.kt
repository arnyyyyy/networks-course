package com.example.hw4_a

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProxyServerA

fun main(args: Array<String>) {
    runApplication<ProxyServerA>(*args)
}