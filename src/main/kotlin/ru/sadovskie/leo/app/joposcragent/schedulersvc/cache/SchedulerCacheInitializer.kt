package ru.sadovskie.leo.app.joposcragent.schedulersvc.cache

import org.slf4j.LoggerFactory
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
	private val log = LoggerFactory.getLogger(javaClass)

	override fun run(args: ApplicationArguments) {
		reloadFromDatabase()
	}

	fun reloadFromDatabase() {
		val rows = repository.findAll().map { r ->
			SchedulerCache.Row(
				jobType = r.jobType,
				nextRun = r.nextRun,
				interval = r.interval,
			)
		}
		cache.replaceAll(rows)
		log.info("scheduler cache loaded from database: {} row(s)", rows.size)
		if (log.isDebugEnabled) {
			val keys = rows.joinToString { it.jobType }
			log.debug("scheduler cache jobTypes: [{}]", keys)
		}
	}

	fun refreshJobType(jobType: String) {
		val r = repository.findByJobType(jobType)
		if (r == null) {
			cache.remove(jobType)
			log.info("scheduler cache refresh: removed jobType={} (no DB row)", jobType)
		} else {
			val previousRun = cache.get(jobType)?.previousRun
			cache.put(
				SchedulerCache.Row(
					jobType = r.jobType,
					nextRun = r.nextRun,
					interval = r.interval,
					previousRun = previousRun,
				),
			)
			log.info(
				"scheduler cache refresh: updated jobType={} nextRun={}",
				jobType,
				r.nextRun,
			)
		}
	}
}
