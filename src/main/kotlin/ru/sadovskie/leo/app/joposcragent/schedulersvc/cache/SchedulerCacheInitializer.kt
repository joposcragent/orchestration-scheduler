package ru.sadovskie.leo.app.joposcragent.schedulersvc.cache

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence.SchedulerRepository

@Component
@Order(0)
class SchedulerCacheInitializer(
	private val repository: SchedulerRepository,
	private val cache: SchedulerCache,
) : ApplicationRunner {
	override fun run(args: ApplicationArguments) {
		reloadFromDatabase()
	}

	fun reloadFromDatabase() {
		val rows = repository.findAll().map { r ->
			SchedulerCache.Row(
				jobType = r.jobType,
				nextRun = r.nextRun,
				cronExpression = r.cronExpression,
			)
		}
		cache.replaceAll(rows)
	}

	fun refreshJobType(jobType: String) {
		val r = repository.findByJobType(jobType)
		if (r == null) {
			cache.remove(jobType)
		} else {
			val previousRun = cache.get(jobType)?.previousRun
			cache.put(
				SchedulerCache.Row(
					jobType = r.jobType,
					nextRun = r.nextRun,
					cronExpression = r.cronExpression,
					previousRun = previousRun,
				),
			)
		}
	}
}
