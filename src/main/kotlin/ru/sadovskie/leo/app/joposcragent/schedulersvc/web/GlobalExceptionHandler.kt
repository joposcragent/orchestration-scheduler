package ru.sadovskie.leo.app.joposcragent.schedulersvc.web

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.BadRequestException

@RestControllerAdvice
class GlobalExceptionHandler {
	@ExceptionHandler(BadRequestException::class)
	fun badRequest(e: BadRequestException): ResponseEntity<String> =
		ResponseEntity.status(400)
			.contentType(MediaType.TEXT_PLAIN)
			.body(e.message ?: "Некорректный запрос")

	@ExceptionHandler(Exception::class)
	fun uncaught(e: Exception): ResponseEntity<String> =
		ResponseEntity.status(500)
			.contentType(MediaType.TEXT_PLAIN)
			.body(e.toString())
}
