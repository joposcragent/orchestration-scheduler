package ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence

import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.JobTypeHelper
import ru.sadovskie.leo.app.joposcragent.schedulersvc.jooq.Tables.SCHEDULER
import ru.sadovskie.leo.app.joposcragent.schedulersvc.jooq.tables.records.SchedulerRecord
import java.time.OffsetDateTime

@Repository
class SchedulerRepository(
	private val dsl: DSLContext,
) {
	fun fetchSettingsListRows(): List<SchedulerSettingsListDbRow> {
		val er: Table<Record1<String>> = jobTypesAsValuesTable()
		val jobTypeField = er.field("job_type", String::class.java)!!
		return dsl.select(jobTypeField, SCHEDULER.NEXT_RUN, SCHEDULER.INTERVAL)
			.from(er)
			.leftJoin(SCHEDULER).on(DSL.cast(SCHEDULER.JOB_TYPE, SQLDataType.VARCHAR).eq(jobTypeField))
			.fetch { r ->
				SchedulerSettingsListDbRow(
					jobType = r.get(jobTypeField)!!,
					nextRun = r.get(SCHEDULER.NEXT_RUN),
					interval = r.get(SCHEDULER.INTERVAL),
				)
			}
	}

	private fun jobTypesAsValuesTable(): Table<Record1<String>> {
		val rows = JobTypeHelper.allJobTypeCodesInDbOrder.map { code -> DSL.row(DSL.`val`(code)) }
		return DSL.values(*rows.toTypedArray()).`as`("er", "job_type")
	}

	fun findByJobType(jobType: String): SchedulerRecord? =
		dsl.selectFrom(SCHEDULER).where(SCHEDULER.JOB_TYPE.eq(jobType)).fetchOne()

	fun findAll(): List<SchedulerRecord> =
		dsl.selectFrom(SCHEDULER).fetch()

	fun updateInterval(jobType: String, interval: String): Int =
		dsl.update(SCHEDULER)
			.set(SCHEDULER.INTERVAL, interval)
			.where(SCHEDULER.JOB_TYPE.eq(jobType))
			.execute()

	fun updateNextRun(jobType: String, nextRun: OffsetDateTime): Int =
		dsl.update(SCHEDULER)
			.set(SCHEDULER.NEXT_RUN, nextRun)
			.where(SCHEDULER.JOB_TYPE.eq(jobType))
			.execute()

	fun insert(jobType: String, interval: String, nextRun: OffsetDateTime) {
		dsl.insertInto(SCHEDULER, SCHEDULER.JOB_TYPE, SCHEDULER.INTERVAL, SCHEDULER.NEXT_RUN)
			.values(jobType, interval, nextRun)
			.execute()
	}
}
