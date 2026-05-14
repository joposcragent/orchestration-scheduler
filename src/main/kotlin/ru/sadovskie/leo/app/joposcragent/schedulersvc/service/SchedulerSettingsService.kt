package ru.sadovskie.leo.app.joposcragent.schedulersvc.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCacheInitializer
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cron.CronNextRunCalculator
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.JobTypeHelper
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.SchedulerSettings
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
	private val clock: Clock,
) {
	private val defaultCron = "0 * * * *"

	fun getSettings(jobTypeQuery: String?): SchedulerSettings {
		val code = JobTypeHelper.resolveJobTypeCode(jobTypeQuery)
		val row = repository.findByJobType(code)
		return if (row == null) {
			SchedulerSettings(
				jobType = JobTypeHelper.toOpenApi(code),
				nextRun = null,
				cronExpression = null,
			)
		} else {
			SchedulerSettings(
				jobType = JobTypeHelper.toOpenApi(row.jobType),
				nextRun = row.nextRun,
				cronExpression = row.cronExpression,
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
		} else {
			repository.updateCronExpression(code, body.value)
		}
		cacheInitializer.refreshJobType(code)
	}

	@Transactional
	fun updateNextRun(jobTypeQuery: String?, body: UpdateNextRun) {
		val code = JobTypeHelper.resolveJobTypeCode(jobTypeQuery)
		val existing = repository.findByJobType(code)
		if (existing == null) {
			repository.insert(code, defaultCron, body.value)
		} else {
			repository.updateNextRun(code, body.value)
		}
		cacheInitializer.refreshJobType(code)
	}
}
