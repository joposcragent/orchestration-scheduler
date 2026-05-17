package ru.sadovskie.leo.app.joposcragent.schedulersvc.domain

import java.time.Duration
import java.time.format.DateTimeParseException

object IsoDurationParser {
	/**
	 * Парсит ISO-8601 duration (`Duration.parse`) и отклоняет нулевые и отрицательные значения.
	 */
	fun parse(text: String): Duration =
		try {
			val d = Duration.parse(text.trim())
			if (d.isNegative || d.isZero) {
				throw BadRequestException("Интервал должен быть положительной ненулевой длительностью (ISO-8601 duration)")
			}
			d
		} catch (e: DateTimeParseException) {
			throw BadRequestException("Некорректная длительность интервала (ISO-8601): ${e.message}")
		}
}
