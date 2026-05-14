package ru.sadovskie.leo.app.joposcragent.schedulersvc.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
class SchedulingConfiguration : SchedulingConfigurer {
	override fun configureTasks(registrar: ScheduledTaskRegistrar) {
		val scheduler = ThreadPoolTaskScheduler().apply {
			poolSize = 4
			setThreadNamePrefix("scheduler-tick-")
			initialize()
		}
		registrar.setScheduler(scheduler)
	}
}
