package ru.sadovskie.leo.app.joposcragent.schedulersvc.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID

@Component
class OrchestrationEnvelopePublisher(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	private val jsonMapper: JsonMapper,
) {
	fun publishCollectionBatchBegin(jobUuid: UUID) {
		val key = jobUuid.toString()
		val payload = jsonMapper.createObjectNode().apply {
			put("jobUuid", jobUuid.toString())
		}
		publishEnvelope(
			topic = OrchestrationKafkaTopics.COLLECTION_BATCH,
			messageKey = key,
			type = OrchestrationMessageTypes.COLLECTION_BATCH_BEGIN,
			payload = payload,
		)
	}

	private fun publishEnvelope(
		topic: String,
		messageKey: String,
		type: String,
		payload: ObjectNode,
	) {
		val createdAt = OffsetDateTime.now().toString()
		val schemaVersion = "1.0"
		val json = jsonMapper.writeValueAsString(payload)
		val record = ProducerRecord(topic, messageKey, json)
		record.headers().add(RecordHeader("type", type.toByteArray(StandardCharsets.UTF_8)))
		record.headers().add(RecordHeader("key", messageKey.toByteArray(StandardCharsets.UTF_8)))
		record.headers().add(RecordHeader("createdAt", createdAt.toByteArray(StandardCharsets.UTF_8)))
		record.headers().add(RecordHeader("schemaVersion", schemaVersion.toByteArray(StandardCharsets.UTF_8)))
		kafkaTemplate.send(record)
	}
}
