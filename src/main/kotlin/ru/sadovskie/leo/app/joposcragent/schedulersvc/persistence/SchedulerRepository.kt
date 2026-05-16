package ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence

import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import ru.sadovskie.leo.app.joposcragent.schedulersvc.jooq.Tables.SCHEDULER
import ru.sadovskie.leo.app.joposcragent.schedulersvc.jooq.tables.records.SchedulerRecord
import java.time.OffsetDateTime

private val SETTINGS_LIST_SQL = """
	SELECT er.job_type::text, s.next_run, s.cron_expression
	FROM unnest(enum_range(NULL::orchestration.scheduler_jobs)) AS er(job_type)
	LEFT JOIN orchestration.scheduler s ON er.job_type::text = s.job_type::text
""".trimIndent()

private val SETTINGS_LIST_MAPPER = RecordMapper<Record, SchedulerSettingsListDbRow> { record ->
	SchedulerSettingsListDbRow(
		jobType = record.get(0, String::class.java)!!,
		nextRun = record.get(1, OffsetDateTime::class.java),
		cronExpression = record.get(2, String::class.java),
	)
}

@Repository
class SchedulerRepository(
	private val dsl: DSLContext,
) {
	fun fetchSettingsListRows(): List<SchedulerSettingsListDbRow> =
		dsl.resultQuery(SETTINGS_LIST_SQL).fetch(SETTINGS_LIST_MAPPER)

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
