package ru.yandex.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.kafka.consumer")
public class KafkaConsumerProperties {

    private ConsumerSettings hubEvents = new ConsumerSettings();
    private ConsumerSettings snapshots = new ConsumerSettings();

    @Getter
    @Setter
    public static class ConsumerSettings {

        private String groupId;
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
        private int maxPollRecords = 100;
        private String topic;
        private long pollTimeoutMs = 1000;
    }
}