package ru.sadovskie.leo.app.joposcragent.schedulersvc.persistence

import java.time.OffsetDateTime

/**
 * Строка для построения списка настроек: `job_type` из перечисления и опциональные поля из `LEFT JOIN scheduler`.
 */
data class SchedulerSettingsListDbRow(
	val jobType: String,
	val nextRun: OffsetDateTime?,
	val interval: String?,
)
