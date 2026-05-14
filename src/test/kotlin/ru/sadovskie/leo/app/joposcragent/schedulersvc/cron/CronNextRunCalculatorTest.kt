package ru.sadovskie.leo.app.joposcragent.schedulersvc.cron

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.BadRequestException
import java.time.OffsetDateTime

class CronNextRunCalculatorTest {
	private val calculator = CronNextRunCalculator()

	@Test
	fun validateRejectsInvalid() {
		assertThrows(BadRequestException::class.java) {
			calculator.validate("not-a-cron")
		}
	}

	@Test
	fun nextAfterIsStrictlyAfterReference() {
		val ref = OffsetDateTime.parse("2026-05-14T12:00:00Z")
		val next = calculator.nextAfter(ref, "0 * * * *")
		assertTrue(next.isAfter(ref))
	}
}
