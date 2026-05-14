package ru.sadovskie.leo.app.joposcragent.schedulersvc.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Configuration
class JacksonConfig {
	@Bean
	fun jsonMapper(): JsonMapper = JsonMapper.builder()
		.addModule(kotlinModule())
		.build()
}
