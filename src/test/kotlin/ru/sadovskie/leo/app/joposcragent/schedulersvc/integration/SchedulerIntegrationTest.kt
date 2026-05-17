package ru.sadovskie.leo.app.joposcragent.schedulersvc.integration

import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCacheInitializer
import ru.sadovskie.leo.app.joposcragent.schedulersvc.kafka.OrchestrationKafkaTopics
import java.sql.DriverManager

@Import(IntegrationKafkaConfig::class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = [
		"scheduler.tick-interval-ms=99999999999",
		"spring.jackson.default-property-inclusion=always",
	],
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@EmbeddedKafka(
	partitions = 1,
	topics = [OrchestrationKafkaTopics.COLLECTION_BATCH],
)
class SchedulerIntegrationTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@Autowired
	private lateinit var cacheInitializer: SchedulerCacheInitializer

	@BeforeEach
	fun resetSchedulerTable() {
		jdbcTemplate.execute("TRUNCATE TABLE orchestration.scheduler")
		cacheInitializer.reloadFromDatabase()
	}

	companion object {
		@Container
		@JvmStatic
		val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
			.withDatabaseName("joposcragent")
			.withUsername("postgres")
			.withPassword("postgres")

		@JvmStatic
		@DynamicPropertySource
		fun datasourceProperties(registry: DynamicPropertyRegistry) {
			postgres.start()
			applyOrchestrationDdl()
			val baseUrl = postgres.jdbcUrl
			val url = if (baseUrl.contains("?")) {
				"$baseUrl&stringtype=unspecified"
			} else {
				"$baseUrl?stringtype=unspecified"
			}
			registry.add("spring.datasource.url") { url }
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
		}

		private fun applyOrchestrationDdl() {
			val ddlStatements = listOf(
				"DROP SCHEMA IF EXISTS orchestration CASCADE",
				"CREATE SCHEMA orchestration",
				"CREATE TYPE orchestration.scheduler_jobs AS ENUM ('COLLECTION_BATCH', 'RETENTION')",
				"""
				CREATE TABLE IF NOT EXISTS orchestration.scheduler
				(
					uuid            uuid    DEFAULT gen_random_uuid()              NOT NULL,
					job_type        orchestration.scheduler_jobs                   NOT NULL,
					next_run        timestamp with time zone                       NOT NULL,
					interval        varchar DEFAULT 'PT1H'::character varying      NOT NULL
				)
				""".trimIndent(),
				"ALTER TABLE orchestration.scheduler ADD CONSTRAINT scheduler_pk PRIMARY KEY (uuid)",
				"ALTER TABLE orchestration.scheduler ADD CONSTRAINT scheduler_idx_unique_job_type UNIQUE (job_type)",
			)
			DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
				connection.createStatement().use { statement ->
					for (sql in ddlStatements) {
						statement.execute(sql)
					}
				}
			}
		}
	}

	@Test
	fun `get settings when empty`() {
		mockMvc.perform(
			get("/settings").accept(MediaType.APPLICATION_JSON),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.jobType").value("COLLECTION_BATCH"))
			.andExpect(jsonPath("$.nextRun").value(nullValue()))
			.andExpect(jsonPath("$.interval").value(nullValue()))
			.andExpect(jsonPath("$.previousRun").value(nullValue()))
	}

	@Test
	fun `get settings list returns all enum job types`() {
		mockMvc.perform(
			get("/settings/list").accept(MediaType.APPLICATION_JSON),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.list.length()").value(2))
			.andExpect(jsonPath("$.list[*].jobType", containsInAnyOrder("COLLECTION_BATCH", "RETENTION")))
	}

	@Test
	fun `post execute without cache row returns 400`() {
		mockMvc.perform(
			post("/execute").param("jobType", "RETENTION"),
		)
			.andExpect(status().isBadRequest)
	}

	@Test
	fun `post execute after interval returns ok`() {
		mockMvc.perform(
			put("/settings/interval")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"value":"PT1H"}"""),
		).andExpect(status().isOk)

		mockMvc.perform(
			post("/execute").param("jobType", "COLLECTION_BATCH"),
		).andExpect(status().isOk)
	}

	@Test
	fun `put interval then get`() {
		mockMvc.perform(
			put("/settings/interval")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"value":"PT6H"}"""),
		).andExpect(status().isOk)

		mockMvc.perform(
			get("/settings").accept(MediaType.APPLICATION_JSON),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.interval").value("PT6H"))
			.andExpect(jsonPath("$.nextRun").exists())
			.andExpect(jsonPath("$.previousRun").value(nullValue()))
	}
}
