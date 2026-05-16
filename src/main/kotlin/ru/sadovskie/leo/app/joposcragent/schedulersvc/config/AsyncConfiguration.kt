package ru.sadovskie.leo.app.joposcragent.schedulersvc.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfiguration {
	@Bean(name = ["schedulerExecute"])
	fun schedulerExecuteExecutor(): Executor {
		val executor = ThreadPoolTaskExecutor()
		executor.corePoolSize = 2
		executor.maxPoolSize = 4
		executor.setThreadNamePrefix("scheduler-exec-")
		executor.initialize()
		return executor
	}
}
