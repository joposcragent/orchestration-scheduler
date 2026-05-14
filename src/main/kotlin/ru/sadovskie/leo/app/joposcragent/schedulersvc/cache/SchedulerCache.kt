package ru.sadovskie.leo.app.joposcragent.schedulersvc.cache

import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

@Component
class SchedulerCache {
	data class Row(
		val jobType: String,
		val nextRun: OffsetDateTime,
		val cronExpression: String,
		val previousRun: OffsetDateTime? = null,
	)

	private val rowsByJobType = ConcurrentHashMap<String, Row>()

	fun get(jobType: String): Row? = rowsByJobType[jobType]

	fun replaceAll(rows: Collection<Row>) {
		rowsByJobType.clear()
		rows.forEach { rowsByJobType[it.jobType] = it }
	}

	fun put(row: Row) {
		rowsByJobType[row.jobType] = row
	}

	fun remove(jobType: String) {
		rowsByJobType.remove(jobType)
	}

	fun snapshot(): Map<String, Row> = HashMap(rowsByJobType)
}
