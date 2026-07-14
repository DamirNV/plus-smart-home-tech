package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        log.info("Начинаю отправку в топик: {}", topic);
        try {
            byte[] data = serializeAvro(event);
            log.info("Данные сериализованы, размер: {} байт", data.length);

            kafkaTemplate.send(topic, data).get(5, TimeUnit.SECONDS);

            log.info("Успешно отправлено в топик: {}", topic);
        } catch (Exception e) {
            log.error("Ошибка при отправке в топик {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Ошибка отправки в Kafka", e);
        }
    }

    private byte[] serializeAvro(SpecificRecordBase record) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(record.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            writer.write(record, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        }
    }
}