package ru.sadovskie.leo.app.joposcragent.schedulersvc.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCache
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cron.CronNextRunCalculator
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.JobTypeHelper
import ru.sadovskie.leo.app.joposcragent.schedulersvc.kafka.OrchestrationEnvelopePublisher
import ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence.SchedulerRepository
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SchedulerTickServiceTest {
	private val cache = SchedulerCache()
	private val repository = mockk<SchedulerRepository>(relaxed = true)
	private val cronCalculator = mockk<CronNextRunCalculator>()
	private val publisher = mockk<OrchestrationEnvelopePublisher>(relaxed = true)
	private val clock = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC)

	private val service = SchedulerTickService(
		cache = cache,
		repository = repository,
		cronCalculator = cronCalculator,
		envelopePublisher = publisher,
		clock = clock,
	)

	@Test
	fun `tick skips when cache empty`() {
		service.tick()
		verify(exactly = 0) { repository.updateNextRun(any(), any()) }
		verify(exactly = 0) { publisher.publishCollectionBatchBegin(any()) }
	}

	@Test
	fun `tick skips when now before nextRun`() {
		cache.put(
			SchedulerCache.Row(
				jobType = JobTypeHelper.COLLECTION_BATCH,
				nextRun = OffsetDateTime.parse("2026-01-15T11:00:00Z"),
				cronExpression = "0 * * * *",
			),
		)
		service.tick()
		verify(exactly = 0) { repository.updateNextRun(any(), any()) }
		verify(exactly = 0) { publisher.publishCollectionBatchBegin(any()) }
		assertNull(cache.get(JobTypeHelper.COLLECTION_BATCH)?.previousRun)
	}

	@Test
	fun `retention advances next run without kafka`() {
		cache.put(
			SchedulerCache.Row(
				jobType = JobTypeHelper.RETENTION,
				nextRun = OffsetDateTime.parse("2026-01-15T09:00:00Z"),
				cronExpression = "0 * * * *",
			),
		)
		val advanced = OffsetDateTime.parse("2026-01-15T11:00:00Z")
		every { cronCalculator.nextAfter(any(), "0 * * * *") } returns advanced
		every { repository.updateNextRun(JobTypeHelper.RETENTION, advanced) } returns 1

		service.tick()

		verify(exactly = 0) { publisher.publishCollectionBatchBegin(any()) }
		verify { repository.updateNextRun(JobTypeHelper.RETENTION, advanced) }
		assertEquals(OffsetDateTime.parse("2026-01-15T10:00:00Z"), cache.get(JobTypeHelper.RETENTION)?.previousRun)
	}

	@Test
	fun `collection batch publishes and updates`() {
		cache.put(
			SchedulerCache.Row(
				jobType = JobTypeHelper.COLLECTION_BATCH,
				nextRun = OffsetDateTime.parse("2026-01-15T09:00:00Z"),
				cronExpression = "0 * * * *",
			),
		)
		val advanced = OffsetDateTime.parse("2026-01-15T11:00:00Z")
		every { cronCalculator.nextAfter(any(), "0 * * * *") } returns advanced
		every { repository.updateNextRun(JobTypeHelper.COLLECTION_BATCH, advanced) } returns 1

		service.tick()

		verify { publisher.publishCollectionBatchBegin(any()) }
		verify { repository.updateNextRun(JobTypeHelper.COLLECTION_BATCH, advanced) }
		assertEquals(OffsetDateTime.parse("2026-01-15T10:00:00Z"), cache.get(JobTypeHelper.COLLECTION_BATCH)?.previousRun)
	}
}
