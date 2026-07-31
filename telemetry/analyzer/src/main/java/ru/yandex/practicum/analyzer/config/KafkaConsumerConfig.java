package ru.yandex.practicum.analyzer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.yandex.practicum.kafka.deserializer.HubEventDeserializer;
import ru.yandex.practicum.kafka.deserializer.SensorsSnapshotDeserializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaConsumerProperties properties;

    public KafkaConsumerConfig(KafkaConsumerProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ConsumerFactory<String, HubEventAvro>
    hubEventsConsumerFactory() {
        KafkaConsumerProperties.ConsumerSettings settings =
                properties.getHubEvents();

        Map<String, Object> props = createCommonProperties(settings);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                HubEventDeserializer.class
        );

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, SensorsSnapshotAvro>
    snapshotsConsumerFactory() {
        KafkaConsumerProperties.ConsumerSettings settings =
                properties.getSnapshots();

        Map<String, Object> props = createCommonProperties(settings);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                SensorsSnapshotDeserializer.class
        );

        return new DefaultKafkaConsumerFactory<>(props);
    }

    private Map<String, Object> createCommonProperties(
            KafkaConsumerProperties.ConsumerSettings settings
    ) {
        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );
        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                settings.getGroupId()
        );
        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                settings.getAutoOffsetReset()
        );
        props.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                settings.isEnableAutoCommit()
        );
        props.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                settings.getMaxPollRecords()
        );
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        return props;
    }
}