package ru.sadovskie.leo.app.joposcragent.schedulersvc.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCache
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCacheInitializer
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cron.CronNextRunCalculator
import ru.sadovskie.leo.app.joposcragent.schedulersvc.jooq.tables.records.SchedulerRecord
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.UpdateCronExpression
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.UpdateNextRun
import ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence.SchedulerRepository
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SchedulerSettingsServiceTest {
	private val repository = mockk<SchedulerRepository>(relaxed = true)
	private val cron = mockk<CronNextRunCalculator>(relaxed = true)
	private val cacheInitializer = mockk<SchedulerCacheInitializer>(relaxed = true)
	private val cache = SchedulerCache()
	private val clock = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC)

	private val service = SchedulerSettingsService(repository, cron, cacheInitializer, cache, clock)

	@Test
	fun `update cron creates row when missing`() {
		every { repository.findByJobType("COLLECTION_BATCH") } returns null
		val next = OffsetDateTime.parse("2026-01-15T11:00:00Z")
		every { cron.nextAfter(any(), "0 0 * * *") } returns next

		service.updateCronExpression(null, UpdateCronExpression("0 0 * * *"))

		verify {
			repository.insert("COLLECTION_BATCH", "0 0 * * *", next)
		}
		verify { cacheInitializer.refreshJobType("COLLECTION_BATCH") }
	}

	@Test
	fun `update next run updates existing`() {
		val row = SchedulerRecord().apply {
			jobType = "COLLECTION_BATCH"
			cronExpression = "0 * * * *"
			nextRun = OffsetDateTime.parse("2026-01-15T09:00:00Z")
		}
		every { repository.findByJobType("COLLECTION_BATCH") } returns row
		val newNext = OffsetDateTime.parse("2026-01-20T08:00:00Z")

		service.updateNextRun(null, UpdateNextRun(newNext))

		verify { repository.updateNextRun("COLLECTION_BATCH", newNext) }
		verify { cacheInitializer.refreshJobType("COLLECTION_BATCH") }
	}

	@Test
	fun `get settings maps previousRun from cache`() {
		val next = OffsetDateTime.parse("2026-01-15T09:00:00Z")
		val row = SchedulerRecord().apply {
			jobType = "COLLECTION_BATCH"
			cronExpression = "0 * * * *"
			nextRun = next
		}
		every { repository.findByJobType("COLLECTION_BATCH") } returns row
		val previousRun = OffsetDateTime.parse("2026-01-14T08:00:00Z")
		cache.put(
			SchedulerCache.Row(
				jobType = "COLLECTION_BATCH",
				nextRun = next,
				cronExpression = "0 * * * *",
				previousRun = previousRun,
			),
		)

		val settings = service.getSettings(null)

		assertEquals(previousRun, settings.previousRun)
		assertEquals(next, settings.nextRun)
	}

	@Test
	fun `get settings returns null previousRun when cache miss`() {
		val row = SchedulerRecord().apply {
			jobType = "COLLECTION_BATCH"
			cronExpression = "0 * * * *"
			nextRun = OffsetDateTime.parse("2026-01-15T09:00:00Z")
		}
		every { repository.findByJobType("COLLECTION_BATCH") } returns row

		val settings = service.getSettings(null)

		assertNull(settings.previousRun)
	}
}
