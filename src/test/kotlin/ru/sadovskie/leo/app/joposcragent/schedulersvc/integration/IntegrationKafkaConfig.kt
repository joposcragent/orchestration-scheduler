package ru.sadovskie.leo.app.joposcragent.schedulersvc.integration

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.EmbeddedKafkaBroker

@TestConfiguration
class IntegrationKafkaConfig {
	@Bean
	fun kafkaTemplate(embeddedKafkaBroker: EmbeddedKafkaBroker): KafkaTemplate<String, String> {
		val props = mapOf(
			ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to embeddedKafkaBroker.brokersAsString,
			ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
			ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
		)
		return KafkaTemplate(DefaultKafkaProducerFactory(props))
	}
}
