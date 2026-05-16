package ru.sadovskie.leo.app.joposcragent.schedulersvc.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCache
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCacheInitializer
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cron.CronNextRunCalculator
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.JobTypeHelper
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.SchedulerSettings
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.SchedulerSettingsItem
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.SchedulerSettingsList
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.UpdateCronExpression
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.UpdateNextRun
import ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence.SchedulerRepository
import java.time.Clock
import java.time.OffsetDateTime

@Service
class SchedulerSettingsService(
	private val repository: SchedulerRepository,
	private val cronCalculator: CronNextRunCalculator,
	private val cacheInitializer: SchedulerCacheInitializer,
	private val cache: SchedulerCache,
	private val clock: Clock,
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private val defaultCron = "0 * * * *"

	fun getSettingsList(): SchedulerSettingsList {
		val rows = repository.fetchSettingsListRows()
		val items = rows.map { db ->
			SchedulerSettingsItem(
				jobType = JobTypeHelper.toOpenApi(db.jobType),
				nextRun = db.nextRun,
				cronExpression = db.cronExpression,
				previousRun = cache.get(db.jobType)?.previousRun,
			)
		}
		return SchedulerSettingsList(list = items)
	}

	fun getSettings(jobTypeQuery: String?): SchedulerSettings {
		val code = JobTypeHelper.resolveJobTypeCode(jobTypeQuery)
		val row = repository.findByJobType(code)
		val previousRun = cache.get(code)?.previousRun
		return if (row == null) {
			SchedulerSettings(
				jobType = JobTypeHelper.toOpenApi(code),
				nextRun = null,
				cronExpression = null,
				previousRun = previousRun,
			)
		} else {
			SchedulerSettings(
				jobType = JobTypeHelper.toOpenApi(row.jobType),
				nextRun = row.nextRun,
				cronExpression = row.cronExpression,
				previousRun = previousRun,
			)
		}
	}

	@Transactional
	fun updateCronExpression(jobTypeQuery: String?, body: UpdateCronExpression) {
		cronCalculator.validate(body.value)
		val code = JobTypeHelper.resolveJobTypeCode(jobTypeQuery)
		val now = OffsetDateTime.now(clock)
		val existing = repository.findByJobType(code)
		if (existing == null) {
			val nextRun = cronCalculator.nextAfter(now, body.value)
			repository.insert(code, body.value, nextRun)
			log.info("updateCronExpression: inserted new row jobType={} nextRun={}", code, nextRun)
		} else {
			repository.updateCronExpression(code, body.value)
			log.info("updateCronExpression: updated cron jobType={}", code)
		}
		cacheInitializer.refreshJobType(code)
		log.info("updateCronExpression: cache refreshed jobType={}", code)
	}

	@Transactional
	fun updateNextRun(jobTypeQuery: String?, body: UpdateNextRun) {
		val code = JobTypeHelper.resolveJobTypeCode(jobTypeQuery)
		val existing = repository.findByJobType(code)
		if (existing == null) {
			repository.insert(code, defaultCron, body.value)
			log.info("updateNextRun: inserted new row jobType={} nextRun={}", code, body.value)
		} else {
			repository.updateNextRun(code, body.value)
			log.info("updateNextRun: updated next_run jobType={} nextRun={}", code, body.value)
		}
		cacheInitializer.refreshJobType(code)
		log.info("updateNextRun: cache refreshed jobType={}", code)
	}
}
