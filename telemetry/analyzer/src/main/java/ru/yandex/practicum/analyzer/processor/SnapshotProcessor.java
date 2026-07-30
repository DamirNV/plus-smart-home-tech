package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaConsumerProperties;
import ru.yandex.practicum.analyzer.serialization.AvroDeserializer;
import ru.yandex.practicum.analyzer.service.SnapshotProcessorService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final KafkaConsumerProperties properties;
    private final AvroDeserializer deserializer;
    private final SnapshotProcessorService snapshotProcessorService;
    private final org.springframework.kafka.core.ConsumerFactory<String, byte[]>
            snapshotsConsumerFactory;

    private volatile boolean running = true;

    public void start() {
        log.info("Запуск SnapshotProcessor");

        try (KafkaConsumer<String, byte[]> consumer =
                     (KafkaConsumer<String, byte[]>)
                             snapshotsConsumerFactory.createConsumer()) {

            String topic = properties.getSnapshots().getTopic();
            consumer.subscribe(Collections.singletonList(topic));

            while (running) {
                ConsumerRecords<String, byte[]> records =
                        consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, byte[]> record : records) {
                    SensorsSnapshotAvro snapshot = deserializer.deserialize(
                            record.value(),
                            SensorsSnapshotAvro.class
                    );

                    snapshotProcessorService.processSnapshot(snapshot);
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (Exception e) {
            log.error("Ошибка в цикле обработки снапшотов", e);
        } finally {
            log.info("SnapshotProcessor остановлен");
        }
    }

    public void shutdown() {
        log.info("Запрос на остановку SnapshotProcessor");
        running = false;
    }
}