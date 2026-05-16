package ru.sadovskie.leo.app.joposcragent.schedulersvc.service

import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCache
import java.time.Clock
import java.time.OffsetDateTime

@Component
class SchedulerAsyncExecutor(
	private val tickService: SchedulerTickService,
	private val cache: SchedulerCache,
	private val clock: Clock,
) {
	@Async("schedulerExecute")
	fun executeJob(jobTypeCode: String) {
		val row = cache.get(jobTypeCode) ?: return
		runBlocking {
			tickService.runScheduledJobActions(jobTypeCode, row, OffsetDateTime.now(clock))
		}
	}
}
