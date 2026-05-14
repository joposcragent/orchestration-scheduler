package ru.sadovskie.leo.app.joposcragent.schedulersvc.web

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.SchedulerSettings
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.UpdateCronExpression
import ru.sadovskie.leo.app.joposcragent.schedulersvc.openapi.model.UpdateNextRun
import ru.sadovskie.leo.app.joposcragent.schedulersvc.service.SchedulerSettingsService

@RestController
class SettingsController(
	private val settingsService: SchedulerSettingsService,
) {
	@GetMapping("/settings", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun getSettings(@RequestParam(required = false) jobType: String?): SchedulerSettings =
		settingsService.getSettings(jobType)

	@PutMapping("/settings/cron-expression")
	fun putCronExpression(
		@RequestParam(required = false) jobType: String?,
		@RequestBody body: UpdateCronExpression,
	): ResponseEntity<Void> {
		settingsService.updateCronExpression(jobType, body)
		return ResponseEntity.ok().build()
	}

	@PutMapping("/settings/next-run")
	fun putNextRun(
		@RequestParam(required = false) jobType: String?,
		@RequestBody body: UpdateNextRun,
	): ResponseEntity<Void> {
		settingsService.updateNextRun(jobType, body)
		return ResponseEntity.ok().build()
	}
}
