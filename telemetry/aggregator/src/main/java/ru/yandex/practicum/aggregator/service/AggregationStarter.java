package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.config.AggregatorKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;
    private final SnapshotService snapshotService;
    private final AggregatorKafkaProperties properties;

    public void start() {
        String inputTopic = properties.getConsumer().getTopic();
        String outputTopic = properties.getProducer().getTopic();

        consumer.subscribe(List.of(inputTopic));

        try {
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(Duration.ofMillis(
                                properties.getConsumer().getPollTimeoutMs()
                        ));

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    snapshotService.updateState(record.value())
                            .ifPresent(snapshot ->
                                    sendSnapshot(outputTopic, snapshot)
                            );
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException e) {
            log.info("Получен сигнал остановки Aggregator");
        } catch (Exception e) {
            log.error("Ошибка обработки событий датчиков", e);
            throw new IllegalStateException(
                    "Aggregator остановлен из-за ошибки",
                    e
            );
        } finally {
            producer.flush();
            consumer.close();
            producer.close();

            log.info("Aggregator остановлен");
        }
    }

    private void sendSnapshot(
            String topic,
            SensorsSnapshotAvro snapshot
    ) {
        try {
            producer.send(
                    new ProducerRecord<>(
                            topic,
                            snapshot.getHubId(),
                            snapshot
                    )
            ).get();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Не удалось отправить снапшот хаба "
                            + snapshot.getHubId(),
                    e
            );
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }
}