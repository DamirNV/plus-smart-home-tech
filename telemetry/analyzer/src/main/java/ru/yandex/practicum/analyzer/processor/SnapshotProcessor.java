package ru.yandex.practicum.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaConsumerProperties;
import ru.yandex.practicum.analyzer.service.SnapshotProcessorService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SnapshotProcessor {

    private final KafkaConsumerProperties properties;
    private final SnapshotProcessorService snapshotProcessorService;
    private final ConsumerFactory<String, SensorsSnapshotAvro> consumerFactory;

    private volatile Consumer<String, SensorsSnapshotAvro> consumer;

    public SnapshotProcessor(
            KafkaConsumerProperties properties,
            SnapshotProcessorService snapshotProcessorService,
            @Qualifier("snapshotsConsumerFactory")
            ConsumerFactory<String, SensorsSnapshotAvro> consumerFactory
    ) {
        this.properties = properties;
        this.snapshotProcessorService = snapshotProcessorService;
        this.consumerFactory = consumerFactory;
    }

    public void start() {
        KafkaConsumerProperties.ConsumerSettings settings =
                properties.getSnapshots();

        try (Consumer<String, SensorsSnapshotAvro> localConsumer =
                     consumerFactory.createConsumer()) {

            consumer = localConsumer;
            localConsumer.subscribe(List.of(settings.getTopic()));

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        localConsumer.poll(
                                Duration.ofMillis(
                                        settings.getPollTimeoutMs()
                                )
                        );

                boolean succeeded = true;
                Map<TopicPartition, Long> batchStartOffsets =
                        new HashMap<>();

                records.forEach(record ->
                        batchStartOffsets.putIfAbsent(
                                new TopicPartition(
                                        record.topic(),
                                        record.partition()
                                ),
                                record.offset()
                        )
                );

                for (ConsumerRecord<String, SensorsSnapshotAvro> record
                        : records) {
                    try {
                        snapshotProcessorService.processSnapshot(
                                record.value()
                        );
                    } catch (Exception e) {
                        succeeded = false;

                        batchStartOffsets.forEach(localConsumer::seek);

                        log.error(
                                "Ошибка обработки снапшота, offset={}",
                                record.offset(),
                                e
                        );

                        break;
                    }
                }

                if (succeeded && !records.isEmpty()) {
                    localConsumer.commitSync();
                }
            }
        } catch (WakeupException e) {
            log.info("SnapshotProcessor остановлен");
        } finally {
            consumer = null;
        }
    }

    public void shutdown() {
        Consumer<String, SensorsSnapshotAvro> current = consumer;

        if (current != null) {
            current.wakeup();
        }
    }
}