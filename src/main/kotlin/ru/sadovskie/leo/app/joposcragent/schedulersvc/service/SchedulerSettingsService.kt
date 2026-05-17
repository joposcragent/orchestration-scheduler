package ru.sadovskie.leo.app.joposcragent.schedulersvc.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCache
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCacheInitializer
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.IsoDurationParser
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.JobTypeHelper
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.SchedulerSettingsItem
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.SchedulerSettingsList
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.UpdateInterval
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.UpdateNextRun
import ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence.SchedulerRepository
import java.time.Clock
import java.time.OffsetDateTime

@Service
class SchedulerSettingsService(
	private val repository: SchedulerRepository,
	private val cacheInitializer: SchedulerCacheInitializer,
	private val cache: SchedulerCache,
	private val clock: Clock,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	/** Совпадает с default в миграции `scheduler.interval`. */
	private val defaultInterval = "PT1H"

	fun getSettingsList(): SchedulerSettingsList {
		val rows = repository.fetchSettingsListRows()
		val items = rows.map { db ->
			SchedulerSettingsItem(
				jobType = JobTypeHelper.toOpenApi(db.jobType),
				nextRun = db.nextRun,
				interval = db.interval,
				previousRun = cache.get(db.jobType)?.previousRun,
			)
		}
		return SchedulerSettingsList(list = items)
	}

	fun getSettings(jobTypeQuery: String?): SchedulerSettingsItem {
		val code = JobTypeHelper.resolveJobTypeCode(jobTypeQuery)
		val row = repository.findByJobType(code)
		val previousRun = cache.get(code)?.previousRun
		return if (row == null) {
			SchedulerSettingsItem(
				jobType = JobTypeHelper.toOpenApi(code),
				nextRun = null,
				interval = null,
				previousRun = previousRun,
			)
		} else {
			SchedulerSettingsItem(
				jobType = JobTypeHelper.toOpenApi(row.jobType),
				nextRun = row.nextRun,
				interval = row.interval,
				previousRun = previousRun,
			)
		}
	}

	@Transactional
	fun updateInterval(jobTypeQuery: String?, body: UpdateInterval) {
		val duration = IsoDurationParser.parse(body.value)
		val code = JobTypeHelper.resolveJobTypeCode(jobTypeQuery)
		val now = OffsetDateTime.now(clock)
		val existing = repository.findByJobType(code)
		if (existing == null) {
			val nextRun = now.plus(duration)
			repository.insert(code, body.value, nextRun)
			log.info("updateInterval: inserted new row jobType={} nextRun={}", code, nextRun)
		} else {
			repository.updateInterval(code, body.value)
			log.info("updateInterval: updated interval jobType={}", code)
		}
		cacheInitializer.refreshJobType(code)
		log.info("updateInterval: cache refreshed jobType={}", code)
	}

	@Transactional
	fun updateNextRun(jobTypeQuery: String?, body: UpdateNextRun) {
		val code = JobTypeHelper.resolveJobTypeCode(jobTypeQuery)
		val existing = repository.findByJobType(code)
		if (existing == null) {
			repository.insert(code, defaultInterval, body.value)
			log.info("updateNextRun: inserted new row jobType={} nextRun={}", code, body.value)
		} else {
			repository.updateNextRun(code, body.value)
			log.info("updateNextRun: updated next_run jobType={} nextRun={}", code, body.value)
		}
		cacheInitializer.refreshJobType(code)
		log.info("updateNextRun: cache refreshed jobType={}", code)
	}
}
