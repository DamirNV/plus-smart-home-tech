package ru.yandex.practicum.aggregator.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.deserializer.SensorEventDeserializer;
import ru.yandex.practicum.kafka.serializer.GeneralAvroSerializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Properties;

@Configuration
public class KafkaClientConfig {

    @Bean(destroyMethod = "")
    public KafkaConsumer<String, SensorEventAvro> sensorEventConsumer(
            AggregatorKafkaProperties properties
    ) {
        Properties config = new Properties();

        config.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.getBootstrapServers()
        );
        config.put(
                ConsumerConfig.CLIENT_ID_CONFIG,
                properties.getConsumer().getClientId()
        );
        config.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                properties.getConsumer().getGroupId()
        );
        config.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                properties.getConsumer().getAutoOffsetReset()
        );
        config.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                properties.getConsumer().isEnableAutoCommit()
        );
        config.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                properties.getConsumer().getMaxPollRecords()
        );
        config.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                SensorEventDeserializer.class
        );

        return new KafkaConsumer<>(config);
    }

    @Bean(destroyMethod = "")
    public KafkaProducer<String, SensorsSnapshotAvro> snapshotProducer(
            AggregatorKafkaProperties properties
    ) {
        Properties config = new Properties();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.getBootstrapServers()
        );
        config.put(
                ProducerConfig.CLIENT_ID_CONFIG,
                properties.getProducer().getClientId()
        );
        config.put(
                ProducerConfig.ACKS_CONFIG,
                properties.getProducer().getAcks()
        );
        config.put(
                ProducerConfig.RETRIES_CONFIG,
                properties.getProducer().getRetries()
        );
        config.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                properties.getProducer().isEnableIdempotence()
        );
        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                GeneralAvroSerializer.class
        );

        return new KafkaProducer<>(config);
    }
}