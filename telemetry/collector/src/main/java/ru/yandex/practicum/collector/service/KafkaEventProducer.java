package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${spring.kafka.producer.topic.sensors}")
    private String sensorsTopic;

    @Value("${spring.kafka.producer.topic.hubs}")
    private String hubsTopic;

    public void sendSensorEvent(SpecificRecordBase event) {
        sendEvent(sensorsTopic, event);
    }

    public void sendHubEvent(SpecificRecordBase event) {
        sendEvent(hubsTopic, event);
    }

    private void sendEvent(String topic, SpecificRecordBase event) {
        try {
            byte[] data = event.toByteBuffer().array();
            CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(topic, data);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Событие успешно отправлено в топик {}: {}", topic, event);
                } else {
                    log.error("Ошибка при отправке события в топик {}: {}", topic, ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Ошибка при сериализации события: {}", e.getMessage(), e);
        }
    }
}
