package ru.sadovskie.leo.app.joposcragent.schedulersvc.cron

import com.cronutils.model.Cron
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import com.cronutils.model.CronType
import org.springframework.stereotype.Component
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.BadRequestException
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class CronNextRunCalculator {
	private val definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
	private val parser = CronParser(definition)

	fun validate(expression: String) {
		try {
			parse(expression)
		} catch (e: Exception) {
			throw BadRequestException("Некорректное cron-выражение: ${e.message}")
		}
	}

	fun nextAfter(reference: OffsetDateTime, expression: String): OffsetDateTime {
		val cron = parse(expression)
		val executionTime = ExecutionTime.forCron(cron)
		val zoned = reference.atZoneSameInstant(ZoneOffset.UTC)
		return executionTime.nextExecution(zoned)
			.orElseThrow { IllegalStateException("Не удалось вычислить следующий запуск по cron") }
			.withZoneSameInstant(ZoneOffset.UTC)
			.toOffsetDateTime()
	}

	private fun parse(expression: String): Cron = parser.parse(expression).validate()
}
