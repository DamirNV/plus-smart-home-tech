package ru.yandex.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.kafka.consumer")
@Getter
@Setter
public class KafkaConsumerProperties {

    private ConsumerConfig hubEvents;
    private ConsumerConfig snapshots;

    @Getter
    @Setter
    public static class ConsumerConfig {
        private String groupId;
        private String autoOffsetReset;
        private boolean enableAutoCommit;
        private int maxPollRecords;
        private String topic;
    }
}