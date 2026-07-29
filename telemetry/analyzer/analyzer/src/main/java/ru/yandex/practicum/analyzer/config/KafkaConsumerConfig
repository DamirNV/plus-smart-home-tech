package ru.yandex.practicum.analyzer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

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
    public ConsumerFactory<String, byte[]> hubEventsConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getHubEvents().getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getHubEvents().getAutoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, properties.getHubEvents().isEnableAutoCommit());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, properties.getHubEvents().getMaxPollRecords());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, byte[]> snapshotsConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getSnapshots().getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getSnapshots().getAutoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, properties.getSnapshots().isEnableAutoCommit());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, properties.getSnapshots().getMaxPollRecords());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }
}