package ru.sadovskie.leo.app.joposcragent.schedulersvc.domain

import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.SchedulerJobType

object JobTypeHelper {
	const val COLLECTION_BATCH = "COLLECTION_BATCH"
	const val RETENTION = "RETENTION"

	/**
	 * Все известные коды `job_type` в порядке, совпадающем с `orchestration.scheduler_jobs` в БД.
	 * Используется для типобезопасного построения источника строк в jOOQ без сырого SQL.
	 */
	val allJobTypeCodesInDbOrder: List<String> = listOf(COLLECTION_BATCH, RETENTION)

	private val allowed = setOf(COLLECTION_BATCH, RETENTION)

	fun resolveJobTypeCode(jobTypeQuery: String?): String {
		if (jobTypeQuery.isNullOrBlank()) {
			return COLLECTION_BATCH
		}
		if (!allowed.contains(jobTypeQuery)) {
			throw BadRequestException("Недопустимое значение jobType: $jobTypeQuery")
		}
		return jobTypeQuery
	}

	fun toOpenApi(code: String): SchedulerJobType = when (code) {
		COLLECTION_BATCH -> SchedulerJobType.COLLECTION_BATCH
		RETENTION -> SchedulerJobType.RETENTION
		else -> throw IllegalStateException("unknown job type: $code")
	}
}
