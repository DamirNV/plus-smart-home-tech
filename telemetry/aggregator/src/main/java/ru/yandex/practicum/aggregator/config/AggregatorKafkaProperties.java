package ru.yandex.practicum.aggregator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aggregator.kafka")
public class AggregatorKafkaProperties {

    private String bootstrapServers;
    private Consumer consumer = new Consumer();
    private Producer producer = new Producer();

    @Getter
    @Setter
    public static class Consumer {
        private String clientId;
        private String groupId;
        private String topic;
        private String autoOffsetReset;
        private boolean enableAutoCommit;
        private int maxPollRecords;
        private long pollTimeoutMs;
    }

    @Getter
    @Setter
    public static class Producer {
        private String clientId;
        private String topic;
        private String acks;
        private int retries;
        private boolean enableIdempotence;
    }
}