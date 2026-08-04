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
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    private final KafkaConsumerProperties properties;
    private final HubEventService hubEventService;
    private final ConsumerFactory<String, HubEventAvro> consumerFactory;

    private volatile Consumer<String, HubEventAvro> consumer;

    public HubEventProcessor(
            KafkaConsumerProperties properties,
            HubEventService hubEventService,
            @Qualifier("hubEventsConsumerFactory")
            ConsumerFactory<String, HubEventAvro> consumerFactory
    ) {
        this.properties = properties;
        this.hubEventService = hubEventService;
        this.consumerFactory = consumerFactory;
    }

    @Override
    public void run() {
        KafkaConsumerProperties.ConsumerSettings settings =
                properties.getHubEvents();

        try (Consumer<String, HubEventAvro> localConsumer =
                     consumerFactory.createConsumer()) {

            consumer = localConsumer;
            localConsumer.subscribe(List.of(settings.getTopic()));

            while (true) {
                ConsumerRecords<String, HubEventAvro> records =
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

                for (ConsumerRecord<String, HubEventAvro> record
                        : records) {
                    try {
                        hubEventService.processHubEvent(record.value());
                    } catch (Exception e) {
                        succeeded = false;

                        batchStartOffsets.forEach(localConsumer::seek);

                        log.error(
                                "Ошибка обработки события хаба, offset={}",
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
            log.info("HubEventProcessor остановлен");
        } finally {
            consumer = null;
        }
    }

    public void shutdown() {
        Consumer<String, HubEventAvro> current = consumer;

        if (current != null) {
            current.wakeup();
        }
    }
}