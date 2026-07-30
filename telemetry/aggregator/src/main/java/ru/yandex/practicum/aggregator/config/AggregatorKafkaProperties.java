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
        private String clientId = "aggregator-consumer";
        private String groupId = "aggregator-group";
        private String topic = "telemetry.sensors.v1";
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
        private int maxPollRecords = 100;
        private long pollTimeoutMs = 1000;
    }

    @Getter
    @Setter
    public static class Producer {
        private String clientId = "aggregator-producer";
        private String topic = "telemetry.snapshots.v1";
        private String acks = "all";
        private int retries = 5;
        private boolean enableIdempotence = true;
    }
}