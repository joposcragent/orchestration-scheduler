package ru.sadovskie.leo.app.joposcragent.schedulersvc.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class JobTypeHelperTest {
	@Test
	fun defaultJobType() {
		assertEquals(JobTypeHelper.COLLECTION_BATCH, JobTypeHelper.resolveJobTypeCode(null))
		assertEquals(JobTypeHelper.COLLECTION_BATCH, JobTypeHelper.resolveJobTypeCode(""))
	}

	@Test
	fun retention() {
		assertEquals(JobTypeHelper.RETENTION, JobTypeHelper.resolveJobTypeCode("RETENTION"))
	}

	@Test
	fun invalid() {
		assertThrows(BadRequestException::class.java) {
			JobTypeHelper.resolveJobTypeCode("OTHER")
		}
	}
}
