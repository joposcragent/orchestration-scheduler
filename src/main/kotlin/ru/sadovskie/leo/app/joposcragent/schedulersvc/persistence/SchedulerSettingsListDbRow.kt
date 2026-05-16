package ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence

import java.time.OffsetDateTime

/**
 * Строка результата запроса полного списка типов задач с LEFT JOIN к [orchestration.scheduler].
 */
data class SchedulerSettingsListDbRow(
	val jobType: String,
	val nextRun: OffsetDateTime?,
	val cronExpression: String?,
)
