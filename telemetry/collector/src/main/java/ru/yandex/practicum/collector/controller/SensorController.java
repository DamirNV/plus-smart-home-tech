package ru.yandex.practicum.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.dto.SensorEvent;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.service.KafkaEventProducer;

@Slf4j
@RestController
@RequestMapping("/events/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final KafkaEventProducer kafkaEventProducer;
    private final SensorEventMapper sensorEventMapper;

    @PostMapping
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        log.info("🔵 ПОЛУЧЕНО СОБЫТИЕ ОТ ДАТЧИКА: {}", event);

        try {
            var avroEvent = sensorEventMapper.toAvro(event);
            log.info("🟡 СКОНВЕРТИРОВАНО В AVRO: {}", avroEvent);

            kafkaEventProducer.sendSensorEvent(avroEvent);
            log.info("✅ СОБЫТИЕ УСПЕШНО ОТПРАВЛЕНО В KAFKA");
        } catch (Exception e) {
            log.error("❌ ОШИБКА ПРИ ОБРАБОТКЕ: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при обработке события датчика", e);
        }
    }
}