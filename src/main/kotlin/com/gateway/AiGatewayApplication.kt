package com.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("com.gateway.config.properties")
class AiGatewayApplication

fun main(args: Array<String>) {
    runApplication<AiGatewayApplication>(*args)
}
