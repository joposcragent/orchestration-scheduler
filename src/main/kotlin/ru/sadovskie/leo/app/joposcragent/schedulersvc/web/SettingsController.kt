package ru.sadovskie.leo.app.joposcragent.schedulersvc.web

import org.slf4j.LoggerFactory
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
import tools.jackson.databind.json.JsonMapper

@RestController
class SettingsController(
	private val settingsService: SchedulerSettingsService,
	private val jsonMapper: JsonMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@GetMapping("/settings", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun getSettings(@RequestParam(required = false) jobType: String?): SchedulerSettings {
		if (log.isDebugEnabled) {
			log.debug("GET /settings jobTypeQuery={}", jobType)
		}
		val settings = settingsService.getSettings(jobType)
		if (log.isDebugEnabled) {
			log.debug("GET /settings response body={}", jsonMapper.writeValueAsString(settings))
		}
		return settings
	}

	@PutMapping("/settings/cron-expression")
	fun putCronExpression(
		@RequestParam(required = false) jobType: String?,
		@RequestBody body: UpdateCronExpression,
	): ResponseEntity<Void> {
		if (log.isDebugEnabled) {
			log.debug(
				"PUT /settings/cron-expression jobTypeQuery={} body={}",
				jobType,
				jsonMapper.writeValueAsString(body),
			)
		}
		settingsService.updateCronExpression(jobType, body)
		if (log.isDebugEnabled) {
			log.debug("PUT /settings/cron-expression -> 200 OK jobTypeQuery={}", jobType)
		}
		return ResponseEntity.ok().build()
	}

	@PutMapping("/settings/next-run")
	fun putNextRun(
		@RequestParam(required = false) jobType: String?,
		@RequestBody body: UpdateNextRun,
	): ResponseEntity<Void> {
		if (log.isDebugEnabled) {
			log.debug(
				"PUT /settings/next-run jobTypeQuery={} body={}",
				jobType,
				jsonMapper.writeValueAsString(body),
			)
		}
		settingsService.updateNextRun(jobType, body)
		if (log.isDebugEnabled) {
			log.debug("PUT /settings/next-run -> 200 OK jobTypeQuery={}", jobType)
		}
		return ResponseEntity.ok().build()
	}
}
