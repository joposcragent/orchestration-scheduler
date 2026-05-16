package ru.sadovskie.leo.app.joposcragent.schedulersvc.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCache
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cron.CronNextRunCalculator
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.JobTypeHelper
import ru.sadovskie.leo.app.joposcragent.schedulersvc.kafka.OrchestrationEnvelopePublisher
import ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence.SchedulerRepository
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class SchedulerTickService(
	private val cache: SchedulerCache,
	private val repository: SchedulerRepository,
	private val cronCalculator: CronNextRunCalculator,
	private val envelopePublisher: OrchestrationEnvelopePublisher,
	private val clock: Clock,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Scheduled(fixedRateString = "\${scheduler.tick-interval-ms:600000}")
	fun tick() {
		runBlocking {
			val now = OffsetDateTime.now(clock)
			val snapshot = cache.snapshot()
			if (snapshot.isEmpty()) {
				if (log.isDebugEnabled) {
					log.debug("scheduler tick: empty cache snapshot, nothing to do")
				}
				return@runBlocking
			}
			log.info("scheduler tick: {} job type(s) in cache", snapshot.size)
			snapshot.keys.map { jobType ->
				async(Dispatchers.Default) {
					try {
						handleJobType(jobType, snapshot.getValue(jobType), now)
					} catch (e: Exception) {
						log.error("tick failed for jobType={}", jobType, e)
					}
				}
			}.awaitAll()
		}
	}

	private suspend fun handleJobType(jobType: String, row: SchedulerCache.Row, now: OffsetDateTime) {
		if (now.isBefore(row.nextRun)) {
			if (log.isDebugEnabled) {
				log.debug(
					"jobType={} not due yet: now={} nextRun={}",
					jobType,
					now,
					row.nextRun,
				)
			}
			return
		}
		runScheduledJobActions(jobType, row, now)
	}

	/**
	 * Выполняет ветки 4.1 и 4.2 из спецификации тика (Kafka при необходимости + сдвиг next_run),
	 * без проверки «наступил ли срок» — для принудительного [POST /execute].
	 */
	suspend fun runScheduledJobActions(jobType: String, row: SchedulerCache.Row, now: OffsetDateTime) {
		coroutineScope {
			launch(Dispatchers.IO) {
				if (jobType == JobTypeHelper.COLLECTION_BATCH) {
					val jobUuid = UUID.randomUUID()
					envelopePublisher.publishCollectionBatchBegin(jobUuid)
					log.info("published COLLECTION_BATCH_BEGIN jobUuid={} jobType={}", jobUuid, jobType)
				} else if (log.isDebugEnabled) {
					log.debug("jobType={} due but no Kafka message scheduled for this type", jobType)
				}
			}
			launch(Dispatchers.IO) {
				try {
					val newNext = cronCalculator.nextAfter(now, row.cronExpression)
					cache.put(
						SchedulerCache.Row(
							jobType = jobType,
							nextRun = newNext,
							cronExpression = row.cronExpression,
							previousRun = now,
						),
					)
					repository.updateNextRun(jobType, newNext)
					log.info("persisted next_run jobType={} newNextRun={}", jobType, newNext)
				} catch (e: Exception) {
					log.error("Failed to persist next_run for jobType={}", jobType, e)
				}
			}
		}
	}
}
