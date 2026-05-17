package ru.sadovskie.leo.app.joposcragent.schedulersvc.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration

class IsoDurationParserTest {
	@Test
	fun `parses positive duration`() {
		assertEquals(Duration.ofHours(6), IsoDurationParser.parse("PT6H"))
	}

	@Test
	fun `rejects zero duration`() {
		assertThrows(BadRequestException::class.java) {
			IsoDurationParser.parse("PT0S")
		}
	}

	@Test
	fun `rejects invalid string`() {
		assertThrows(BadRequestException::class.java) {
			IsoDurationParser.parse("not-a-duration")
		}
	}
}
