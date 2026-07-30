package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaConsumerProperties;
import ru.yandex.practicum.analyzer.serialization.AvroDeserializer;
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumerProperties properties;
    private final AvroDeserializer deserializer;
    private final HubEventService hubEventService;
    private final org.springframework.kafka.core.ConsumerFactory<String, byte[]> hubEventsConsumerFactory;

    private volatile boolean running = true;

    @Override
    public void run() {
        log.info("Запуск HubEventProcessor");

        try (KafkaConsumer<String, byte[]> consumer =
                (KafkaConsumer<String, byte[]>) hubEventsConsumerFactory.createConsumer()) {

            String topic = properties.getHubEvents().getTopic();
            consumer.subscribe(Collections.singletonList(topic));
            log.info("Подписались на топик: {}", topic);

            while (running) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(1000));

                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, byte[]> record : records) {
                    try {
                        HubEventAvro event = deserializer.deserialize(record.value(), HubEventAvro.class);
                        log.debug("Получено событие хаба: hubId={}, type={}",
                                event.getHubId(), event.getPayload().getClass().getSimpleName());

                        hubEventService.processHubEvent(event);
                    } catch (Exception e) {
                        log.error("Ошибка обработки события хаба из offset {}", record.offset(), e);
                    }
                }

                // Фиксируем смещения после обработки батча
                consumer.commitSync();
                log.debug("Зафиксировано {} сообщений из топика {}", records.count(), topic);
            }
        } catch (Exception e) {
            log.error("Ошибка в цикле обработки событий хабов", e);
        } finally {
            log.info("HubEventProcessor остановлен");
        }
    }

    public void shutdown() {
        log.info("Запрос на остановку HubEventProcessor");
        running = false;
    }
}