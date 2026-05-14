package ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import ru.sadovskie.leo.app.joposcragent.schedulersvc.jooq.tables.records.SchedulerRecord
import ru.sadovskie.leo.app.joposcragent.schedulersvc.jooq.Tables.SCHEDULER
import java.time.OffsetDateTime

@Repository
class SchedulerRepository(
	private val dsl: DSLContext,
) {
	fun findByJobType(jobType: String): SchedulerRecord? =
		dsl.selectFrom(SCHEDULER).where(SCHEDULER.JOB_TYPE.eq(jobType)).fetchOne()

	fun findAll(): List<SchedulerRecord> =
		dsl.selectFrom(SCHEDULER).fetch()

	fun updateCronExpression(jobType: String, cronExpression: String): Int =
		dsl.update(SCHEDULER)
			.set(SCHEDULER.CRON_EXPRESSION, cronExpression)
			.where(SCHEDULER.JOB_TYPE.eq(jobType))
			.execute()

	fun updateNextRun(jobType: String, nextRun: OffsetDateTime): Int =
		dsl.update(SCHEDULER)
			.set(SCHEDULER.NEXT_RUN, nextRun)
			.where(SCHEDULER.JOB_TYPE.eq(jobType))
			.execute()

	fun insert(jobType: String, cronExpression: String, nextRun: OffsetDateTime) {
		dsl.insertInto(SCHEDULER, SCHEDULER.JOB_TYPE, SCHEDULER.CRON_EXPRESSION, SCHEDULER.NEXT_RUN)
			.values(jobType, cronExpression, nextRun)
			.execute()
	}
}
